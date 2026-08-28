#!/usr/bin/env bash
# 백업 파일을 Azure Blob Storage 로 올린다.
#
#   bash upload-backup.sh /opt/getit/backups/getit-20260829-040000.sql.gz
#
# backup-db.sh 가 백업에 성공하면 이 스크립트를 부른다. 단독으로도 쓸 수 있다.
#
# 백업이 DB 와 같은 디스크에 있으면 디스크가 깨질 때 원본과 백업이 함께 사라진다.
# 다른 장애 도메인에 사본을 둬야 백업이라고 할 수 있다.
#
# 자격증명을 저장하지 않는다. VM 의 관리 ID(managed identity)로 토큰을 받아 쓴다.
# .env 에 키를 넣으면 VM 이 털렸을 때 백업까지 함께 털린다.
#
# 필요한 준비는 docs/DEPLOYMENT.md 의 "백업 오프디스크 복사" 참조.

set -euo pipefail

FILE="${1:?업로드할 파일 경로를 넘기세요}"
APP_DIR=/opt/getit

# 설정은 .env 에서 읽는다.
if [ -f "$APP_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$APP_DIR/.env"
  set +a
fi

ACCOUNT="${AZURE_STORAGE_ACCOUNT:-}"
CONTAINER="${AZURE_BACKUP_CONTAINER:-db-backups}"

# 설정 전에는 조용히 넘어간다. 로컬 백업까지 실패로 만들 이유가 없다.
if [ -z "$ACCOUNT" ]; then
  echo "[$(date '+%F %T')] AZURE_STORAGE_ACCOUNT 가 없어 원격 복사를 건너뜁니다." >&2
  exit 0
fi

if [ ! -f "$FILE" ]; then
  echo "[$(date '+%F %T')] 업로드할 파일이 없습니다: $FILE" >&2
  exit 1
fi

SIZE=$(wc -c < "$FILE")

# 단일 PUT 은 256MB 까지다. 그보다 커지면 블록 분할 업로드가 필요하다.
if [ "$SIZE" -gt 209715200 ]; then
  echo "[$(date '+%F %T')] 파일이 200MB 를 넘었습니다 (${SIZE}B). 분할 업로드 구현이 필요합니다." >&2
  exit 1
fi

# 관리 ID 토큰. VM 안에서만 받을 수 있고 만료가 짧아 유출 위험이 낮다.
TOKEN=$(curl -s --max-time 10 -H Metadata:true \
  "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https%3A%2F%2Fstorage.azure.com%2F" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))' 2>/dev/null || true)

if [ -z "$TOKEN" ]; then
  echo "[$(date '+%F %T')] 관리 ID 토큰을 받지 못했습니다." >&2
  echo "    VM 에 시스템 할당 관리 ID 가 켜져 있고, 스토리지에 Storage Blob Data Contributor 역할이 있어야 합니다." >&2
  exit 1
fi

# 연/월로 나눠 담는다. 한 컨테이너에 수천 개가 평평하게 쌓이면 찾기 어렵다.
NAME=$(basename "$FILE")
BLOB="getit/$(date +%Y/%m)/$NAME"
URL="https://${ACCOUNT}.blob.core.windows.net/${CONTAINER}/${BLOB}"

CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$URL" \
  -H "Authorization: Bearer $TOKEN" \
  -H "x-ms-version: 2021-08-06" \
  -H "x-ms-blob-type: BlockBlob" \
  -H "Content-Type: application/gzip" \
  -H "Content-Length: $SIZE" \
  --data-binary "@$FILE" \
  --max-time 300)

if [ "$CODE" != "201" ]; then
  echo "[$(date '+%F %T')] 업로드 실패 (HTTP $CODE): $BLOB" >&2
  case "$CODE" in
    403) echo "    권한 문제입니다. 관리 ID 에 Storage Blob Data Contributor 를 부여했는지 확인하세요." >&2 ;;
    404) echo "    컨테이너 '$CONTAINER' 가 없습니다." >&2 ;;
  esac
  exit 1
fi

# 올라갔다고 믿지 않고 크기를 확인한다. 잘린 파일은 백업이 아니다.
REMOTE=$(curl -s -I "$URL" \
  -H "Authorization: Bearer $TOKEN" -H "x-ms-version: 2021-08-06" --max-time 30 \
  | tr -d '\r' | awk -F': ' 'tolower($1)=="content-length"{print $2}')

if [ "$REMOTE" != "$SIZE" ]; then
  echo "[$(date '+%F %T')] 업로드 크기가 다릅니다 (로컬 ${SIZE}B / 원격 ${REMOTE:-없음}B)" >&2
  exit 1
fi

echo "[$(date '+%F %T')] 원격 복사 완료: $CONTAINER/$BLOB (${SIZE}B)"
