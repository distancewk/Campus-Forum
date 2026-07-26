#!/usr/bin/env bash
# 校园论坛 —— 本地一键启动脚本
#
# 启动顺序：PostgreSQL + Redis  ->  后端(Spring Boot)  ->  前端(Vite)
# 仅用于本地开发，请勿用于生产环境。
#
# 用法：
#   ./start.sh            一键启动全部
#   ./stop.sh             一键停止全部
#
# 密钥（JWT 等）必须提供，否则后端无法启动。本脚本顶部给出的是
# 仅用于本地开发的占位值；可通过环境变量或项目根目录的 .env 文件覆盖
# （.env 已被 gitignore，可安全存放真实值）。

set -o pipefail
# 说明：未使用 set -u（nounset）。已知 bash 在 LANG=C 下会因
# “含中文的双引号字符串中引用变量”而误报 unbound variable，故此处仅保留 pipefail。

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT/.runtime-logs"
mkdir -p "$LOG_DIR"

BACKEND_DIR="$ROOT/campus-forum-backend"
FRONTEND_DIR="$ROOT/campus-forum-frontend-new"
BACKEND_JAR="$BACKEND_DIR/target/campus-forum-1.0.0-SNAPSHOT.jar"
BACKEND_PORT=8080
FRONTEND_PORT=5173

# ---- 可选：从 .env 读取覆盖（.env 已被 gitignore）----
if [ -f "$ROOT/.env" ]; then
  set -a; . "$ROOT/.env"; set +a
fi

# ---- 本地开发默认配置（DEV ONLY，生产请通过环境变量或 .env 覆盖）----
export DB_PASSWORD="${DB_PASSWORD:-}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export JWT_ACCESS_SECRET="${JWT_ACCESS_SECRET:-dev-access-secret-change-me-1234567890abcdef}"
export JWT_REFRESH_SECRET="${JWT_REFRESH_SECRET:-dev-refresh-secret-change-me-1234567890abcdef}"
export MAIL_USERNAME="${MAIL_USERNAME:-}"
export MAIL_PASSWORD="${MAIL_PASSWORD:-}"
export CAMPUS_SCHOOL_EMAIL_DOMAIN="${CAMPUS_SCHOOL_EMAIL_DOMAIN:-@your-school.edu.cn}"
export CAMPUS_ADMIN_PASSWORD="${CAMPUS_ADMIN_PASSWORD:-}"

log()  { printf '\033[36m[启动]\033[0m %s\n' "$*"; }
ok()   { printf '\033[32m[OK]\033[0m %s\n' "$*"; }
warn() { printf '\033[33m[注意]\033[0m %s\n' "$*"; }
err()  { printf '\033[31m[失败]\033[0m %s\n' "$*" >&2; }

# ---- 依赖服务：PostgreSQL ----
pg_ready()  { command -v pg_isready  >/dev/null && pg_isready  -h localhost -p 5432        >/dev/null 2>&1; }
pg_start()  {
  if command -v brew >/dev/null 2>&1; then
    local f=postgresql@16
    brew services list 2>/dev/null | grep -q '^postgresql@16' || f=postgresql
    brew services start "$f" >/dev/null 2>&1
  else
    pg_ctl -D /usr/local/var/postgres  start >/dev/null 2>&1 \
      || pg_ctl -D /opt/homebrew/var/postgres start >/dev/null 2>&1
  fi
}

# ---- 依赖服务：Redis ----
redis_ready() { command -v redis-cli >/dev/null && redis-cli -h 127.0.0.1 -p 6379 ping 2>/dev/null | grep -q PONG; }
redis_start() { command -v brew >/dev/null 2>&1 && brew services start redis >/dev/null 2>&1; }

# ---- 确保数据库与 pgvector 扩展（最佳努力，失败仅告警）----
ensure_db() {
  command -v psql >/dev/null 2>&1 || { warn "未找到 psql，跳过数据库预检（请确认 campus_forum 已存在）"; return 0; }
  if psql -h localhost -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='campus_forum'" 2>/dev/null | grep -q 1; then
    ok "数据库 campus_forum 已存在"
  else
    log "创建数据库 campus_forum ..."
    createdb -h localhost -U postgres campus_forum 2>/dev/null || { warn "无法创建数据库，请手动创建"; return 0; }
  fi
  psql -h localhost -U postgres -d campus_forum -c "CREATE EXTENSION IF NOT EXISTS vector;" >/dev/null 2>&1 \
    && ok "扩展 vector 已就绪" || warn "无法确保 vector 扩展（Flyway V4 迁移需要它）"
}

