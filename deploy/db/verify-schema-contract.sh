#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <base-sha> <head-sha>" >&2
    exit 2
fi

BASE_SHA=$1
HEAD_SHA=$2
PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
BOOTSTRAP_SCRIPT="$PROJECT_ROOT/deploy/db/bootstrap-fresh.sh"
RUNTIME_CONTRACT_SCRIPT="$PROJECT_ROOT/deploy/db/verify-runtime-contract.sh"

MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}
MYSQL_SQL_MODE=${MYSQL_SQL_MODE:-NO_ENGINE_SUBSTITUTION}
MYSQL_TIME_ZONE=${MYSQL_TIME_ZONE:-+00:00}

for command_name in git tar mysql mysqldump diff awk sed; do
    command -v "$command_name" > /dev/null || {
        echo "required command not found: $command_name" >&2
        exit 2
    }
done

git -C "$PROJECT_ROOT" rev-parse --verify "${BASE_SHA}^{commit}" > /dev/null
git -C "$PROJECT_ROOT" rev-parse --verify "${HEAD_SHA}^{commit}" > /dev/null

TEMP_DIR=$(mktemp -d)
cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT INT TERM

BASE_ROOT="$TEMP_DIR/base"
HEAD_ROOT="$TEMP_DIR/head"
mkdir -p "$BASE_ROOT" "$HEAD_ROOT"
git -C "$PROJECT_ROOT" archive "$BASE_SHA" | tar -x -C "$BASE_ROOT"
git -C "$PROJECT_ROOT" archive "$HEAD_SHA" | tar -x -C "$HEAD_ROOT"

CONTRACT_FILE="$HEAD_ROOT/release-contract.yml"
if [[ ! -f "$CONTRACT_FILE" ]]; then
    echo "release-contract.yml is required" >&2
    exit 1
fi

read_scalar() {
    local key=$1
    sed -n -E "s/^${key}:[[:space:]]*(.+)[[:space:]]*$/\\1/p" "$CONTRACT_FILE" | head -n 1
}

SCHEMA_CHANGE=$(read_scalar schemaChange)
RUNTIME_CONTRACT_VERSION=$(read_scalar runtimeContractVersion)
EPUB_CONTRACT_CHANGE=$(read_scalar epubContractChange)

case "$SCHEMA_CHANGE" in
    none|additive|breaking) ;;
    *)
        echo "invalid schemaChange: $SCHEMA_CHANGE" >&2
        exit 1
        ;;
esac
if [[ ! "$RUNTIME_CONTRACT_VERSION" =~ ^[1-9][0-9]*$ ]]; then
    echo "invalid runtimeContractVersion: $RUNTIME_CONTRACT_VERSION" >&2
    exit 1
fi
case "$EPUB_CONTRACT_CHANGE" in
    true|false) ;;
    *)
        echo "invalid epubContractChange: $EPUB_CONTRACT_CHANGE" >&2
        exit 1
        ;;
esac

mapfile -t REQUIRED_MIGRATIONS < <(
    awk '
        /^requiredMigrations:[[:space:]]*\[\][[:space:]]*$/ { exit }
        /^requiredMigrations:[[:space:]]*$/ { in_list=1; next }
        in_list && /^[^[:space:]]/ { exit }
        in_list && /^[[:space:]]*-[[:space:]]*/ {
            sub(/^[[:space:]]*-[[:space:]]*/, "")
            gsub(/^["'\'']|["'\'']$/, "")
            print
        }
    ' "$CONTRACT_FILE"
)

for migration_path in "${REQUIRED_MIGRATIONS[@]}"; do
    case "$migration_path" in
        src/main/resources/db/manual/V*.sql|docs/데이터베이스/migration/[0-9]*.sql) ;;
        *)
            echo "required migration is outside an approved migration directory: $migration_path" >&2
            exit 1
            ;;
    esac
    case "$migration_path" in
        *rollback*|*/U*.sql)
            echo "rollback SQL cannot be a required forward migration: $migration_path" >&2
            exit 1
            ;;
    esac
    if [[ ! -f "$HEAD_ROOT/$migration_path" ]]; then
        echo "required migration not found at head: $migration_path" >&2
        exit 1
    fi
done

