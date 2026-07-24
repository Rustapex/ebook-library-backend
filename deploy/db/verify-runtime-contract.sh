#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <abc_ci schema> <contract-version>" >&2
    exit 2
fi

DATABASE_NAME=$1
CONTRACT_VERSION=$2
PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
CONTRACT_SQL="$PROJECT_ROOT/deploy/db/runtime-contract-v${CONTRACT_VERSION}.sql"

case "$DATABASE_NAME" in
    abc_ci|abc_ci_*) ;;
    *)
        echo "refusing to inspect non-CI schema: $DATABASE_NAME" >&2
        exit 2
        ;;
esac

if [[ ! -f "$CONTRACT_SQL" ]]; then
    echo "runtime contract not found: version $CONTRACT_VERSION" >&2
    exit 2
fi

export MYSQL_PWD=${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}
results=$(mysql \
    --protocol=TCP \
    --host="${MYSQL_HOST:-127.0.0.1}" \
    --port="${MYSQL_PORT:-3306}" \
    --user="${MYSQL_USER:-root}" \
    --default-character-set=utf8mb4 \
    --batch \
    --skip-column-names \
    "--init-command=SET SESSION sql_mode='${MYSQL_SQL_MODE:-NO_ENGINE_SUBSTITUTION}', time_zone='${MYSQL_TIME_ZONE:-+00:00}'" \
    "$DATABASE_NAME" < "$CONTRACT_SQL")

printf '%s\n' "$results"
if grep -q '^fail:' <<< "$results"; then
    echo "runtime contract v${CONTRACT_VERSION} failed" >&2
    exit 1
fi

echo "runtime contract v${CONTRACT_VERSION} passed"
