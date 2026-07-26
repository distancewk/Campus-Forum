#!/usr/bin/env bash
# 校园论坛 —— 一键停止脚本（停止后端 + 前端）
# 用法：./stop.sh

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT/.runtime-logs"

stop_one() {
  local name="$1" pidfile="$2" pattern="$3"
  if [ -f "$pidfile" ]; then
    local pid; pid="$(cat "$pidfile" 2>/dev/null)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null && echo "已停止 $name (pid $pid)"
    fi
    rm -f "$pidfile"
  fi
  # 兜底：按进程特征清理可能残留的子进程（如 vite）
  if pkill -f "$pattern" 2>/dev/null; then
    echo "已清理残留的 $name 进程"
  fi
}

stop_one "后端" "$LOG_DIR/backend.pid"  "campus-forum-1.0.0-SNAPSHOT.jar"
stop_one "前端" "$LOG_DIR/frontend.pid"  "campus-forum-frontend-new"

echo "停止完成（PostgreSQL / Redis 仍由 brew 托管，未停止）"
