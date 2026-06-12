# 公共模块 (Common) — 任务清单

> 前置依赖：无（本模块是其他所有模块的基础）

---

## 任务 1：项目初始化与基础配置

- [ ] 1.1 使用 Spring Initializr 创建 Spring Boot 3 项目（Maven，JDK 17+）
- [ ] 1.2 配置 `pom.xml` 依赖：spring-boot-starter-web、spring-boot-starter-security、spring-boot-starter-validation、spring-boot-starter-websocket、spring-boot-starter-mail、mybatis-plus-boot-starter、postgresql、jjwt-api/impl/jackson、knife4j-openapi3-jakarta-spring-boot-starter、spring-boot-starter-data-redis、flyway-core、log4j2（排除默认 logback）
- [ ] 1.3 创建 `application.yml` 主配置（端口、profiles、multipart、flyway、mybatis-plus、logging）
- [ ] 1.4 创建 `application-dev.yml` 开发配置（数据源、Redis、邮件、JWT 密钥/过期时间、上传路径）
- [ ] 1.5 创建 `log4j2.xml` 日志配置（控制台 + 文件滚动）
- [ ] 1.6 创建启动类 `CampusForumApplication.java`
- [ ] 1.7 验证：项目能 `mvn spring-boot:run` 成功启动（数据库可暂用 H2 内存库）

---

## 任务 2：数据库初始化 (Flyway)

- [ ] 2.1 创建 Flyway 迁移文件 `V1__init_schema.sql`（user、board、post、comment、like、favorite、message、verify_code 共 8 张表）
- [ ] 2.2 启动项目验证 Flyway 自动建表成功
- [ ] 2.3 创建 `V2__init_data.sql` 插入预设板块数据（校园公告、二手交易、失物招领、表白墙、学习交流、灌水区）和管理员账号
- [ ] 2.4 验证：查询数据库确认表结构和初始数据正确

---

## 任务 3：统一响应体与枚举

- [ ] 3.1 创建 `R<T>` 统一响应体（code/message/data，静态工厂方法 ok/fail）
- [ ] 3.2 创建 `ResultCode` 枚举（SUCCESS、PARAM_ERROR、UNAUTHORIZED、FORBIDDEN、NOT_FOUND、CONFLICT、各业务错误码 410-421、INTERNAL_ERROR）
- [ ] 3.3 创建 `PageQuery` 分页请求基类（page 默认 1，size 默认 20，上限 50）
- [ ] 3.4 创建 `PageResult<T>` 分页响应封装（records/total/page/size/pages）
- [ ] 3.5 编写单元测试：`R` 序列化格式验证、`PageQuery` 校验注解验证

---

## 任务 4：全局异常处理

- [ ] 4.1 创建 `BusinessException` 自定义业务异常（code + message）
- [ ] 4.2 创建 `GlobalExceptionHandler`（@RestControllerAdvice）
- [ ] 4.3 处理 `BusinessException` → R.fail(code, message)
- [ ] 4.4 处理 `MethodArgumentNotValidException` → 提取第一条校验错误信息
- [ ] 4.5 处理 `AccessDeniedException` → R.fail(403)
- [ ] 4.6 处理 `Exception` → R.fail(500) + 日志记录
- [ ] 4.7 编写集成测试：验证各异常类型返回正确的 JSON 格式

---

## 任务 5：MyBatis-Plus 配置

- [ ] 5.1 创建 `MyBatisPlusConfig`（分页插件 PaginationInnerInterceptor）
- [ ] 5.2 配置 `@MapperScan("com.campus.*.mapper")`
- [ ] 5.3 配置逻辑删除全局策略（deleted 字段）
- [ ] 5.4 配置下划线转驼峰
- [ ] 5.5 验证：后续模块 Mapper 可正常注入使用

---

## 任务 6：JWT 工具类

