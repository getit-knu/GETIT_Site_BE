#!/usr/bin/env bash
# nginx 리버스 프록시 구성 + Let's Encrypt 인증서 발급.
#
#   sudo bash issue-cert.sh api.getit.co.kr admin@getit.co.kr
#   sudo bash issue-cert.sh getit-api.koreacentral.cloudapp.azure.com admin@example.com
#
# 도메인이 이 서버의 공인 IP 를 가리키고 있어야 한다. 아니면 인증서 발급이 실패한다.
# 도메인을 나중에 바꿔도 이 스크립트를 다시 돌리면 된다.

set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${2:-}"

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "사용법: sudo bash issue-cert.sh {도메인} {이메일}" >&2
  exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
  echo "sudo 로 실행하세요." >&2
  exit 1
fi

log() { echo -e "\n\033[1;34m▸ $*\033[0m"; }

log "DNS 확인"
RESOLVED=$(getent hosts "$DOMAIN" | awk '{print $1}' | head -1 || true)
PUBLIC_IP=$(curl -fsS --max-time 10 https://api.ipify.org || echo "")
echo "  $DOMAIN → ${RESOLVED:-없음}"
echo "  이 서버   → ${PUBLIC_IP:-확인 불가}"

if [ -z "$RESOLVED" ]; then
  echo "::error:: 도메인이 아직 해석되지 않습니다. DNS 전파를 기다리세요." >&2
  exit 1
fi
if [ -n "$PUBLIC_IP" ] && [ "$RESOLVED" != "$PUBLIC_IP" ]; then
  echo "  ⚠️  도메인이 이 서버를 가리키지 않습니다. 인증서 발급이 실패할 수 있습니다." >&2
fi

log "nginx 리버스 프록시 구성"
cat > /etc/nginx/sites-available/getit <<NGINXEOF
# 백엔드 리버스 프록시. TLS 는 certbot 이 아래에 443 서버 블록을 추가한다.
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN;

    # 업로드 파일 크기. 과제 제출이 최대 50MB 다.
    client_max_body_size 55M;

    # 파일 업로드가 느린 회선에서 끊기지 않도록 여유를 준다.
    proxy_read_timeout 120s;
    proxy_send_timeout 120s;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        # 이게 없으면 Spring 이 http 로 인식해 OAuth2 리다이렉트 주소가 http 로 나간다.
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host  \$host;
    }

    # 헬스체크는 접근 로그를 남기지 않는다. 15초마다 찍히면 로그를 뒤덮는다.
    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        access_log off;
    }
}
NGINXEOF

ln -sf /etc/nginx/sites-available/getit /etc/nginx/sites-enabled/getit
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx
echo "  nginx 구성 반영됨"

log "Let's Encrypt 인증서 발급"
certbot --nginx \
  -d "$DOMAIN" \
  --non-interactive \
  --agree-tos \
  --email "$EMAIL" \
  --redirect

log "자동 갱신 확인"
systemctl status certbot.timer --no-pager 2>/dev/null | head -3 | sed 's/^/  /' || true
certbot renew --dry-run 2>&1 | tail -3 | sed 's/^/  /'

log "완료"
cat <<NEXT

  https://$DOMAIN 로 접근됩니다.

  다음 순서
    1. /opt/getit/.env 의 도메인 값을 채운다
         CORS_ALLOWED_ORIGINS  = 프론트 주소
         OAUTH2_REDIRECT_URI   = {프론트 주소}/oauth/callback
         FILE_BASE_URL         = https://$DOMAIN/api/public/files
    2. Google Cloud Console 승인된 리디렉션 URI 에 추가
         https://$DOMAIN/login/oauth2/code/google
    3. GitHub Actions 에서 CD 실행

NEXT
