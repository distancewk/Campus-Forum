# Campus Forum 对抗性安全审查报告

> 审查视角：攻击者 / 逻辑漏洞挖掘。不依赖扫描器，逐文件通读 + 交叉调用链验证。
> 覆盖范围：认证与会话、授权与资源越权（IDOR/提权）、AI 模块、管理员模块、输入滥用面。
> 日期：2026-07-26 ｜ 代码基线：Phase 0/1/2 全部完成后
> **修复状态：✅ 已完成（2026-07-26，多 Agent 并行执行）** —— 详见 `doc/security-remediation-plan.md` 与 `CHANGELOG.md` 的 `1.2.0`。20/20 安全回归测试通过，后端 `mvn package` 成功。

## 一、总体结论

项目在**资源归属校验**上实现扎实（IDOR 类预期漏洞基本不存在），但在**会话撤销、账号恢复强度、AI 内容安全、后端消毒信任边界**上存在可被直接利用的逻辑漏洞。

- Critical 3 ｜ High 6 ｜ Medium 9 ｜ Low 8（约 26 项，另 9 项"预期漏洞"经核实为已防护）
- 最危险链路：**验证码爆破 → 账户接管 → refresh token 无撤销使会话无法处置**，且默认管理员哈希可构成独立接管入口。

## 二、关键漏洞清单

### Critical（必须立即修复）

| 编号 | 漏洞 | 位置 | 攻击路径 |
|------|------|------|----------|
| V-A | Refresh Token 轮换无服务端状态，旧 token 永不失效 | AuthService.java:251-276 | 窃得 refresh token 即可无限续期；登出/改密均不能让攻击者下线 |
| V-B | OTP 验证码可在线爆破 | AuthService.java:137-149,301-334,346-351 | 6 位数字、300s 有效、校验接口无限尝试 → 重置/冒名任意账户 |
| V-C | 硬编码默认管理员 BCrypt 凭据 | V2__init_data.sql:16-17 | 仓库分发固定哈希；未设 `CAMPUS_ADMIN_PASSWORD` 即 ADMIN 接管全站 |

### High

| 编号 | 漏洞 | 位置 |
|------|------|------|
| V-D | 登出仅清 Cookie，token 未失效 | AuthService.java:280-282 |
| V-E | 重置密码不撤销已有 token（改密后旧 refresh 仍续期） | AuthService.java:321-328 |
| V-F | JWT 过滤器不回查 user.status/role，封禁/权限变更滞后≤2h | JwtAuthenticationFilter.java:28-37 |
| V-G | AI 审核默认失效（`enabled=false`→全放行，localFallback 默认 ALLOW） | application.yml:33；AiModerationService.java:38,82-98 |
| V-H | 审核 Prompt Injection：帖子内容注入指令绕过审核 | OpenAiCompatibleClient.java:103-115 |
| V-I | 待审核/已拒绝帖子公开可读（详情 SQL 缺 `status=1`，且 `GET /api/posts/*` permitAll） | PostMapper.xml:74-98；SecurityConfig.java:45 |

### Medium

| 编号 | 漏洞 | 位置 |
|------|------|------|
| V-J | 存储型 XSS：title/nickname/bio 后端未消毒，仅靠前端的 DOMPurify | PostService.java:97；UserService.java:54-67 |
| V-K | Q&A 直接 Prompt Injection：泄露 system prompt / 伪造官方回答 | AiQuestionAnswerService.java:105-117 |
| V-L | RAG 知识库投毒：审核通过帖/精华评论入库无注入护栏 | KnowledgeIngestionService.java:44-66 |
| V-M | 注册接口账号/邮箱枚举（差异错误码） | AuthService.java:102-111 |
| V-N | 学校邮箱 `endsWith` 子域绕过 | AuthService.java:338-344 |
| V-O | 学校邮箱默认占位符 `@your-school.edu.cn` 默认可注册 | application.yml:54 |
| V-P | 登录基于响应时长枚举用户名 | AuthService.java:206-227 |
| V-Q | 登录锁定不覆盖 refresh + 可被用于 DoS 任意账号 | AuthService.java:353-368,217 |
| V-R | 限流缺失：私信/邮件验证码/AI/发帖评论无多维频控 | MessageService.java:31；AuthService.java:65-94；AiQuestionAnswerService.java:90-103 |

### Low（加固项）

- 审核置信度阈值 `medium/highThreshold` 配置死代码从未生效（AiProperties.java:40-41）
- AI 文档上传缺大小/魔数/文件名净化（AdminKnowledgeService.java:29-43；maxFileSize 未读取）
- 业务异常 `message` 直接回传前端（GlobalExceptionHandler.java:16）
- WebSocket CONNECT 无 token 未被拒绝（WebSocketConfig.java:61-77）
- 管理员用户列表返回完整学号+邮箱（UserAdminVO.java:12,16）
- 私信无删除/撤回端点（逻辑删除字段未用）
- JWT 缺显式 `requireAlgorithm/iss/aud`；prod profile 缺 `campus.jwt.*` 配置
- CSRF 全局关闭 + Cookie Refresh（CORS 误配通配即 token 泄露）
- 弱口令策略（min 6，无复杂度）

## 三、已确认安全的点（务必保留）

- **IDOR 全部正确**：删他人帖子/评论、越权置顶、读任意私信会话、点赞收藏越权/负数计数、改他人资料/密码 —— 均经 `SecurityUtil.requireCurrentUserId()` 比对 owner 或角色。
- **管理员端点双重鉴权**：类级 `@PreAuthorize("hasRole('ADMIN')")` + URL 级 `/api/admin/**`。
- **SQL 无注入**：全局无 `${}` 拼接（仅配置占位符），pgvector 检索参数化。
- **文件上传魔数校验到位**（FileUtil 的 isJpeg/isPng/isGif/isWebp + UUID 重命名 + 路径穿越防护）。
- **JWT 无 alg 混淆**（固定 HMAC 密钥，拒绝 none）。
- **WebSocket senderId 绑定 Principal**，无法伪造发信者。
- **全局异常不泄露堆栈**；登录 5 次失败锁 15 分钟；注册竞态由唯一索引兜底。

## 四、修复优先级

1. **Critical（本周）**：
   - 引入 refresh token 家族/Redis 黑名单（统一解 V-A/V-D/V-E）。
   - OTP 校验加尝试计数（5 次作废）+ 提熵 + 注册/重置 IP+账号限流（V-B）。
   - 删除 V2 默认管理员行；`AdminInitializer` 在密码为空时拒绝启动（V-C）。
2. **High（两周内）**：JWT 过滤器回查 status/role 或 `tokenVersion`（V-F）；AI 关闭时默认转人工而非放行、移除 `localFallback` ALLOW、加审核护栏（V-G/V-H/V-阈值）；`selectPostDetail` 加 `status=1` 过滤（V-I）。
3. **Medium/Low**：补 title/nickname/bio 后端 Jsoup 消毒（V-J）；Q&A/RAG 指令护栏（V-K/V-L）；注册防枚举（V-M）；邮箱域名正则锚定+非空强校验（V-N/V-O）；登录统一 BCrypt 耗时（V-P）；限流（V-R）；其余加固项。

## 五、方法论备注

本结论基于 `SecurityUtil.requireCurrentUserId()` 在所有变更型 Service 中与记录的 `authorId` 比对；WebSocket 发送者身份来自 CONNECT 阶段经校验的 JWT Principal 而非请求体。**"仅前端防护、后端未校验"是核心信任边界错误**（V-J、V-B、V-R），必须在后端补最后一道防线。
