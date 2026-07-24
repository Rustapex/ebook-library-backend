#!/bin/sh
# .env.prod의 ABC_API_IMAGE / ABC_API_TAG가 compose.prod.yml에 실제로 적용되는지 검증한다.
#
# 배경: `--env-file .env.prod`를 빠뜨리면 image tag가 조용히 기본값(abc-api:local)으로 떨어져
#       의도한 것과 다른 image가 배포된다. 이 script는 그 회귀를 잡는다.
#
# 실행:
#   sh deploy/verify-compose-image-tag.sh
#   echo "exit code: $?"    # 0이어야 성공
#
# 이 script는 실제 secret을 출력하지 않는다(`config --images`만 사용).
# 기존 .env.prod가 있으면 읽지도 수정하지도 않고 그대로 둔다.

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
COMPOSE_FILE="$PROJECT_ROOT/compose.prod.yml"
ENV_PROD="$PROJECT_ROOT/.env.prod"
TMP_DIR=$(mktemp -d)
CREATED_ENV_PROD=0
FAILURES=0

cleanup() {
    [ "$CREATED_ENV_PROD" -eq 1 ] && rm -f "$ENV_PROD"
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT INT TERM

# compose의 `env_file:` 항목은 file이 실제로 있어야 config가 통과한다.
# 없으면 example로 임시 생성하고 끝나면 지운다. 있으면 절대 건드리지 않는다.
if [ ! -f "$ENV_PROD" ]; then
    cp "$PROJECT_ROOT/.env.prod.example" "$ENV_PROD"
    CREATED_ENV_PROD=1
fi

# interpolation 전용 env file. 실제 .env.prod 대신 이 file로 tag를 주입해
# 사용자의 secret을 읽지 않고 치환 동작만 검증한다.
make_env_file() {
    _path="$TMP_DIR/$1"
    {
        [ -n "$2" ] && echo "ABC_API_IMAGE=$2"
        [ -n "$3" ] && echo "ABC_API_TAG=$3"
    } > "$_path"
    echo "$_path"
}

actual_image() {
    docker compose --env-file "$1" -f "$COMPOSE_FILE" config --images
}

assert_image() {
    _label="$1"
    _env_file="$2"
    _expected="$3"
    _actual=$(actual_image "$_env_file")
    if [ "$_actual" = "$_expected" ]; then
        echo "ok   - $_label: $_actual"
    else
        echo "FAIL - $_label: expected '$_expected' but got '$_actual'"
        FAILURES=$((FAILURES + 1))
    fi
}

echo "== compose image tag interpolation =="

# 1. .env.prod의 image/tag가 그대로 반영된다.
assert_image "env file tag applied" \
    "$(make_env_file one abc-api-registry 2026-07-23-a)" \
    "abc-api-registry:2026-07-23-a"

# 2. 값이 하드코딩된 게 아니라 실제로 file에서 온다(다른 값으로 재확인).
assert_image "env file tag is not hardcoded" \
    "$(make_env_file two other-registry/abc-api v9.9.9)" \
    "other-registry/abc-api:v9.9.9"

# 3. tag만 지정하면 image 이름은 기본값을 쓴다.
assert_image "tag only, image defaults" \
    "$(make_env_file three '' only-tag)" \
    "abc-api:only-tag"

# 4. 아무것도 없으면 기본값으로 떨어진다(= --env-file 누락 시의 위험을 문서화하는 근거).
assert_image "empty env file falls back to default" \
    "$(make_env_file four '' '')" \
    "abc-api:local"

echo
if [ "$FAILURES" -eq 0 ]; then
    echo "PASS: compose image tag interpolation works"
    exit 0
fi
echo "FAIL: $FAILURES check(s) failed"
exit 1
