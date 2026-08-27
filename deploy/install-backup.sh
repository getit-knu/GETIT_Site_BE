#!/usr/bin/env bash
# 백업 스크립트를 VM 에 설치하고 cron 에 등록한다.
#
#   scp -i {키} deploy/backup-db.sh deploy/install-backup.sh azureuser@{IP}:/tmp/
#   ssh -i {키} azureuser@{IP} 'sudo bash /tmp/install-backup.sh'
#
# 여러 번 돌려도 중복 등록되지 않는다.

set -euo pipefail

APP_DIR=/opt/getit
APP_USER=azureuser
CRON_FILE=/etc/cron.d/getit-backup

if [ "$(id -u)" -ne 0 ]; then
  echo "sudo 로 실행하세요." >&2
  exit 1
fi

install -o "$APP_USER" -g "$APP_USER" -m 0755 /tmp/backup-db.sh "$APP_DIR/backup-db.sh"
echo "▸ $APP_DIR/backup-db.sh 설치됨"

mkdir -p "$APP_DIR/backups"
chown "$APP_USER:$APP_USER" "$APP_DIR/backups"

# 새벽 4시. 트래픽이 가장 적은 시간대다.
cat > "$CRON_FILE" <<CRONEOF
# GETIT DB 백업. deploy/install-backup.sh 가 생성한다. 직접 수정하지 않는다.
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
CRON_TZ=Asia/Seoul

0 4 * * * $APP_USER cd $APP_DIR && ./backup-db.sh >> $APP_DIR/backups/backup.log 2>&1
CRONEOF
chmod 0644 "$CRON_FILE"
echo "▸ cron 등록됨 — 매일 04:00 (KST)"

# 로그가 무한정 커지지 않게 한다.
cat > /etc/logrotate.d/getit-backup <<'LOGEOF'
/opt/getit/backups/backup.log {
    weekly
    rotate 8
    compress
    missingok
    notifempty
    copytruncate
}
LOGEOF
echo "▸ logrotate 등록됨"

echo
echo "  확인: sudo run-parts --test /etc/cron.d 2>/dev/null; cat $CRON_FILE"
echo "  수동 실행: cd $APP_DIR && ./backup-db.sh"
