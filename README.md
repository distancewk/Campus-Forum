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
├── docker-compose.yml         # 一键部署编排（pgvector/postgres + redis + backend + frontend）
├── .env.example               # 环境变量模板
├── CHANGELOG.md
└── CONTRIBUTING.md
```

## 环境要求

- 一键部署（推荐）：安装 [Docker](https://www.docker.com/) 与 [Docker Compose](https://docs.docker.com/compose/)（Docker Desktop 已内含）。无需本地 JDK / Node / 数据库。
- 本地开发：JDK 17、Node.js 22+、PostgreSQL 16（**需 `pgvector` 扩展**）、Redis 7。

## 快速开始（克隆即可部署）

### 方式 A · 一键部署（Docker Compose，推荐给克隆者）

只需装好 Docker，无需本地安装 JDK / Node / 数据库。数据库已内置 `pgvector` 扩展，克隆后直接可跑。

```bash
# 1) 克隆
git clone <your-repo-url> campus-forum
cd campus-forum

# 2) 生成并填写环境变量
cp .env.example .env
#    至少设置下面几个（用强随机值替换占位）：
#      DB_PASSWORD / JWT_ACCESS_SECRET / JWT_REFRESH_SECRET / CAMPUS_ADMIN_PASSWORD
#    AI 功能可选：CAMPUS_AI_API_KEY 等（见下方「设置 API Key」）；不填则发帖会被 AI 审核拦截。

# 3) 构建并启动（首次构建会下载依赖，稍慢）
docker compose up -d --build

# 4) 查看状态
docker compose ps
```

- 访问前端：**http://localhost**（容器内 Nginx 已把 `/api`、`/ws` 反代到后端，无需额外配置）。
- 后端 API 文档（Knife4j）：**http://localhost:8080/doc.html**
- 日志排查：`docker compose logs -f backend`

> 默认管理员：学号 `admin001`、邮箱取 `CAMPUS_ADMIN_EMAIL`、密码取 `CAMPUS_ADMIN_PASSWORD`（`CAMPUS_ADMIN_PASSWORD` 留空时后端**拒绝创建管理员**，避免默认凭据接管）。

### 方式 B · 本地开发（前后端分别启动）

```bash
# 后端
cd campus-forum-backend
mvn package -DskipTests
DB_PASSWORD= JWT_ACCESS_SECRET=xxx JWT_REFRESH_SECRET=yyy \
  java -jar target/campus-forum-1.0.0-SNAPSHOT.jar --server.port=8080

# 前端（另开终端，默认 http://localhost:5173）
cd campus-forum-frontend-new
npm install
npm run dev
```

> 本地开发如需 PostgreSQL / Redis，可直接 `docker compose up -d postgres redis` 仅起这两个依赖，后端连本地 5432 / 6379。

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
| `CAMPUS_AI_API_KEY` | 大模型 API Key（阿里云百炼 / DashScope），用于 **AI 内容审核**与**校园知识库问答**。见下方「设置 API Key」 |
| `CAMPUS_AI_ENABLED` | 是否启用 AI 能力（默认 `false`；dev 环境已设为 `true`） |
| `CAMPUS_AI_BASE_URL` | 兼容 OpenAI 接口的 API 基址（默认官方 OpenAI；dev 默认 `https://dashscope.aliyuncs.com/compatible-mode/v1`） |
| `CAMPUS_AI_CHAT_MODEL` | 聊天 / 审核模型（默认 `gpt-4o-mini`；dev 默认 `qwen-plus`） |
| `CAMPUS_AI_MODERATION_ENABLED` | 是否启用 AI 内容审核（默认 `true`） |
| `CAMPUS_AI_MODERATION_MODE` | 审核模式：`post`=先发布后复核（**默认**，不阻断发帖/评论）；`pre`=先审后发；`off`=不审核直接发布 |

> 邮件（SMTP）变量用于邮箱验证码等完整功能；AI 相关变量用于 AI 审核与问答。二者基础运行可不填。
> **发布策略（第一性原理）**：默认 `CAMPUS_AI_ENABLED=false`（未配 Key）或 `CAMPUS_AI_MODERATION_ENABLED=false` 时，发帖/评论**直接公开可见**（已做 HTML 清洗保底），AI 审核**不调用、不阻断**。仅当 `CAMPUS_AI_ENABLED=true` 且 `CAMPUS_AI_MODERATION_ENABLED=true` 时 AI 才介入；此时默认 `mode=post` 为"先发布、后台复核"，AI 明确判拒才下架；设 `mode=pre` 才回到"先审后发"。这样**不接 AI 也能正常发帖/评论**。

## 设置 API Key（启用 AI 功能）

