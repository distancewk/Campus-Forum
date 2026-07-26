# Campus Forum · 校园论坛

一个面向高校的校园论坛系统，支持板块 / 帖子 / 评论 / 私信，并内置 AI 校园知识库问答与内容审核能力。后端基于 Spring Boot 3，前端基于 Vue 3。

> 当前版本：**v1.2.0** — 已完成 25 项缺陷修复与 26 项安全漏洞整改（详见 `doc/` 与 `CHANGELOG.md`）。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.2.5 · Java 17 · MyBatis-Plus 3.5.5 |
| 数据 | PostgreSQL 16 + pgvector · Redis 7 |
| 认证 | JWT（jjwt 0.12.5）访问/刷新双令牌 · Jsoup 1.17.2 消毒 |
| 文档 | Knife4j（API 文档）· Flyway（数据库迁移） |
| 前端 | Vue 3.5 · Vite 8 · Pinia 3 · Vue Router 4 · Axios · DOMPurify · Element Plus |
| 工程 | Docker Compose · GitHub Actions CI |

## 功能特性

- **用户**：学邮箱注册 / 登录、个人主页、密码修改
- **内容**：板块、帖子、评论、私信，支持富文本
- **AI**：校园知识库问答（RAG）、AI 内容审核
- **安全（v1.2.0 强化）**
  - JWT 访问 / 刷新双令牌轮换，刷新令牌带服务端状态（Redis `jti` 白名单）
  - `tokenVersion` 会话级撤销 —— 改密 / 封禁即时失效
  - OTP 验证码爆破防护（尝试次数上限 + 频控）
  - 基于 Redis 的接口限流（`@RateLimit` 注解拦截器）
  - 信任边界 HTML 消毒（Jsoup），防存储型 XSS
  - AI 审核默认开启且**失败拒绝（fail-closed）**，防 Prompt Injection 绕过

## 目录结构

```
Campus Forum/
├── campus-forum-backend/      # Spring Boot 后端
│   └── src/main/resources/db/migration/   # Flyway 迁移 V1–V8
├── campus-forum-frontend-new/ # Vue 3 + Vite 前端（活跃前端）
├── doc/                       # 优化方案 / 安全审查 / 整改方案 / 部署说明
├── docker-compose.yml         # 生产编排（postgres + redis + backend + frontend）
├── .env.example               # 环境变量模板
├── CHANGELOG.md
└── CONTRIBUTING.md
```

## 环境要求

- JDK 17
- Node.js 22+
- PostgreSQL 16（**需启用 `pgvector` 扩展**，见下方数据库迁移）
- Redis 7

## 快速开始（本地开发）

### 后端

参考 `.env.example` 设置环境变量，然后打包并启动（默认端口 `8080`）：

```bash
cd campus-forum-backend
mvn package -DskipTests
DB_PASSWORD= JWT_ACCESS_SECRET=xxx JWT_REFRESH_SECRET=yyy \
  java -jar target/campus-forum-1.0.0-SNAPSHOT.jar --server.port=8080
```

> 若本地已存在运行中的 PostgreSQL / Redis，直接连即可；否则先用下方 Docker Compose 起依赖。

### 前端

```bash
cd campus-forum-frontend-new
npm install
npm run dev        # http://localhost:5173
```

## 配置（环境变量）

复制 `.env.example` 为 `.env` 并填写。核心变量：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | PostgreSQL 密码（Docker 中用，本地也可通过 `spring.datasource` 覆盖） |
| `JWT_ACCESS_SECRET` | 访问令牌签名密钥（**务必在生产环境设置且足够随机**） |
| `JWT_REFRESH_SECRET` | 刷新令牌签名密钥 |
| `CAMPUS_ADMIN_STUDENT_NO` | 初始管理员学号（默认 `admin001`） |
| `CAMPUS_ADMIN_EMAIL` | 初始管理员邮箱 |
| `CAMPUS_ADMIN_PASSWORD` | 初始管理员密码；**留空则后端拒绝创建管理员**（防默认凭据接管） |
| `CAMPUS_SCHOOL_EMAIL_DOMAIN` | 学校邮箱域名（注册校验，默认 `@your-school.edu.cn`） |
| `VITE_API_BASE_URL` | 前端构建时注入的后端地址 |

> 邮件（SMTP）与 AI（大模型 API Key）相关变量用于完整功能，基础运行可不填。

## 数据库迁移

后端启动时 **Flyway 自动执行** `src/main/resources/db/migration/` 下的 `V1`–`V8` 脚本：

- `V4` 依赖 `pgvector` 扩展。本地 PostgreSQL 需先执行：

  ```sql
  CREATE EXTENSION IF NOT EXISTS vector;
  ```

- `V5` 板块软删除 · `V6` `updated_at` 自动触发器 · `V7` 移除默认管理员 · `V8` 用户 `token_version` 列。

## 测试

```bash
# 后端（含 20 项安全测试）
cd campus-forum-backend && mvn test

# 前端
cd campus-forum-frontend-new
npm run test        # Vitest 单元/组件测试
npm run e2e         # Playwright 端到端测试
npm run lint        # ESLint
npm run typecheck   # vue-tsc 类型检查
```

## 生产部署（Docker Compose）

```bash
cp .env.example .env   # 填写生产值（尤其 JWT 密钥与管理员密码）
docker compose up -d --build
```

服务：`postgres:16` / `redis:7` / `backend`（Spring `prod` 配置）/ `frontend`（Nginx 静态托管）。

## API 文档

后端启动后访问 Knife4j 文档：`http://localhost:8080/doc.html`。

## 文档与贡献

- 优化方案：`doc/optimization-plan.md`
- 安全审查：`doc/security-review.md`
- 安全整改方案：`doc/security-remediation-plan.md`
- 部署说明：`doc/deploy.md`
- 版本记录：`CHANGELOG.md`
- 贡献规范（分支策略 / Conventional Commits / SemVer / 打 tag）：`CONTRIBUTING.md`

## 许可证

本仓库未声明许可证，请在使用前与作者确认。