mapfile -d '' -t CHANGED_MIGRATIONS < <(
    while IFS= read -r -d '' migration_path; do
        case "$migration_path" in
            *rollback*|*/U[0-9]*.sql) ;;
            *) printf '%s\0' "$migration_path" ;;
        esac
    done < <(
        git -C "$PROJECT_ROOT" diff --name-only -z --diff-filter=AM \
            "$BASE_SHA" "$HEAD_SHA" -- \
            'src/main/resources/db/manual/V*.sql' \
            'docs/데이터베이스/migration/[0-9]*.sql'
    )
)

sorted_required=$(printf '%s\n' "${REQUIRED_MIGRATIONS[@]}" | sed '/^$/d' | LC_ALL=C sort)
sorted_changed=$(printf '%s\n' "${CHANGED_MIGRATIONS[@]}" | sed '/^$/d' | LC_ALL=C sort)
if [[ "$sorted_required" != "$sorted_changed" ]]; then
    echo "requiredMigrations does not match added/modified forward migrations" >&2
    echo "contract list:" >&2
    printf '%s\n' "$sorted_required" >&2
    echo "git diff list:" >&2
    printf '%s\n' "$sorted_changed" >&2
    exit 1
fi

BASE_MANIFEST="$BASE_ROOT/deploy/db/bootstrap-manifest.txt"
if [[ ! -f "$BASE_MANIFEST" ]]; then
    BASE_MANIFEST="$HEAD_ROOT/deploy/db/bootstrap-manifest.txt"
fi
HEAD_MANIFEST="$HEAD_ROOT/deploy/db/bootstrap-manifest.txt"

export MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_SQL_MODE MYSQL_TIME_ZONE
bash "$BOOTSTRAP_SCRIPT" "$BASE_ROOT" abc_ci_base "$BASE_MANIFEST"
bash "$BOOTSTRAP_SCRIPT" "$HEAD_ROOT" abc_ci_head "$HEAD_MANIFEST"

export MYSQL_PWD=$MYSQL_PASSWORD
MYSQL_ARGS=(
    --protocol=TCP
    --host="$MYSQL_HOST"
    --port="$MYSQL_PORT"
    --user="$MYSQL_USER"
    --default-character-set=utf8mb4
    --batch
    --skip-column-names
    "--init-command=SET SESSION sql_mode='$MYSQL_SQL_MODE', time_zone='$MYSQL_TIME_ZONE'"
)
DUMP_COMPAT_ARGS=()
if mysqldump --help 2>&1 | grep -q -- '--set-gtid-purged'; then
    DUMP_COMPAT_ARGS+=(--set-gtid-purged=OFF)
fi
if mysqldump --help 2>&1 | grep -q -- '--column-statistics'; then
    DUMP_COMPAT_ARGS+=(--column-statistics=0)
fi

dump_schema() {
    local database_name=$1
    local output_path=$2
    local raw_path="$output_path.raw"
    local ignore_args=()
    local table_name

    while IFS= read -r table_name; do
        [[ -n "$table_name" ]] && ignore_args+=("--ignore-table=${database_name}.${table_name}")
    done < <(
        mysql "${MYSQL_ARGS[@]}" "$database_name" -e "
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND (
                TABLE_NAME LIKE '\\\\_migration\\\\_%'
                OR TABLE_NAME LIKE '%\\\\_backup'
                OR TABLE_NAME LIKE '%\\\\_migration_meta'
              )
            ORDER BY TABLE_NAME"
    )

    mysqldump \
        --protocol=TCP \
        --host="$MYSQL_HOST" \
        --port="$MYSQL_PORT" \
        --user="$MYSQL_USER" \
        --default-character-set=utf8mb4 \
        --no-data \
        --skip-comments \
        --skip-dump-date \
        --skip-add-locks \
        "${DUMP_COMPAT_ARGS[@]}" \
        "${ignore_args[@]}" \
        "$database_name" > "$raw_path"

    sed -E \
        -e 's/AUTO_INCREMENT=[0-9]+[[:space:]]*//g' \
        -e '/^\/\*![0-9]+ SET /d' \
        -e '/^--/d' \
        -e '/^[[:space:]]*$/d' \
        "$raw_path" > "$output_path"
}

