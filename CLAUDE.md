# CLAUDE.md — Campus Forum

面向本项目 AI 助手的项目规则与结构速查。外部接入请看 `README.md`。

## 项目结构
- `campus-forum-backend/` — Spring Boot 3.2.5 / Java 17 后端（活跃）。
- `campus-forum-frontend-new/` — Vue 3.5 / Vite 8 前端（活跃）。旧 `campus-forum-frontend/` 已删除。
- `doc/` — 优化方案 / 安全审查 / 整改方案 / 部署说明。
- `docs/` — 仅保留 `docs/superpowers/`（历史计划；远端已移除，本地待清理）。
- `docker-compose.yml` — 生产编排（postgres + redis + backend + frontend）。
- `.env.example` — 环境变量模板；`.commitlintrc.json` — 提交规范。

## 技术栈（硬事实）
- 后端：MyBatis-Plus 3.5.5、PostgreSQL 16 + **pgvector**、Redis 7、jjwt 0.12.5、Jsoup 1.17.2、Knife4j。
- 前端：Pinia 3、Vue Router 4、Axios、DOMPurify、Element Plus。
- DB 迁移：Flyway `src/main/resources/db/migration/` V1–V8。**V4 依赖 pgvector 扩展**，新环境须先 `CREATE EXTENSION vector;`。

## 运行（速查）
- 后端端口 8080；前端 dev 5173。**前端 dev 绑 `[::1]`，必须用 `localhost` 访问**（非 127.0.0.1）。
- 本地依赖：PostgreSQL 16（含 pgvector）+ Redis 6379。
- 后端启动（需环境变量）：`mvn package -DskipTests` → `java -jar target/campus-forum-1.0.0-SNAPSHOT.jar --server.port=8080`。
- 生产：`docker compose up -d --build`。
- API 文档（Knife4j）：`http://localhost:8080/doc.html`。

## 红线 / 必须遵守
- **`.workbuddy/` 已在 .gitignore，禁止 `git add` 提交**（含助手与项目记忆）。
- **所有密钥走环境变量 `${ENV_VAR}`，禁止硬编码**到源码或 yml。
- 提交信息须符合 Conventional Commits（类型：feat/fix/docs/refactor/test/chore；subject ≤72 字符）。
- 发布打 annotated tag（如 `v1.2.0`）；版本遵循 SemVer。
- 新增/修改 API 路由、环境变量、数据表时，同步更新 `README.md` 与 `doc/` 相关文档。

## 深入文档
- 安全模型与漏洞清单：`doc/security-review.md` + `doc/security-remediation-plan.md`
- 优化方案与缺陷清单：`doc/optimization-plan.md`
- 部署细节：`doc/deploy.md`
- 变更记录：`CHANGELOG.md`
- 贡献规范：`CONTRIBUTING.md`
