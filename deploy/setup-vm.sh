#!/usr/bin/env bash
# VM 최초 세팅. 여러 번 돌려도 같은 결과가 되도록 작성했다.
#
#   scp -i {키} deploy/setup-vm.sh deploy/issue-cert.sh azureuser@{IP}:/tmp/
#   ssh -i {키} azureuser@{IP} 'sudo bash /tmp/setup-vm.sh'
#
# 하는 일 — docker · nginx · certbot 설치, 방화벽, 배포 디렉터리 생성.
# TLS 인증서 발급은 DNS 이름이 IP 를 가리킨 뒤에 따로 실행한다 (issue-cert.sh).

set -euo pipefail

APP_DIR=/opt/getit
APP_USER=azureuser

log() { echo -e "\n\033[1;34m▸ $*\033[0m"; }

if [ "$(id -u)" -ne 0 ]; then
  echo "sudo 로 실행하세요." >&2
  exit 1
fi

log "패키지 목록 갱신"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq

log "기본 도구 설치"
apt-get install -y -qq ca-certificates curl gnupg ufw

log "타임존 Asia/Seoul"
timedatectl set-timezone Asia/Seoul

log "Docker 설치"
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
else
  echo "  이미 설치됨"
fi

log "$APP_USER 를 docker 그룹에 추가"
usermod -aG docker "$APP_USER"
echo "  (재로그인 후 sudo 없이 docker 사용 가능)"

log "nginx · certbot 설치"
apt-get install -y -qq nginx certbot python3-certbot-nginx
systemctl enable --now nginx

log "방화벽"
# SSH 를 먼저 열지 않으면 ufw enable 순간 접속이 끊긴다.
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable
ufw status | sed 's/^/  /'

log "배포 디렉터리 $APP_DIR"
mkdir -p "$APP_DIR"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  cat > "$APP_DIR/.env" <<'ENVEOF'
# 운영 환경변수. 이 파일은 VM 에만 있고 저장소에 올리지 않는다.
# 값을 채운 뒤 docker compose up -d 로 반영한다.

# --- 이미지 ---
APP_IMAGE=ghcr.io/getit-knu/getit_site_be:latest

# --- MySQL ---
DB_NAME=getit
DB_USERNAME=getit
DB_PASSWORD=여기를_채우세요
MYSQL_ROOT_PASSWORD=여기를_채우세요

# --- 인증 ---
# openssl rand -base64 48 로 만든다. 로컬 값을 재사용하지 말 것.
JWT_SECRET=여기를_채우세요
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# --- 도메인 ---
# 프론트 주소. 여러 개면 콤마로 구분한다.
CORS_ALLOWED_ORIGINS=
OAUTH2_REDIRECT_URI=
FILE_BASE_URL=

# --- 쿠키 ---
REFRESH_COOKIE_SECURE=true
# 프론트와 백엔드가 같은 등록 도메인이면 Lax, 다르면 None
REFRESH_COOKIE_SAME_SITE=Lax

# --- Azure Storage (백업 · 파일 업로드 공용) ---
# 자격증명은 넣지 않는다. VM 의 관리 ID 로 인증한다.
# 준비 절차는 docs/DEPLOYMENT.md 참조.
AZURE_STORAGE_ACCOUNT=
AZURE_BACKUP_CONTAINER=db-backups

# --- 파일 업로드 (비워두면 VM 로컬 디스크에 저장) ---
# true 로 켜면 프론트가 Azure Blob 으로 직접 올린다. 파일이 VM 디스크에 남지 않는다.
# 공개·비공개를 다른 계정에 둔다. 백업 계정(AZURE_STORAGE_ACCOUNT)과도 분리돼 있다.
FILE_AZURE_ENABLED=false
# 강의 자료 · 과제 제출물 — 비공개, 서명 주소로만 읽는다
AZURE_UPLOAD_ACCOUNT=
AZURE_UPLOAD_CONTAINER=uploads
# 프로필 이미지 · 프로젝트 썸네일 — 공개, 고정 주소
AZURE_PUBLIC_ACCOUNT=
AZURE_PUBLIC_CONTAINER=public-assets
ENVEOF
  chown "$APP_USER:$APP_USER" "$APP_DIR/.env"
  chmod 600 "$APP_DIR/.env"
  echo "  .env 템플릿 생성됨 — 값을 채워야 한다"
else
  echo "  .env 이미 존재. 건드리지 않는다"
fi

log "완료"
cat <<'NEXT'

  다음 순서
    1. /opt/getit/.env 의 값을 채운다
    2. Azure 포털에서 공인 IP 에 DNS 이름 라벨을 지정한다
    3. sudo bash /tmp/issue-cert.sh {도메인} {이메일} 로 TLS 인증서를 발급한다
    4. GitHub Actions 에서 CD 를 돌린다

NEXT
