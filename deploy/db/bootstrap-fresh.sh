#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
    echo "usage: $0 <repository-root> <abc_ci schema> [manifest]" >&2
    exit 2
fi

REPOSITORY_ROOT=$(cd "$1" && pwd)
DATABASE_NAME=$2
MANIFEST_PATH=${3:-"$REPOSITORY_ROOT/deploy/db/bootstrap-manifest.txt"}

case "$DATABASE_NAME" in
    abc_ci|abc_ci_*) ;;
    *)
        echo "refusing to recreate non-CI schema: $DATABASE_NAME" >&2
        exit 2
        ;;
esac

MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}
MYSQL_SQL_MODE=${MYSQL_SQL_MODE:-NO_ENGINE_SUBSTITUTION}
MYSQL_TIME_ZONE=${MYSQL_TIME_ZONE:-+00:00}

if [[ ! -f "$MANIFEST_PATH" ]]; then
    echo "bootstrap manifest not found: $MANIFEST_PATH" >&2
    exit 2
fi

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

mysql "${MYSQL_ARGS[@]}" <<SQL
DROP DATABASE IF EXISTS \`$DATABASE_NAME\`;
CREATE DATABASE \`$DATABASE_NAME\`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
SQL

while IFS= read -r relative_path || [[ -n "$relative_path" ]]; do
    relative_path=${relative_path%$'\r'}
    [[ -z "$relative_path" || "$relative_path" == \#* ]] && continue

    case "$relative_path" in
        /*|*..*)
            echo "invalid bootstrap path: $relative_path" >&2
            exit 2
            ;;
        src/main/resources/db/manual/V00*.sql|docs/데이터베이스/migration/000[12]_*.sql)
            echo "existing-DB migration is forbidden in fresh bootstrap: $relative_path" >&2
            exit 2
            ;;
    esac

    sql_file="$REPOSITORY_ROOT/$relative_path"
    if [[ ! -f "$sql_file" ]]; then
        echo "bootstrap SQL not found: $relative_path" >&2
        exit 2
    fi

    echo "bootstrap: $relative_path"
    mysql "${MYSQL_ARGS[@]}" "$DATABASE_NAME" < "$sql_file" > /dev/null
done < "$MANIFEST_PATH"

actual_environment=$(mysql "${MYSQL_ARGS[@]}" "$DATABASE_NAME" -e \
    "SELECT CONCAT(@@SESSION.sql_mode, '|', @@SESSION.time_zone, '|', @@character_set_database, '|', @@collation_database)")
expected_environment="$MYSQL_SQL_MODE|$MYSQL_TIME_ZONE|utf8mb4|utf8mb4_0900_ai_ci"
if [[ "$actual_environment" != "$expected_environment" ]]; then
    echo "CI DB environment mismatch: $actual_environment" >&2
    exit 1
fi

echo "fresh bootstrap complete: $DATABASE_NAME"