- [ ] 6.1 创建 `JwtUtil`（注入配置的 secret 和 expiration）
- [ ] 6.2 实现 `generateAccessToken(userId, role)` → JWT 字符串
- [ ] 6.3 实现 `generateRefreshToken(userId)` → JWT 字符串
- [ ] 6.4 实现 `parseAccessToken(token)` → Claims
- [ ] 6.5 实现 `validateAccessToken(token)` → boolean
- [ ] 6.6 实现 `getUserIdFromAccessToken(token)` → Long
- [ ] 6.7 实现 `getRoleFromAccessToken(token)` → String
- [ ] 6.8 编写单元测试：Token 生成/解析/过期/篡改检测

---

## 任务 7：Spring Security 配置

- [ ] 7.1 创建 `SecurityConfig`（@Configuration, @EnableWebSecurity, @EnableMethodSecurity）
- [ ] 7.2 配置 CORS（CorsConfigurationSource，允许 localhost:5173）
- [ ] 7.3 禁用 CSRF，设置 SessionPolicy.STATELESS
- [ ] 7.4 配置 URL 权限规则：`/api/auth/**` permitAll，GET `/api/boards`/`/api/posts`/`/api/posts/{id}` permitAll，`/api/admin/**` hasRole('ADMIN')，其余 authenticated
- [ ] 7.5 配置 AuthenticationEntryPoint（401 JSON 响应）
- [ ] 7.6 配置 AccessDeniedHandler（403 JSON 响应）
- [ ] 7.7 注册 Bean PasswordEncoder（BCryptPasswordEncoder）
- [ ] 7.8 创建 `JwtAuthenticationFilter`（OncePerRequestFilter，从 Authorization header 提取 Token，验证后设置 SecurityContext）
- [ ] 7.9 将 JwtAuthenticationFilter 注册到 FilterChain（addFilterBefore UsernamePasswordAuthenticationFilter）
- [ ] 7.10 创建 `SecurityUtil` 工具类（getCurrentUserId()，从 SecurityContext 获取）
- [ ] 7.11 编写集成测试：未认证请求返回 401，无权限请求返回 403，正常请求通过

---

## 任务 8：文件上传工具

- [ ] 8.1 创建 `FileUtil`（@Component，注入 uploadPath/allowedTypes/maxSize）
- [ ] 8.2 实现 `upload(file, subDir)` → 相对路径
- [ ] 8.3 校验逻辑：文件非空、类型匹配（image/jpeg,image/png,image/gif,image/webp）、大小 ≤ 5MB
- [ ] 8.4 UUID 重命名 + 按日期生成子目录（uploads/avatar/2026/05/27/xxx.jpg）
- [ ] 8.5 配置静态资源映射（`/uploads/**` → 本地文件路径）
- [ ] 8.6 编写单元测试：文件名校验、类型校验、大小校验、路径生成

---

## 任务 9：邮件发送工具

- [ ] 9.1 创建 `EmailUtil`（@Component，注入 JavaMailSender）
- [ ] 9.2 实现 `sendVerifyCode(email, code)` → 发送验证码邮件
- [ ] 9.3 邮件模板：标题"校园论坛验证码"，正文含验证码和 5 分钟过期提示
- [ ] 9.4 使用 `@Async` 异步发送，避免阻塞主线程
- [ ] 9.5 配置 `@EnableAsync` 到启动类
- [ ] 9.6 编写单元测试：Mock JavaMailSender 验证发送逻辑

---

## 任务 10：Redis 工具类

- [ ] 10.1 创建 `RedisUtil`（@Component，注入 StringRedisTemplate）
- [ ] 10.2 实现 `set(key, value, timeout)` / `get(key)` / `delete(key)`
- [ ] 10.3 实现 `increment(key)`（用于限流计数）
- [ ] 10.4 实现 `hasKey(key)`
- [ ] 10.5 编写单元测试：Mock RedisTemplate 验证操作

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
- 任务 5 依赖任务 1
- 任务 6、7、8、9、10 均依赖任务 3、4
- 任务 7 依赖任务 6
- **本模块全部完成后，其他后端模块才可开始**
