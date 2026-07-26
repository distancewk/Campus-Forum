#!/usr/bin/env bash
# 一站式运行前后端测试（后端 hermetic + 前端 lint/test/build）。
set -o pipefail
cd "$(dirname "$0")/.."

echo "==> 后端测试 (mvn test + JaCoCo)"
bash scripts/test-backend.sh

echo "==> 前端测试 (lint + test + build)"
bash scripts/test-frontend.sh

echo "全部完成 ✅"
