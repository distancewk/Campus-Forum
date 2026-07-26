# Campus Forum 安全漏洞修复计划（多 Agent 执行）

> 基线：`doc/security-review.md` 审查结论（Critical 3 / High 6 / Medium 9 / Low 8）
> 日期：2026-07-26 ｜ 执行方式：3 路并行 Agent（认证会话 / 内容滥用 / 回归测试），文件归属零冲突
> 依赖现状：`spring-boot-starter-data-redis`、`org.jsoup:jsoup` **已在 pom.xml**，无需新增依赖

## 统一设计原则

1. **单点真相在后端**：所有"前端消毒 / 前端频控"类信任边界错误，一律在后端补最后一道防线。
2. **会话撤销用 `tokenVersion` + Redis 刷新令牌白名单**（统一解 V-A/V-D/V-E/V-F）。
3. **文件零冲突划分**：Agent A 独占 `application.yml`/`application-dev.yml` 与 `auth`/`config`/`websocket` 包；Agent B 独占 `ai`/`post`/`user` 包、`GlobalExceptionHandler`、`common/ratelimit` 包，且**只用 `@Value` 默认值、不碰 yml**；Agent C 只写 `src/test`。

---

## Agent A — 认证与会话 / 账号安全（Critical + 会话撤销 + 枚举）

负责文件：`com.campus.auth.**`、`com.campus.**.config.SecurityConfig`、`com.campus.**.config.WebSocketConfig`、`com.campus.**.filter.JwtAuthenticationFilter`、`com.campus.auth.service.AdminInitializer`、`src/main/resources/db/migration/V7__*.sql`、`V8__*.sql`、`application.yml`、`application-dev.yml`。

### V-A / V-D / V-E / V-F — 会话撤销核心
- 新增迁移 `V8__user_token_version.sql`：`ALTER TABLE "user" ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;`
- 新增 `com.campus.auth.token.RefreshTokenStore`（用 `StringRedisTemplate`）：
  - `issue(userId)`：生成 `jti=UUID`，`SET campus:rt:{jti} {userId} EX {rtTtl}`，返回 jti；
  - `validate(jti)`：存在且未过期返回 userId，否则抛 `InvalidTokenException`；
  - `revoke(jti)`：`DEL campus:rt:{jti}`。
- Access Token 负载加 `tv`（=user.tokenVersion）；Refresh Token 负载加 `jti` + `tv`。
- `AuthService`：
  - `login`/`refreshToken`：签发时调用 `refreshTokenStore.issue`；`refreshToken` 先 `validate(jti)`，再 `revoke(oldJti)` + 重新 `issue`（轮换，旧 jti 立即失效）；
  - `logout(userId, jti)`：调用 `userMapper.incrTokenVersion(userId)` 并 `refreshTokenStore.revoke(jti)` → 所有 access/refresh 立即失效；
  - `resetPassword`：更新哈希后 `incrTokenVersion` + `revoke` 当前 jti（V-E 已解）。
- `JwtAuthenticationFilter`：签名+过期校验通过后，查 `user`，比对 `claim.tv == user.tokenVersion`；否则清 `SecurityContext` 返回 401。同时校验 `user.status` 非封禁、`role` 与 claim 一致（V-F：封禁/改权≤一次请求内生效）。

### V-B — OTP 防爆破
- `AuthService` 生成/校验验证码改用 `RefreshTokenStore` 同款 Redis：
  - 存 `campus:otp:{purpose}:{account}` = code，TTL 300s；
  - 另存 `campus:otp:{purpose}:{account}:att` 尝试计数，每次校验 +1，达到 5 次 `DEL` 验证码并锁定该账号验证码 10 分钟；
  - 同一账号/手机号 60s 内不可重复发送（发送频控）。

### V-C — 删除默认管理员
- 新增迁移 `V7__remove_default_admin.sql`：
  ```sql
  -- 移除随仓库分发的硬编码默认管理员（V2 种子），改由 AdminInitializer 从环境变量注入
  DELETE FROM "user" WHERE student_no = 'admin001' AND role = 'ADMIN' AND email = 'admin@your-school.edu.cn';
  ```
- `AdminInitializer`（Phase 1 已建）：若 `campus.admin.password` 为空 → `log.error("未配置 CAMPUS_ADMIN_PASSWORD，不创建任何管理员账户")` 并直接 return（**不创建默认账号、不复用 V2 种子**）；非空才用 BCrypt 创建/更新。

### V-M / V-N / V-O / V-P / V-Q — 账号枚举与时序 & 锁定
- `V-M 注册枚举`：`register` 与 `sendOtp` 对"账号/邮箱已存在""验证码错误"返回**完全一致**的 generic 错误（如 `操作失败，请稍后重试`），不暴露具体原因。
- `V-N/O 邮箱域名`：`validateSchoolEmail` 用锚定正则 `^@[\w.-]+\.edu\.cn$`；若 `campus.school-email-domain` 等于占位符 `@your-school.edu.cn`，注册直接拒绝（要求部署方配置真实域名）。
- `V-P 时序枚举`：用户不存在时仍对**固定 dummy BCrypt 哈希**执行一次 `matches`，使响应耗时恒定。
- `V-Q 锁定`：登录失败锁定逻辑扩展到 refresh 端点（refresh 时也查 `lockedUntil`）；保持唯一索引兜底防竞态（沿用 Phase 1 的 `selectById` 真实计数）。

### WebSocket 鉴权（Low）
- `WebSocketConfig`：`StompSubProtocolErrorHandler` / `ChannelInterceptor` 在 CONNECT 阶段校验 `Authorization` token，无效→拒绝连接。