BASE_DUMP="$TEMP_DIR/base-schema.sql"
HEAD_DUMP="$TEMP_DIR/head-schema.sql"
dump_schema abc_ci_base "$BASE_DUMP"
dump_schema abc_ci_head "$HEAD_DUMP"

SCHEMA_DIFF="$TEMP_DIR/schema.diff"
if diff -u "$BASE_DUMP" "$HEAD_DUMP" > "$SCHEMA_DIFF"; then
    HAS_SCHEMA_DIFF=false
else
    HAS_SCHEMA_DIFF=true
fi

if [[ "$HAS_SCHEMA_DIFF" == true && "$SCHEMA_CHANGE" == none ]]; then
    echo "actual MySQL schema changed but release contract says schemaChange: none" >&2
    sed -n '1,200p' "$SCHEMA_DIFF" >&2
    exit 1
fi
if [[ "$HAS_SCHEMA_DIFF" == false && "$SCHEMA_CHANGE" != none ]]; then
    echo "release contract declares schema change but normalized MySQL schemas are equal" >&2
    exit 1
fi
if [[ "$HAS_SCHEMA_DIFF" == true && ${#REQUIRED_MIGRATIONS[@]} -eq 0 ]]; then
    echo "schema changed but requiredMigrations is empty" >&2
    exit 1
fi

dump_table() {
    local database_name=$1
    local table_name=$2
    local output_path=$3
    mysqldump \
        --protocol=TCP \
        --host="$MYSQL_HOST" \
        --port="$MYSQL_PORT" \
        --user="$MYSQL_USER" \
        --default-character-set=utf8mb4 \
        --no-data \
        --skip-comments \
        --skip-dump-date \
        --skip-add-locks \
        "${DUMP_COMPAT_ARGS[@]}" \
        "$database_name" "$table_name" |
        sed -E \
            -e 's/AUTO_INCREMENT=[0-9]+[[:space:]]*//g' \
            -e '/^\/\*![0-9]+ SET /d' \
            -e '/^--/d' \
            -e '/^[[:space:]]*$/d' > "$output_path"
}

dump_table abc_ci_base book_epub "$TEMP_DIR/base-book-epub.sql"
dump_table abc_ci_head book_epub "$TEMP_DIR/head-book-epub.sql"
if diff -q "$TEMP_DIR/base-book-epub.sql" "$TEMP_DIR/head-book-epub.sql" > /dev/null; then
    ACTUAL_EPUB_CONTRACT_CHANGE=false
else
    ACTUAL_EPUB_CONTRACT_CHANGE=true
fi
if [[ "$ACTUAL_EPUB_CONTRACT_CHANGE" != "$EPUB_CONTRACT_CHANGE" ]]; then
    echo "epubContractChange does not match the actual book_epub schema delta" >&2
    exit 1
fi

bash "$RUNTIME_CONTRACT_SCRIPT" abc_ci_head "$RUNTIME_CONTRACT_VERSION"

if [[ ${#REQUIRED_MIGRATIONS[@]} -gt 0 ]]; then
    bash "$BOOTSTRAP_SCRIPT" "$BASE_ROOT" abc_ci_migrated "$BASE_MANIFEST"
    for migration_path in "${REQUIRED_MIGRATIONS[@]}"; do
        echo "migration rehearsal: $migration_path"
        mysql "${MYSQL_ARGS[@]}" abc_ci_migrated < "$HEAD_ROOT/$migration_path" > /dev/null
        echo "migration idempotency: $migration_path"
        mysql "${MYSQL_ARGS[@]}" abc_ci_migrated < "$HEAD_ROOT/$migration_path" > /dev/null
    done

    MIGRATED_DUMP="$TEMP_DIR/migrated-schema.sql"
    dump_schema abc_ci_migrated "$MIGRATED_DUMP"
    if ! diff -u "$MIGRATED_DUMP" "$HEAD_DUMP" > "$TEMP_DIR/convergence.diff"; then
        echo "base + required migrations does not converge to head fresh bootstrap" >&2
        sed -n '1,200p' "$TEMP_DIR/convergence.diff" >&2
        exit 1
    fi
    bash "$RUNTIME_CONTRACT_SCRIPT" abc_ci_migrated "$RUNTIME_CONTRACT_VERSION"
fi

echo "schema contract verified: $BASE_SHA -> $HEAD_SHA"
