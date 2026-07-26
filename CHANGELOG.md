# 更新日志（Changelog）

本项目所有重要变更均记录于此。格式遵循 [Keep a Changelog](https://keepachangelog.com/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增
- 规划中的特性将记录在此处。

## [1.2.0] - Unreleased

### 安全修复（基于 `doc/security-review.md` 对抗性审查）
- **会话撤销（V-A/V-D/V-E/V-F）**：新增 `user.token_version` 列（V8 迁移）+ Redis 刷新令牌 `jti` 白名单（`RefreshTokenStore`）。登出/改密/封禁立即失效所有旧 token；JWT 过滤器回查 `tokenVersion`、用户状态与角色。
- **OTP 防爆破（V-B）**：验证码改存 Redis 并带尝试计数（5 次作废）+ 发送频控。
- **默认管理员（V-C）**：V7 迁移删除随仓库分发的硬编码默认管理员；`AdminInitializer` 在 `CAMPUS_ADMIN_PASSWORD` 为空时拒绝创建任何管理员。
- **账号枚举/时序（V-M/V-N/V-O/V-P/V-Q）**：注册/验证码返回统一模糊错误；邮箱域名锚定正则 + 拒绝占位符域名；用户不存在时恒定耗时；登录锁定覆盖 refresh。
- **AI 内容安全（V-G/V-H/V-K/V-L）**：审核默认开启且失败拒绝（`ContentRejectedException`，移除 `localFallback` ALLOW）；Q&A/审核结构化消息 + 提示注入护栏；RAG 入库前剥离注入标记并跳过可疑片段。
- **待审核帖子不公开（V-I）**：`PostMapper` 详情/列表查询加 `status = 1` 过滤。
- **后端消毒（V-J）**：新增 `HtmlSanitizer`（Jsoup），`title/nickname/bio` 落库前消毒，消除"仅前端防护"信任边界。
- **多维限流（V-R）**：新增 `@RateLimit` + `RateLimitInterceptor`（Redis 计数），覆盖发帖/评论/私信/AI/验证码。
- **异常不泄露（Low）**：`GlobalExceptionHandler` 500 返回通用文案。
- **WebSocket 鉴权（Low）**：CONNECT 阶段校验 token，无效拒绝连接。

### 测试
- 新增 13 个安全回归测试（会话撤销、OTP 防爆破、默认管理员、审核失败拒绝、待审核帖子不可见、后端消毒、限流、邮箱域名）；全部通过（20/20 安全测试 GREEN）。

## [1.1.0] - Unreleased

### 新增 (Phase 2)
- 前端渐进引入 TypeScript（api/stores 层类型定义）。
- Playwright E2E 测试覆盖核心链路（登录→发帖→评论→私信→搜索）。
- 版本管理规范（Conventional Commits + Semantic Versioning 标签）。
- 后端 `updated_at` 数据库触发器，自动维护更新时间。

### 修复 / 优化 (Phase 2)
- 消除 `MessageService.convertToVO` 的 N+1 查询。
- 统一 DOMPurify 白名单（`utils/sanitize.js`），前端富文本消毒一致。
- 学校邮箱域名配置化（`CAMPUS_SCHOOL_EMAIL_DOMAIN`）。
- CORS 收紧：明确允许来源与显式请求头，保留凭据。
- 引入 ESLint + Prettier，统一前后端代码风格。
- 同步 `doc/tasks/progress.md` 进度文档。

## [1.0.0] - 2026-07-26

### 新增
- 校园论坛初始实现：Spring Boot 后端 + Vue 3 前端。
- 核心模块：认证、用户、板块、帖子、评论、互动、私信、搜索、管理后台。
- 前端 ~16 个页面、10 个 API 模块及 AI 问答/知识库模块（`campus-forum-frontend-new`）。
- Docker 容器化（后端 + 前端 Nginx）与 `docker-compose.yml` 一键编排。
- CI 流水线（前后端测试 + 前端 lint/test）。
- `.env.example` 与部署文档 `doc/deploy.md`。

### 修复 (Phase 0 / Phase 1)
- P0：修复 `selectBoardStats` 引用不存在的 `b.deleted` 列（板块逻辑删除）；删除废弃旧前端 `campus-forum-frontend`；补充 `.gitignore`。
- P1：评论删除计数逻辑、登录锁定竞态、管理员密码改由环境变量注入（`AdminInitializer`）、提取 `formatTime` 公共函数、移除 WebSocket 全局变量 hack 改用 Pinia、补充后端/前端单测、CI/CD、Docker 容器化、`.env.example`、部署文档。
