#!/usr/bin/env bash
# 运行前端 lint / 单元测试 / 构建。
# 说明：typecheck 因 typescript 与 vue-tsc 版本不匹配暂时跳过（详见 TESTING.md）。
set -o pipefail
cd "$(dirname "$0")/.."
cd campus-forum-frontend-new
npm install --no-audit --no-fund
npm run lint
npm test
npm run build
echo "前端检查完成 ✅（typecheck 已知 broken，见 TESTING.md）"