AI 能力依赖一个大模型 API Key，通过环境变量 `CAMPUS_AI_API_KEY` 注入。**该变量只写在 `.env` 文件中（已被 `.gitignore` 忽略，不会提交），切勿写入任何被版本控制的文件。**

### 方式一：本地开发（接入阿里云百炼 / DashScope）

1. 登录 [阿里云百炼控制台](https://dashscope.console.aliyun.com/)，开通模型服务并**创建 API Key**（形如 `sk-xxxxxxxx`）。
2. 在项目根目录的 `.env` 中填入（`.env.example` 已含模板）：

   ```bash
   # 后端根目录 .env
   CAMPUS_AI_API_KEY=sk-你的真实key
   CAMPUS_AI_ENABLED=true
   CAMPUS_AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
   CAMPUS_AI_CHAT_MODEL=qwen-plus
   CAMPUS_AI_MODERATION_ENABLED=true
   CAMPUS_AI_MODERATION_MODE=post   # 先发布后复核（默认）；pre=先审后发
   ```

3. 后端 `application-dev.yml` 已默认指向 DashScope，直接启动即可：

   ```bash
   cd campus-forum-backend
   mvn package -DskipTests
   CAMPUS_AI_API_KEY=sk-你的真实key java -jar target/campus-forum-1.0.0-SNAPSHOT.jar --server.port=8080
   ```

> 也可把 `CAMPUS_AI_API_KEY` 等统一写进根目录 `.env`，启动脚本 `start.sh` 会自动 `export` 这些变量。

### 方式二：生产环境（Docker Compose）

编辑根目录 `.env`（由 `.env.example` 复制而来），至少填：

```bash
CAMPUS_AI_API_KEY=sk-你的真实key
CAMPUS_AI_ENABLED=true
CAMPUS_AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1   # 或你选用的兼容 OpenAI 接口的服务
CAMPUS_AI_CHAT_MODEL=qwen-plus
CAMPUS_AI_MODERATION_ENABLED=true
CAMPUS_AI_MODERATION_MODE=post   # 先发布后复核（默认）；pre=先审后发
```

然后 `docker compose up -d --build` 启动，`backend` 服务会读取这些变量。

### 重要提醒

- **不要提交 `.env`**：`.gitignore` 已忽略 `.env` 与 `.env.*`（仅 `.env.example` 被提交，且其中不含真实值）。若曾误提交，需从 git 历史中清除并**立即吊销旧 Key**。
- **Key 即凭证**：一旦 `.env` 中的 Key 可能已泄露，请到对应平台**吊销并重新生成**，再更新本地 `.env`。
- **未配置 Key 的后果**：AI 审核默认开启且 fail-closed，缺 Key 时发帖/评论会被审核拦截（拒绝发布），这是预期的保护行为，不是 bug。
- 支持的接口形态：任意**兼容 OpenAI `/chat/completions`** 的服务均可（配置 `CAMPUS_AI_BASE_URL` 与 `CAMPUS_AI_CHAT_MODEL` 即可切换，如 OpenAI、其他国产大模型网关等）。

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

## 生产部署

生产环境与上方「快速部署（方式 A）」使用同一套 `docker-compose.yml`，只需在 `.env` 中填入**强随机**的值：

- 必填：`DB_PASSWORD`、`JWT_ACCESS_SECRET`、`JWT_REFRESH_SECRET`、`CAMPUS_ADMIN_PASSWORD`
- AI 功能：`CAMPUS_AI_API_KEY`（见「设置 API Key」）；不填则发帖会被 AI 审核拦截
- 可选邮件：`MAIL_USERNAME` / `MAIL_PASSWORD`（用于邮箱验证码）

```bash
cp .env.example .env   # 填写上述生产值
docker compose up -d --build
```

服务：`pgvector/pgvector:pg16`（已含向量扩展）/ `redis:7` / `backend`（Spring `prod` 配置）/ `frontend`（Nginx 静态托管，反代 `/api`、`/ws` 到后端）。

> 生产建议：用反向代理（Nginx / Caddy）终结 HTTPS 并把 `CAMPUS_COOKIE_SECURE` 设为 `true`；密钥务必用随机值且勿提交；如需自定义前端 API 地址，构建前设置 `VITE_API_BASE_URL`（Docker 部署下可不设，走同源反代）。

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

<!-- auto-generated: readme-links:start -->
## Project navigation

<!-- generated-from: sha256:5b57ff9c7b1f2038d65744e95a058c42dddf4a6026b85c00d751ffb452cc23f6 -->
This project was classified as **large** for documentation depth.

- [Project map](docs/PROJECT_MAP.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Code guide](docs/CODE_GUIDE.md)
<!-- auto-generated: readme-links:end -->
