#!/usr/bin/env bash
# 运行后端测试并生成 JaCoCo 覆盖率报告。
# 完全 hermetic，无需启动 Postgres / Redis。
set -o pipefail
cd "$(dirname "$0")/.."
cd campus-forum-backend
mvn test
echo "覆盖率报告见 campus-forum-backend/target/site/jacoco/index.html"
