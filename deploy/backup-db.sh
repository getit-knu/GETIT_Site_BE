#!/usr/bin/env bash
# MySQL 백업. cron 이 매일 새벽에 실행한다.
#
# 설치 — install-backup.sh 가 /tmp/backup-db.sh 를 읽으므로 두 파일을 함께 보낸다.
#   scp -i {키} deploy/backup-db.sh deploy/install-backup.sh azureuser@{IP}:/tmp/
#   ssh -i {키} azureuser@{IP} 'sudo bash /tmp/install-backup.sh'
#
# 수동 실행: cd /opt/getit && ./backup-db.sh
#
# 지원서 · 평가 데이터가 들어가면 손실이 치명적이다. 컨테이너 볼륨만 믿지 않는다.

set -euo pipefail

APP_DIR=/opt/getit
BACKUP_DIR="$APP_DIR/backups"
RETENTION_DAYS=14

cd "$APP_DIR"

# .env 의 값을 읽는다. 비밀번호를 스크립트에 넣지 않는다.
set -a
# shellcheck disable=SC1091
source .env
set +a

mkdir -p "$BACKUP_DIR"

STAMP=$(date +%Y%m%d-%H%M%S)
FILE="$BACKUP_DIR/getit-$STAMP.sql.gz"

# --single-transaction 으로 잠금 없이 일관된 스냅샷을 뜬다.
# 없으면 백업 중 쓰기가 막혀 서비스가 멈춘다.
#
# stderr 를 버리지 않는다. 실패했을 때 원인이 남아야 고칠 수 있다.
# 다만 "Using a password on the command line" 경고는 매번 나오므로 걸러낸다.
ERR_LOG=$(mktemp)
trap 'rm -f "$ERR_LOG"' EXIT

if ! docker compose exec -T mysql \
  mysqldump \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    -u root -p"$MYSQL_ROOT_PASSWORD" \
    "${DB_NAME:-getit}" 2>"$ERR_LOG" | gzip > "$FILE"
then
  echo "[$(date '+%F %T')] 백업 실패" >&2
  grep -v "Using a password on the command line" "$ERR_LOG" | sed 's/^/    /' >&2 || true
  rm -f "$FILE"
  exit 1
fi

# 빈 파일이 만들어졌는데 성공으로 넘어가면 백업이 있다고 착각하게 된다.
#
# stat -c 는 GNU 전용이라 실패하면 0 이 되고, 그러면 멀쩡한 백업을 지운다.
# wc -c 는 어디서나 같게 동작한다.
SIZE=$(wc -c < "$FILE")
if [ "$SIZE" -lt 1000 ]; then
  echo "[$(date '+%F %T')] 백업 파일이 비정상적으로 작습니다 (${SIZE}B)" >&2
  rm -f "$FILE"
  exit 1
fi

echo "[$(date '+%F %T')] 백업 완료: $FILE ($(numfmt --to=iec "$SIZE"))"

# 오래된 백업 정리. 디스크가 29GB 밖에 없다.
DELETED=$(find "$BACKUP_DIR" -name 'getit-*.sql.gz' -mtime +"$RETENTION_DAYS" -print -delete | wc -l)
[ "$DELETED" -gt 0 ] && echo "[$(date '+%F %T')] 오래된 백업 ${DELETED}건 삭제"

echo "[$(date '+%F %T')] 보관 중: $(find "$BACKUP_DIR" -name 'getit-*.sql.gz' | wc -l)건, \
$(du -sh "$BACKUP_DIR" | cut -f1)"
