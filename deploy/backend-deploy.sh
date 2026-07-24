#!/usr/bin/env bash

set -euo pipefail
umask 077

if [[ $# -ne 4 ]]; then
    echo "usage: backend-deploy.sh <image-uri> <40-char-sha> <sha256-digest> <release-id>" >&2
    exit 2
fi

IMAGE_URI=$1
IMAGE_TAG=$2
EXPECTED_DIGEST=$3
RELEASE_ID=$4

PROJECT_ROOT=/opt/abc
ENV_FILE="$PROJECT_ROOT/.env.prod"
COMPOSE_FILE="$PROJECT_ROOT/compose.prod.yml"
BACKUP_DIR="$PROJECT_ROOT/backups"
CONTAINER_NAME=abc-api
LOCK_FILE=/var/lock/abc-backend-deploy.lock

if [[ $EUID -ne 0 ]]; then
    echo "deployment script must run as root" >&2
    exit 2
fi
if [[ ! "$IMAGE_URI" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/[a-z0-9._/-]+$ ]]; then
    echo "invalid ECR image URI" >&2
    exit 2
fi
if [[ ! "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
    echo "image tag must be a full 40-character git SHA" >&2
    exit 2
fi
if [[ ! "$EXPECTED_DIGEST" =~ ^sha256:[0-9a-f]{64}$ ]]; then
    echo "invalid image digest" >&2
    exit 2
fi
if [[ ! "$RELEASE_ID" =~ ^[A-Za-z0-9._-]{1,80}$ ]]; then
    echo "invalid release ID" >&2
    exit 2
fi
if [[ ! -f "$ENV_FILE" || ! -f "$COMPOSE_FILE" ]]; then
    echo "deployment files are missing under /opt/abc" >&2
    exit 1
fi

exec 9> "$LOCK_FILE"
if ! flock -n 9; then
    echo "another backend deployment is already running" >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
chown root:root "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/.env.prod.${RELEASE_ID}.bak"
ENV_REPLACED=0
DEPLOY_STARTED=0

TEMP_ENV=$(mktemp "$PROJECT_ROOT/.env.prod.tmp.XXXXXX")
cleanup() {
    if [[ "$ENV_REPLACED" -eq 1 && "$DEPLOY_STARTED" -eq 0 && -f "$BACKUP_FILE" ]]; then
        install -o root -g root -m 600 "$BACKUP_FILE" "$ENV_FILE"
    fi
    if [[ -n "${TEMP_ENV:-}" ]]; then
        rm -f "$TEMP_ENV"
    fi
}
trap cleanup EXIT INT TERM

awk -v image="$IMAGE_URI" -v tag="$IMAGE_TAG" '
    BEGIN {
        image_written = 0
        tag_written = 0
    }
    /^ABC_API_IMAGE=/ {
        if (!image_written) {
            print "ABC_API_IMAGE=" image
            image_written = 1
        }
        next
    }
    /^ABC_API_TAG=/ {
        if (!tag_written) {
            print "ABC_API_TAG=" tag
            tag_written = 1
        }
        next
    }
    {
        print
    }
    END {
        if (!image_written) {
            print "ABC_API_IMAGE=" image
        }
        if (!tag_written) {
            print "ABC_API_TAG=" tag
        }
    }
' "$ENV_FILE" > "$TEMP_ENV"

chown root:root "$TEMP_ENV"
chmod 600 "$TEMP_ENV"

REGISTRY=${IMAGE_URI%%/*}
ECR_REGION=$(printf '%s' "$REGISTRY" |
    sed -n -E 's/^[0-9]{12}\.dkr\.ecr\.([a-z0-9-]+)\.amazonaws\.com$/\1/p')
if [[ -z "$ECR_REGION" ]]; then
    echo "could not derive ECR region" >&2
    exit 1
fi

if ! aws ecr get-login-password --region "$ECR_REGION" 2> /dev/null |
    docker login --username AWS --password-stdin "$REGISTRY" > /dev/null 2>&1; then
    echo "ECR login failed" >&2
    exit 1
fi

IMAGE_REF="${IMAGE_URI}:${IMAGE_TAG}"
if ! docker pull "$IMAGE_REF" > /dev/null 2>&1; then
    echo "image pull failed for release $IMAGE_TAG" >&2
    exit 1
fi

if ! docker image inspect "$IMAGE_REF" --format '{{range .RepoDigests}}{{println .}}{{end}}' |
    grep -Fxq "${IMAGE_URI}@${EXPECTED_DIGEST}"; then
    echo "pulled image digest does not match expected digest" >&2
    exit 1
fi

cd "$PROJECT_ROOT"
if ! docker compose --env-file "$TEMP_ENV" -f "$COMPOSE_FILE" config --quiet > /dev/null 2>&1; then
    echo "compose validation failed" >&2
    exit 1
fi

# login, pull, digest, compose validation까지 끝난 뒤에만 production env를 교체한다.
# 이 지점 이전의 실패는 기존 env/container에 영향을 주지 않는다.
install -o root -g root -m 600 "$ENV_FILE" "$BACKUP_FILE"
ENV_REPLACED=1
mv -f "$TEMP_ENV" "$ENV_FILE"
TEMP_ENV=

DEPLOY_STARTED=1
if ! docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d > /dev/null 2>&1; then
    echo "compose deployment failed" >&2
    exit 1
fi

echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=starting"
HEALTH_DEADLINE=$((SECONDS + 300))
while (( SECONDS < HEALTH_DEADLINE )); do
    state=$(docker inspect --format \
        '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' \
        "$CONTAINER_NAME" 2> /dev/null || true)
    container_state=${state%% *}
    health_state=${state#* }

    case "$container_state" in
        exited|dead|removing)
            echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=$container_state" >&2
            exit 1
            ;;
    esac
    case "$health_state" in
        healthy)
            break
            ;;
        unhealthy)
            echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=unhealthy" >&2
            exit 1
            ;;
        starting|missing|"")
            ;;
        *)
            echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=unknown" >&2
            exit 1
            ;;
    esac
    sleep 5
done

if [[ ${health_state:-} != healthy ]]; then
    echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=timeout" >&2
    exit 1
fi

READINESS_OK=0
for _ in $(seq 1 30); do
    if curl -fsS --max-time 5 http://127.0.0.1:8080/api/v1/categories > /dev/null 2>&1; then
        READINESS_OK=1
        break
    fi
    sleep 5
done
if [[ $READINESS_OK -ne 1 ]]; then
    echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=ready-failed" >&2
    exit 1
fi

RUNNING_REF=$(docker inspect --format '{{.Config.Image}}' "$CONTAINER_NAME")
RUNNING_IMAGE_ID=$(docker inspect --format '{{.Image}}' "$CONTAINER_NAME")
if [[ "$RUNNING_REF" != "$IMAGE_REF" ]]; then
    echo "running container tag does not match release" >&2
    exit 1
fi
if ! docker image inspect "$RUNNING_IMAGE_ID" --format '{{range .RepoDigests}}{{println .}}{{end}}' |
    grep -Fxq "${IMAGE_URI}@${EXPECTED_DIGEST}"; then
    echo "running container digest does not match release" >&2
    exit 1
fi

echo "release=$IMAGE_TAG digest=$EXPECTED_DIGEST health=healthy readiness=passed"