### 配置（仅 A 改 yml）
`application.yml` / `application-dev.yml` 增加：`campus.jwt.access-ttl`（默认 900）、`campus.jwt.refresh-ttl`（默认 604800）、`campus.redis.rt-prefix`（默认 `campus:rt:`）、`campus.otp.*`、`campus.admin.*`（Phase 1 已有）。

---

## Agent B — 内容安全 / 滥用面

负责文件：`com.campus.ai.**`、`com.campus.post.service.PostService`、`src/main/resources/mapper/PostMapper.xml`、`com.campus.user.service.UserService`、`com.campus.**.exception.GlobalExceptionHandler`、`com.campus.common.ratelimit.**`（新建）、WebMvcConfigurer（新建或既有）。**不修改任何 yml，阈值用 `@Value` 默认值**。

### V-G — AI 审核默认开启 + 失败拒绝
- `AiModerationService`：`enabled` 默认值改为 `true`（yml 同步，但 B 用 `@Value("${campus.ai.moderation-enabled:true}")`）；审核异常/超时 → **拒绝发布**（抛 `RejectedException`），移除 `localFallback` 的 ALLOW 分支（改为拒绝或人工队列）；`enabled=false` 时也走拒绝而非放行。

### V-H / V-K / V-L — Prompt Injection 护栏
- `OpenAiCompatibleClient` / `AiQuestionAnswerService`：system prompt 与用户内容**结构分离**（用 messages 数组而非字符串拼接），并在内容前后加不可绕过的分隔标记；审核判定改用结构化 JSON 输出 + 关键词/规则护栏，禁止内容中的"忽略以上/ignore previous/system"等指令改变判定。
- `KnowledgeIngestionService`：入库前剥离/转义 Markdown 代码块与可执行指令，拒绝含疑似 prompt 注入的片段（记录并告警，不静默入库）。

### V-I — 待审核/已拒绝帖子不公开
- `PostMapper.xml` 的 `selectPostDetail`（及列表查询）加 `AND p.status = 1`（已发布）；非作者访问非发布态返回 null/404。`PostService` 详情方法对非作者、非管理员且 `status != 1` 返回 404。

### V-J — 后端 Jsoup 消毒（信任边界）
- 新建 `com.campus.common.util.HtmlSanitizer`（Jsoup `Whitelist.basic()` 或 `none()` 用于纯文本字段）：
  - `PostService` 标题/正文落库前消毒（标题按纯文本 `clean`，正文按 basic）；
  - `UserService` 昵称/简介落库前按纯文本 `clean`。
- 前端 DOMPurify 保留为纵深防御，但后端为单点真相。

### V-R — 多维限流
- 新建 `com.campus.common.ratelimit`：
  - `@RateLimit(key = "#userId", limit = 10, window = 60)` 注解；
  - `RateLimitInterceptor`（`HandlerInterceptor`）：用 `StringRedisTemplate` 计数器 `campus:rl:{scope}:{key}:{window}` INCR+EXPIRE，超限返回 429；
  - `WebMvcConfigurer` 注册拦截器，并在 `PostController`/`CommentController`/`MessageController`/`AiQuestionAnswerController`/`AuthController(otp)` 的写操作上标注 `@RateLimit`，阈值经 `@Value` 默认（如发帖 10/分、私信 30/分、AI 20/分、OTP 发送 5/分/IP）。

### Low — 异常不泄露
- `GlobalExceptionHandler`：未预期异常（500）返回通用文案，**不**回传 `e.getMessage()`；业务异常（4xx）保持既有中文 message（不泄露内部实现）。

---

## Agent C — 安全回归测试（仅 `src/test`）

- 用 `Mockito` mock `RefreshTokenStore`/`StringRedisTemplate`，用 `@WebMvcTest` + `spring-security-test` 验证端点。
- 覆盖：
  - V-D/V-E/V-F：tokenVersion 不匹配 → 401；logout 后旧 token 失效；resetPassword 后旧 refresh 失效。
  - V-B：OTP 连续错误 5 次作废；发送频控。
  - V-C：`campus.admin.password` 为空时 `AdminInitializer` 不创建管理员。
  - V-G：审核服务在 `enabled=false`/异常时拒绝发布。
  - V-I：非作者访问 `status!=1` 帖子详情 → 404/空。
  - V-J：标题/昵称含 `<script>` 落库后被剥离。
  - V-R：同一用户超阈值 → 429。
  - V-N：非法邮箱域名被拒。
- 若 `mvn -o test` 因离线缺依赖失败，至少保证 `mvn -o compile` 通过。

---

## 验证与交付

1. 三路并行完成后，我统一抽查：`V8` 迁移、`JwtAuthenticationFilter` tv 比对、`RefreshTokenStore`、`PostMapper status=1`、`HtmlSanitizer`、`@RateLimit` 注册。
2. 后端 `mvn -o package -DskipTests` 复验可打包；本地起服验证 `/api/posts` 受保护、pending 帖子不可见。
3. 更新 `doc/security-review.md` 顶部状态横幅；`CHANGELOG.md` 新增 `## [1.2.0]` 记录安全修复。

## 风险与取舍

- **Fail-fast vs 不阻断启动**：V-C 采用"密码为空则不创建管理员、仅记错误日志、应用照常启动"，避免破坏本地开发，同时彻底消除默认凭据。
- **刷新令牌白名单需 Redis**：本地 Redis 已在 6379 运行；离线测试用 Mockito mock，不依赖真 Redis。
- **不引入新依赖**：Jsoup / Redis 已具备。