# ---- 构建后端（jar 已存在则跳过）----
build_backend() {
  [ -f "$BACKEND_JAR" ] && { ok "后端 jar 已存在，跳过构建"; return 0; }
  log "构建后端（mvn 离线打包）..."
  ( cd "$BACKEND_DIR" && mvn -o package -DskipTests ) \
    || ( warn "离线构建失败，尝试联网构建..."; cd "$BACKEND_DIR" && mvn package -DskipTests ) \
    || { err "后端构建失败，请检查 Maven / 网络"; exit 1; }
}

# ---- 后端就绪探测 ----
backend_ready() { curl -s -o /dev/null --noproxy '*' "http://localhost:$BACKEND_PORT/" 2>/dev/null; }

# ---- 前端就绪探测 ----
frontend_ready() { curl -s -o /dev/null --noproxy '*' "http://localhost:$FRONTEND_PORT/" 2>/dev/null; }

alive() { [ -f "$1" ] && kill -0 "$(cat "$1" 2>/dev/null)" 2>/dev/null; }

log "== 校园论坛 一键启动 =="

# 1) PostgreSQL
if pg_ready; then ok "PostgreSQL 已在运行"; else
  log "启动 PostgreSQL ..."; pg_start
  local_i=0; until pg_ready; do local_i=$((local_i+1)); [ $local_i -ge 30 ] && { err "PostgreSQL 启动超时"; exit 1; }; sleep 1; done
  ok "PostgreSQL 已就绪"
fi

# 2) Redis
if redis_ready; then ok "Redis 已在运行"; else
  log "启动 Redis ..."; redis_start
  local_i=0; until redis_ready; do local_i=$((local_i+1)); [ $local_i -ge 30 ] && { warn "Redis 启动超时，继续（后端启动后可能报错）"; break; }; sleep 1; done
  redis_ready && ok "Redis 已就绪" || warn "Redis 未就绪，请手动启动"
fi

# 3) 数据库预检
ensure_db

# 4) 构建并启动后端
build_backend
if alive "$LOG_DIR/backend.pid"; then
  warn "后端已在运行（pid $(cat "$LOG_DIR/backend.pid")），跳过"
else
  log "启动后端（端口 $BACKEND_PORT）..."
  nohup java -jar "$BACKEND_JAR" --server.port=$BACKEND_PORT >> "$LOG_DIR/backend.log" 2>&1 &
  echo $! > "$LOG_DIR/backend.pid"
  disown
  local_i=0; until backend_ready; do local_i=$((local_i+1)); [ $local_i -ge 60 ] && { err "后端启动超时，详见 $LOG_DIR/backend.log"; exit 1; }; sleep 2; done
  ok "后端已就绪 (http://localhost:$BACKEND_PORT)"
fi

# 5) 前端
if alive "$LOG_DIR/frontend.pid"; then
  warn "前端已在运行（pid $(cat "$LOG_DIR/frontend.pid")），跳过"
else
  [ -d "$FRONTEND_DIR/node_modules" ] || { log "安装前端依赖（npm install）..."; ( cd "$FRONTEND_DIR" && npm install ); }
  log "启动前端（端口 $FRONTEND_PORT）..."
  # 注：在 WorkBuddy 沙箱中运行前端 dev 可能因缓存目录触发文件守卫，
  # 可设置 VITE_CACHE_DIR=/tmp/vite-campus-cache 再运行（正常机器无需）。
  [ -n "${VITE_CACHE_DIR:-}" ] && export VITE_CACHE_DIR
  nohup npm --prefix "$FRONTEND_DIR" run dev >> "$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "$LOG_DIR/frontend.pid"
  disown
  local_i=0; until frontend_ready; do local_i=$((local_i+1)); [ $local_i -ge 40 ] && { warn "前端启动超时，详见 $LOG_DIR/frontend.log"; break; }; sleep 2; done
  frontend_ready && ok "前端已就绪 (http://localhost:$FRONTEND_PORT)" || warn "前端可能仍在编译，请稍候查看 $LOG_DIR/frontend.log"
fi

ok "启动完成"
cat <<EOF

  前端:  http://localhost:$FRONTEND_PORT/
  后端:  http://localhost:$BACKEND_PORT/
  日志:  $LOG_DIR/backend.log
         $LOG_DIR/frontend.log

  停止:  ./stop.sh
EOF
