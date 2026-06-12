# 校园论坛网站 — 详细设计文档

> 版本：v1.0 | 日期：2026-05-27
> 基于：[需求文档 v1.0](./proposal.md) · [概要设计 v1.0](./high-level-design.md)

---

## 目录

- [一、公共模块](#一公共模块)
- [二、数据库 DDL](#二数据库-ddl)
- [三、认证模块 (Auth)](#三认证模块-auth)
- [四、用户模块 (User)](#四用户模块-user)
- [五、板块模块 (Board)](#五板块模块-board)
- [六、帖子模块 (Post)](#六帖子模块-post)
- [七、评论模块 (Comment)](#七评论模块-comment)
- [八、互动模块 (Interaction)](#八互动模块-interaction)
- [九、私信模块 (Message)](#九私信模块-message)
- [十、搜索模块 (Search)](#十搜索模块-search)
- [十一、管理模块 (Admin)](#十一管理模块-admin)
- [十二、前端详细设计](#十二前端详细设计)

---

## 一、公共模块

### 1.1 统一响应体 `R<T>`

```java
package com.campus.common.response;

@Data
public class R<T> {
    private int code;       // 业务状态码：0=成功，其他=失败
    private String message; // 提示信息
    private T data;         // 响应数据

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> ok() { ... }
    public static <T> R<T> fail(int code, String message) { ... }
    public static <T> R<T> fail(ResultCode resultCode) { ... }
}
```

### 1.2 业务状态码枚举 `ResultCode`

```java
package com.campus.common.enums;

public enum ResultCode {
    SUCCESS(0, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    USER_DISABLED(410, "账号已被禁用"),
    VERIFY_CODE_EXPIRED(411, "验证码已过期"),
    VERIFY_CODE_WRONG(412, "验证码错误"),
    STUDENT_NO_EXISTS(413, "学号已注册"),
    EMAIL_EXISTS(414, "邮箱已注册"),
    LOGIN_FAIL(415, "学号或密码错误"),
    ACCOUNT_LOCKED(416, "账号已锁定，请15分钟后重试"),
    FILE_TOO_LARGE(420, "文件超过大小限制"),
    FILE_TYPE_NOT_ALLOWED(421, "不支持的文件格式"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
```

### 1.3 全局异常处理器

```java
package com.campus.common.exception;

// 自定义业务异常
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    // 构造函数接受 ResultCode 或 code + message
}

// 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        // 提取 @Valid 校验失败的第一条错误信息
        String msg = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<?> handleAccessDenied(AccessDeniedException e) {
        return R.fail(403, "无权限");
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("未知异常", e);
        return R.fail(500, "服务器内部错误");
    }
}
```

### 1.4 分页请求/响应封装

```java
// 分页请求参数
@Data
public class PageQuery {
    @Min(1)
    private int page = 1;      // 页码，从 1 开始
    @Min(1) @Max(50)
    private int size = 20;     // 每页条数，上限 50
}

// 分页响应
@Data
public class PageResult<T> {
    private List<T> records;   // 数据列表
    private long total;        // 总条数
    private int page;          // 当前页
    private int size;          // 每页条数
    private int pages;         // 总页数
}
```

### 1.5 工具类

| 类名 | 职责 |
|------|------|
| `JwtUtil` | JWT 生成/解析/校验（Access Token + Refresh Token） |
| `SecurityUtil` | 获取当前登录用户 ID（从 SecurityContext） |
| `FileUtil` | 文件上传（UUID 重命名、路径生成、格式校验） |
| `EmailUtil` | 发送验证码邮件 |
| `RedisUtil` | Redis 操作封装（验证码缓存、限流计数） |
| `SensitiveWordUtil` | 敏感词过滤（可选，基于 DFA 算法） |

### 1.6 全局配置

#### `application.yml` 主配置

```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 20MB
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  config: classpath:log4j2.xml
```

#### `application-dev.yml` 开发配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/campus_forum
    username: postgres
    password: ${DB_PASSWORD:123456}
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
  mail:
    host: smtp.xxx.edu.cn
    port: 465
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.ssl.enable: true

campus:
  jwt:
    access-secret: ${JWT_ACCESS_SECRET:campus-access-secret-key-at-least-32bytes}
    access-expiration: 7200000       # 2小时 (ms)
    refresh-secret: ${JWT_REFRESH_SECRET:campus-refresh-secret-key-at-least-32}
    refresh-expiration: 604800000    # 7天 (ms)
  upload:
    path: ./uploads/
    allowed-types: image/jpeg,image/png,image/gif,image/webp
    max-size: 5242880                # 5MB (bytes)
  email:
    expire: 300                      # 验证码过期时间 (秒)
```

#### `log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <RollingFile name="File" fileName="logs/campus-forum.log"
                     filePattern="logs/campus-forum-%d{yyyy-MM-dd}.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <SizeBasedTriggeringPolicy size="10MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>
    <Loggers>
        <Logger name="com.campus" level="DEBUG"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="File"/>
        </Root>
    </Loggers>
</Configuration>
```

### 1.7 Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/boards", "/api/posts", "/api/posts/{id}").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"message\":\"无权限\"}");
                })
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 1.8 JWT 过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request); // 从 Authorization: Bearer xxx 提取
        if (token != null && jwtUtil.validateAccessToken(token)) {
            Long userId = jwtUtil.getUserIdFromAccessToken(token);
            String role = jwtUtil.getRoleFromAccessToken(token);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

### 1.9 CORS 配置

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 1.10 MyBatis-Plus 配置

```java
@Configuration
@MapperScan("com.campus.*.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
```

---

## 二、数据库 DDL

使用 Flyway 管理，文件位于 `src/main/resources/db/migration/V1__init_schema.sql`。

```sql
-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE "user" (
    id            BIGSERIAL PRIMARY KEY,
    student_no    VARCHAR(20)  NOT NULL,          -- 学号
    nickname      VARCHAR(50)  NOT NULL,          -- 昵称
    password      VARCHAR(100) NOT NULL,          -- BCrypt 加密密码
    email         VARCHAR(100) NOT NULL,          -- 学校邮箱
    avatar        VARCHAR(255) DEFAULT NULL,      -- 头像路径
    bio           VARCHAR(200) DEFAULT '',        -- 个人简介
    role          VARCHAR(10)  NOT NULL DEFAULT 'STUDENT', -- STUDENT / ADMIN
    status        SMALLINT     NOT NULL DEFAULT 1,         -- 1=正常 0=禁用
    login_fail    SMALLINT     NOT NULL DEFAULT 0,         -- 连续登录失败次数
    locked_until  TIMESTAMP    DEFAULT NULL,               -- 锁定截止时间
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_student_no ON "user"(student_no) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_user_email ON "user"(email) WHERE deleted = 0;

-- ============================================================
-- 板块表
-- ============================================================
CREATE TABLE board (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,       -- 板块名称
    description VARCHAR(200) DEFAULT '',     -- 板块描述
    icon        VARCHAR(100) DEFAULT NULL,   -- 板块图标
    sort_order  INT          NOT NULL DEFAULT 0,  -- 排序权重，越小越靠前
    status      SMALLINT     NOT NULL DEFAULT 1,  -- 1=启用 0=禁用
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 帖子表
-- ============================================================
CREATE TABLE post (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(100) NOT NULL,          -- 标题
    content       TEXT         NOT NULL,          -- 富文本内容 (HTML)
    author_id     BIGINT       NOT NULL,          -- 作者 ID
    board_id      BIGINT       NOT NULL,          -- 板块 ID
    view_count    INT          NOT NULL DEFAULT 0, -- 浏览量
    like_count    INT          NOT NULL DEFAULT 0, -- 点赞数（冗余）
    comment_count INT          NOT NULL DEFAULT 0, -- 评论数（冗余）
    fav_count     INT          NOT NULL DEFAULT 0, -- 收藏数（冗余）
    hot_score     DOUBLE PRECISION NOT NULL DEFAULT 0, -- 热度分（预计算）
    is_pinned     BOOLEAN      NOT NULL DEFAULT FALSE, -- 是否置顶
    is_featured   BOOLEAN      NOT NULL DEFAULT FALSE, -- 是否精华
    status        SMALLINT     NOT NULL DEFAULT 1,     -- 1=正常 0=待审核 -1=已删除
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES "user"(id),
    CONSTRAINT fk_post_board  FOREIGN KEY (board_id)  REFERENCES board(id)
);

CREATE INDEX idx_post_board_id    ON post(board_id);
CREATE INDEX idx_post_author_id   ON post(author_id);
CREATE INDEX idx_post_created_at  ON post(created_at DESC);
CREATE INDEX idx_post_hot_score   ON post(hot_score DESC);
CREATE INDEX idx_post_board_pinned ON post(board_id, is_pinned DESC, hot_score DESC);

-- ============================================================
-- 评论表
-- ============================================================
CREATE TABLE comment (
    id          BIGSERIAL PRIMARY KEY,
    content     TEXT     NOT NULL,               -- 评论内容（纯文本）
    author_id   BIGINT   NOT NULL,
    post_id     BIGINT   NOT NULL,
    parent_id   BIGINT   DEFAULT NULL,           -- 父评论 ID（楼中楼），顶层为 NULL
    reply_to_user_id BIGINT DEFAULT NULL,        -- 回复的目标用户 ID（楼中楼场景）
    like_count  INT      NOT NULL DEFAULT 0,
    status      SMALLINT NOT NULL DEFAULT 1,     -- 1=正常 -1=已删除
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted     SMALLINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES "user"(id),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES post(id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment(id)
);

CREATE INDEX idx_comment_post_id   ON comment(post_id);
CREATE INDEX idx_comment_parent_id ON comment(parent_id);

-- ============================================================
-- 点赞表（多态：支持帖子和评论）
-- ============================================================
CREATE TABLE "like" (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(10) NOT NULL,            -- POST / COMMENT
    target_id   BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT uk_like_user_target UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_like_target ON "like"(target_type, target_id);

-- ============================================================
-- 收藏表
-- ============================================================
CREATE TABLE favorite (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    post_id    BIGINT   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT fk_fav_post FOREIGN KEY (post_id) REFERENCES post(id),
    CONSTRAINT uk_fav_user_post UNIQUE (user_id, post_id)
);

-- ============================================================
-- 私信表
-- ============================================================
CREATE TABLE message (
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT   NOT NULL,
    receiver_id BIGINT   NOT NULL,
    content     TEXT     NOT NULL,               -- 私信内容（纯文本）
    is_read     BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_by_sender   SMALLINT NOT NULL DEFAULT 0, -- 发送者删除标记
    deleted_by_receiver SMALLINT NOT NULL DEFAULT 0, -- 接收者删除标记

    CONSTRAINT fk_msg_sender   FOREIGN KEY (sender_id)   REFERENCES "user"(id),
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES "user"(id)
);

CREATE INDEX idx_msg_sender    ON message(sender_id, created_at DESC);
CREATE INDEX idx_msg_receiver  ON message(receiver_id, is_read, created_at DESC);

-- ============================================================
-- 验证码表（可选，也可用 Redis 缓存）
-- ============================================================
CREATE TABLE verify_code (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(100) NOT NULL,
    code       VARCHAR(6)   NOT NULL,
    type       VARCHAR(20)  NOT NULL,            -- REGISTER / RESET_PASSWORD
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_verify_email ON verify_code(email, type, used);
```

---

## 三、认证模块 (Auth)

### 3.1 模块结构

```
auth/
├── controller/AuthController.java
├── service/AuthService.java
├── dto/
│   ├── RegisterRequest.java        # 注册请求
│   ├── VerifyCodeRequest.java      # 验证码校验请求
│   ├── LoginRequest.java           # 登录请求
│   ├── LoginResponse.java          # 登录响应（含 AccessToken）
│   ├── RefreshTokenRequest.java    # 刷新 Token 请求
│   └── ResetPasswordRequest.java   # 重置密码请求
└── security/
    ├── JwtAuthenticationFilter.java
    └── JwtUtil.java
```

### 3.2 DTO 定义

```java
// RegisterRequest
@Data
public class RegisterRequest {
    @NotBlank @Size(min = 8, max = 20)
    @Pattern(regexp = "^\\d{8,12}$", message = "学号格式不正确")
    private String studentNo;

    @NotBlank @Email
    @Pattern(regexp = "^[\\w.-]+@xxx\\.edu\\.cn$", message = "必须使用学校邮箱")
    private String email;

    @NotBlank @Size(min = 6, max = 20)
    private String password;
}

// VerifyCodeRequest
@Data
public class VerifyCodeRequest {
    @NotBlank private String email;
    @NotBlank @Size(min = 6, max = 6)
    private String code;
}

// LoginRequest
@Data
public class LoginRequest {
    @NotBlank private String studentNo;
    @NotBlank private String password;
}

// LoginResponse
@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private UserInfoVO user;
}

// UserInfoVO（登录后返回的用户信息）
@Data
public class UserInfoVO {
    private Long id;
    private String studentNo;
    private String nickname;
    private String avatar;
    private String role;
}
```

### 3.3 接口签名

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    // 发送注册验证码
    // POST /api/auth/send-code
    // Body: { email: "xxx@xxx.edu.cn" }
    // Response: R<Void>
    @PostMapping("/send-code")
    public R<Void> sendVerifyCode(@RequestBody @Valid SendCodeRequest request);

    // 注册
    // POST /api/auth/register
    // Body: RegisterRequest
    // Response: R<Void>
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid RegisterRequest request);

    // 验证码校验并完成注册（合并到 register 中也可，此处分离）
    // POST /api/auth/register/verify
    // Body: VerifyCodeRequest
    // Response: R<LoginResponse>
    @PostMapping("/register/verify")
    public R<LoginResponse> verifyAndComplete(@RequestBody @Valid VerifyCodeRequest request);

    // 登录
    // POST /api/auth/login
    // Body: LoginRequest
    // Response: R<LoginResponse>
    // Set-Cookie: refreshToken (HttpOnly)
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                   HttpServletResponse response);

    // 刷新 Token
    // POST /api/auth/refresh
    // Cookie: refreshToken
    // Response: R<{ accessToken }>
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@CookieValue("refreshToken") String refreshToken,
                                           HttpServletResponse response);

    // 登出
    // POST /api/auth/logout
    // Response: R<Void>
    // 清除 Cookie
    @PostMapping("/logout")
    public R<Void> logout(HttpServletResponse response);

    // 忘记密码 - 发送验证码
    // POST /api/auth/forgot-password
    // Body: { email }
    // Response: R<Void>
    @PostMapping("/forgot-password")
    public R<Void> forgotPassword(@RequestBody @Valid SendCodeRequest request);

    // 重置密码
    // POST /api/auth/reset-password
    // Body: ResetPasswordRequest { email, code, newPassword }
    // Response: R<Void>
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request);
}
```

### 3.4 AuthService 核心逻辑

```java
@Service
public class AuthService {

    // --- 发送验证码 ---
    public void sendVerifyCode(String email, String type) {
        // 1. 检查 60 秒内是否已发送（Redis key: verify:limit:{email}）
        // 2. 生成 6 位随机数字验证码
        // 3. 存入 Redis（key: verify:code:{email}:{type}，TTL 300 秒）
        // 4. 异步发送邮件（@Async）
    }

    // --- 注册 ---
    public void register(RegisterRequest request) {
        // 1. 检查学号是否已注册
        // 2. 检查邮箱是否已注册
        // 3. 暂存注册信息到 Redis（key: register:pending:{email}，TTL 300 秒）
    }

    public LoginResponse verifyAndComplete(VerifyCodeRequest request) {
        // 1. 从 Redis 获取验证码比对
        // 2. 从 Redis 获取暂存的注册信息
        // 3. 密码 BCrypt 加密
        // 4. 插入 user 表
        // 5. 生成 Access Token + Refresh Token
        // 6. 返回 LoginResponse
    }

    // --- 登录 ---
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        // 1. 根据 studentNo 查询用户
        // 2. 检查账号状态（是否禁用）
        // 3. 检查是否锁定（locked_until > NOW）
        // 4. BCrypt 校验密码
        //    - 失败：login_fail++，连续 5 次锁定 15 分钟
        //    - 成功：login_fail 归 0
        // 5. 生成 Access Token + Refresh Token
        // 6. Refresh Token 写入 HttpOnly Cookie
        // 7. 返回 LoginResponse
    }

    // --- 刷新 Token ---
    public Map<String, String> refreshToken(String refreshToken, HttpServletResponse response) {
        // 1. 校验 Refresh Token 签名和有效期
        // 2. 提取 userId，查询用户是否存在且未禁用
        // 3. 生成新的 Access Token
        // 4. （可选）Rotation：生成新 Refresh Token 替换旧的
        // 5. 返回新 Access Token
    }
}
```

### 3.5 JwtUtil 关键方法

```java
@Component
public class JwtUtil {

    // 生成 Access Token
    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
            .signWith(Keys.hmacShaKeyFor(accessSecret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }

    // 生成 Refresh Token
    public String generateRefreshToken(Long userId) { ... }

    // 解析 Access Token
    public Claims parseAccessToken(String token) { ... }

    // 校验 Token 有效性
    public boolean validateAccessToken(String token) {
        try { parseAccessToken(token); return true; }
        catch (JwtException e) { return false; }
    }

    // 提取用户 ID
    public Long getUserIdFromAccessToken(String token) {
        return Long.valueOf(parseAccessToken(token).getSubject());
    }
}
```

### 3.6 错误处理

| 场景 | 状态码 | 错误信息 |
|------|--------|----------|
| 学号已注册 | 413 | "学号已注册" |
| 邮箱已注册 | 414 | "邮箱已注册" |
| 验证码过期 | 411 | "验证码已过期，请重新获取" |
| 验证码错误 | 412 | "验证码错误" |
| 学号或密码错误 | 415 | "学号或密码错误" |
| 账号已禁用 | 410 | "账号已被禁用" |
| 账号已锁定 | 416 | "账号已锁定，请X分钟后重试" |
| Refresh Token 无效 | 401 | "Token 已过期，请重新登录" |

---

## 四、用户模块 (User)

### 4.1 模块结构

```
user/
├── controller/UserController.java
├── service/UserService.java
├── mapper/UserMapper.java
├── entity/User.java
└── dto/
    ├── UpdateProfileRequest.java
    ├── UserProfileVO.java
    └── UserAdminVO.java
```

### 4.2 实体类

```java
@Data
@TableName("\"user\"")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String nickname;
    private String password;
    private String email;
    private String avatar;
    private String bio;
    private String role;        // STUDENT / ADMIN
    private Integer status;     // 1=正常 0=禁用
    private Integer loginFail;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

### 4.3 接口签名

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 获取当前登录用户信息
    // GET /api/users/me
    // Response: R<UserProfileVO>
    @GetMapping("/me")
    public R<UserProfileVO> getCurrentUser();

    // 更新个人信息
    // PUT /api/users/me
    // Body: UpdateProfileRequest { nickname?, bio? }
    // Response: R<Void>
    @PutMapping("/me")
    public R<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest request);

    // 上传头像
    // POST /api/users/me/avatar
    // Body: MultipartFile (image)
    // Response: R<{ avatarUrl }>
    @PostMapping("/me/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file);

    // 获取指定用户公开信息
    // GET /api/users/{id}
    // Response: R<UserProfileVO>
    @GetMapping("/{id}")
    public R<UserProfileVO> getUserById(@PathVariable Long id);
}
```

### 4.4 DTO 定义

```java
// UpdateProfileRequest
@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 20)
    private String nickname;

    @Size(max = 100)
    private String bio;
}

// UserProfileVO
@Data
public class UserProfileVO {
    private Long id;
    private String studentNo;   // 脱敏：2021****0001
    private String nickname;
    private String avatar;
    private String bio;
    private Integer postCount;  // 发帖数
    private Integer likeCount;  // 获赞数
    private LocalDateTime createdAt;
}
```

### 4.5 UserService 核心逻辑

```java
@Service
public class UserService {

    // 获取当前用户
    public UserProfileVO getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        // 转 VO，学号脱敏
        // 查询发帖数、获赞数
    }

    // 更新个人信息
    public void updateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = new User();
        user.setId(userId);
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getBio() != null) user.setBio(request.getBio());
        userMapper.updateById(user);
    }

    // 上传头像
    public String uploadAvatar(MultipartFile file) {
        // 1. 校验文件类型和大小
        String path = FileUtil.upload(file, "avatar/");
        // 2. 更新用户头像路径
        // 3. 返回可访问的 URL
        return path;
    }
}
```

### 4.6 文件上传工具

```java
@Component
public class FileUtil {

    @Value("${campus.upload.path}")
    private String uploadPath;

    @Value("${campus.upload.allowed-types}")
    private String allowedTypes;

    @Value("${campus.upload.max-size}")
    private long maxSize;

    public String upload(MultipartFile file, String subDir) {
        // 1. 校验文件不为空
        if (file.isEmpty()) throw new BusinessException(400, "文件不能为空");

        // 2. 校验文件类型
        String contentType = file.getContentType();
        if (!Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new BusinessException(421, "不支持的文件格式");
        }

        // 3. 校验文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException(420, "文件超过5MB限制");
        }

        // 4. 生成 UUID 文件名
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + ext;

        // 5. 按日期生成子目录：uploads/avatar/2026/05/27/
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fullPath = uploadPath + subDir + datePath + "/" + fileName;

        // 6. 保存文件
        file.transferTo(new File(fullPath));

        // 7. 返回相对路径（前端拼接 base URL）
        return "/uploads/" + subDir + datePath + "/" + fileName;
    }
}
```

---

## 五、板块模块 (Board)

### 5.1 模块结构

```
board/
├── controller/BoardController.java
├── service/BoardService.java
├── mapper/BoardMapper.java
├── entity/Board.java
└── dto/
    ├── BoardVO.java
    ├── BoardCreateRequest.java
    └── BoardUpdateRequest.java
```

### 5.2 实体类

```java
@Data
@TableName("board")
public class Board {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer status;     // 1=启用 0=禁用
    private LocalDateTime createdAt;
}
```

### 5.3 接口签名

```java
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    // 获取板块列表（公开接口）
    // GET /api/boards
    // Response: R<List<BoardVO>>
    @GetMapping
    public R<List<BoardVO>> listBoards();

    // --- 以下为管理员接口 ---

    // 创建板块
    // POST /api/admin/boards
    // Body: BoardCreateRequest { name, description?, icon?, sortOrder? }
    // Response: R<BoardVO>
    @PostMapping("/admin/boards")
    @PreAuthorize("hasRole('ADMIN')")
    public R<BoardVO> createBoard(@RequestBody @Valid BoardCreateRequest request);

    // 更新板块
    // PUT /api/admin/boards/{id}
    // Body: BoardUpdateRequest
    // Response: R<Void>
    @PutMapping("/admin/boards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateBoard(@PathVariable Long id,
                                @RequestBody @Valid BoardUpdateRequest request);

    // 删除板块（软删除，需检查是否有帖子）
    // DELETE /api/admin/boards/{id}
    // Response: R<Void>
    @DeleteMapping("/admin/boards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteBoard(@PathVariable Long id);
}
```

### 5.4 DTO 定义

```java
@Data
public class BoardVO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer postCount;  // 板块内帖子数
}

@Data
public class BoardCreateRequest {
    @NotBlank @Size(max = 50)
    private String name;
    @Size(max = 200)
    private String description;
    private String icon;
    private Integer sortOrder;
}

@Data
public class BoardUpdateRequest {
    @Size(max = 50)
    private String name;
    @Size(max = 200)
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer status;
}
```

---

## 六、帖子模块 (Post)

### 6.1 模块结构

```
post/
├── controller/PostController.java
├── service/PostService.java
├── mapper/PostMapper.java        # 含复杂 XML 查询
├── entity/Post.java
├── dto/
│   ├── PostCreateRequest.java
│   ├── PostUpdateRequest.java
│   ├── PostVO.java
│   ├── PostListVO.java
│   └── PostQuery.java
└── scheduled/HotScoreScheduler.java  # 热度分定时计算
```

### 6.2 实体类

```java
@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Long boardId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favCount;
    private Double hotScore;
    private Boolean isPinned;
    private Boolean isFeatured;
    private Integer status;     // 1=正常 0=待审核 -1=已删除
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

### 6.3 接口签名

```java
@RestController
@RequestMapping("/api/posts")
public class PostController {

    // 获取帖子列表（公开接口）
    // GET /api/posts?boardId=1&sort=latest&page=1&size=20
    // sort: latest(最新) / hot(热门) / featured(精华)
    // Response: R<PageResult<PostListVO>>
    @GetMapping
    public R<PageResult<PostListVO>> listPosts(@Valid PostQuery query);

    // 获取帖子详情
    // GET /api/posts/{id}
    // Response: R<PostVO>
    @GetMapping("/{id}")
    public R<PostVO> getPostDetail(@PathVariable Long id);

    // 发布帖子（需登录）
    // POST /api/posts
    // Body: PostCreateRequest (JSON，内容为富文本 HTML)
    // Response: R<PostVO>
    @PostMapping
    public R<PostVO> createPost(@RequestBody @Valid PostCreateRequest request);

    // 编辑帖子（仅作者）
    // PUT /api/posts/{id}
    // Body: PostUpdateRequest
    // Response: R<Void>
    @PutMapping("/{id}")
    public R<Void> updatePost(@PathVariable Long id,
                               @RequestBody @Valid PostUpdateRequest request);

    // 删除帖子（仅作者或管理员）
    // DELETE /api/posts/{id}
    // Response: R<Void>
    @DeleteMapping("/{id}")
    public R<Void> deletePost(@PathVariable Long id);

    // 上传帖子图片（富文本编辑器内使用）
    // POST /api/posts/upload-image
    // Body: MultipartFile
    // Response: R<{ url }>  （wangEditor 要求返回 { url } 格式）
    @PostMapping("/upload-image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file);
}
```

### 6.4 DTO 定义

```java
@Data
public class PostQuery extends PageQuery {
    private Long boardId;       // 板块筛选，可选
    private String sort = "latest"; // latest / hot / featured
    private String keyword;     // 搜索关键词（搜索模块用）
}

@Data
public class PostCreateRequest {
    @NotBlank @Size(max = 100)
    private String title;

    @NotBlank
    private String content;     // 富文本 HTML

    @NotNull
    private Long boardId;
}

@Data
public class PostUpdateRequest {
    @Size(max = 100)
    private String title;
    private String content;
}

// PostListVO（列表页展示）
@Data
public class PostListVO {
    private Long id;
    private String title;
    private String summary;         // 内容摘要（前 100 字纯文本）
    private String authorNickname;
    private String authorAvatar;
    private Long boardId;
    private String boardName;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
}

// PostVO（详情页展示，含当前用户的互动状态）
@Data
public class PostVO {
    private Long id;
    private String title;
    private String content;         // 完整 HTML
    private AuthorVO author;
    private BoardVO board;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favCount;
    private Boolean isPinned;
    private Boolean isFeatured;
    private Boolean isLiked;        // 当前用户是否已点赞
    private Boolean isFavorited;    // 当前用户是否已收藏
    private Boolean isOwner;        // 当前用户是否为作者
    private LocalDateTime createdAt;
}

@Data
public class AuthorVO {
    private Long id;
    private String nickname;
    private String avatar;
}
```

### 6.5 PostService 核心逻辑

```java
@Service
public class PostService {

    // 获取帖子列表
    public PageResult<PostListVO> listPosts(PostQuery query) {
        // 1. 构建查询条件（boardId 筛选、status=1、deleted=0）
        // 2. 排序：
        //    - latest: ORDER BY created_at DESC
        //    - hot:    ORDER BY is_pinned DESC, hot_score DESC
        //    - featured: WHERE is_featured = true ORDER BY created_at DESC
        // 3. MyBatis-Plus 分页查询
        // 4. 批量查询作者信息（避免 N+1）
        // 5. 生成内容摘要（去除 HTML 标签，取前 100 字）
    }

    // 获取帖子详情
    public PostVO getPostDetail(Long postId) {
        // 1. 查询帖子
        // 2. 浏览量 +1（UPDATE post SET view_count = view_count + 1）
        // 3. 查询作者信息
        // 4. 查询板块信息
        // 5. 查询当前用户是否点赞/收藏（SecurityUtil.getCurrentUserId()，未登录跳过）
        // 6. 组装 PostVO 返回
    }

    // 发布帖子
    public PostVO createPost(PostCreateRequest request) {
        // 1. 校验板块是否存在且启用
        // 2. 富文本内容 Jsoup 过滤 XSS
        // 3. 构建 Post 实体，插入数据库
        // 4. 初始热度分 = 0
        // 5. 返回帖子详情
    }

    // 编辑帖子
    public void updatePost(Long postId, PostUpdateRequest request) {
        // 1. 查询帖子是否存在
        // 2. 校验当前用户是否为作者
        // 3. 更新字段
    }

    // 删除帖子
    public void deletePost(Long postId) {
        // 1. 查询帖子
        // 2. 校验当前用户是否为作者或管理员
        // 3. 逻辑删除
    }
}
```

### 6.6 热度分定时计算 `HotScoreScheduler`

```java
@Component
@Slf4j
public class HotScoreScheduler {

    @Autowired private PostMapper postMapper;

    // 每 10 分钟执行一次，重新计算所有近 7 天帖子的热度分
    @Scheduled(fixedRate = 600000)
    public void recalculateHotScore() {
        log.info("开始计算帖子热度分...");
        // SQL:
        // UPDATE post SET hot_score =
        //   (like_count * 3.0 + comment_count * 2.0 + fav_count * 1.0)
        //   / POWER(EXTRACT(EPOCH FROM (NOW() - created_at)) / 3600 + 2, 1.5)
        // WHERE created_at > NOW() - INTERVAL '7 days'
        //   AND deleted = 0 AND status = 1
        int updated = postMapper.recalculateHotScore();
        log.info("热度分计算完成，更新 {} 条帖子", updated);
    }
}
```

MyBatis XML（`mapper/PostMapper.xml`）：

```xml
<update id="recalculateHotScore">
    UPDATE post SET hot_score =
        (like_count * 3.0 + comment_count * 2.0 + fav_count * 1.0)
        / POWER(EXTRACT(EPOCH FROM (NOW() - created_at)) / 3600 + 2, 1.5)
    WHERE created_at > NOW() - INTERVAL '7 days'
      AND deleted = 0 AND status = 1
</update>
```

### 6.7 XSS 过滤

```java
// 在 PostService.createPost() 中使用
private String sanitizeHtml(String html) {
    return Jsoup.clean(html, Safelist.relaxed()
        .addTags("img")
        .addAttributes("img", "src", "alt", "width", "height")
        .addProtocols("img", "src", "http", "https"));
}
```

---

## 七、评论模块 (Comment)

### 7.1 模块结构

```
comment/
├── controller/CommentController.java
├── service/CommentService.java
├── mapper/CommentMapper.java
├── entity/Comment.java
└── dto/
    ├── CommentCreateRequest.java
    ├── CommentVO.java
    └── CommentQuery.java
```

### 7.2 实体类

```java
@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long authorId;
    private Long postId;
    private Long parentId;          // NULL = 顶层评论
    private Long replyToUserId;     // 回复目标用户
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
```

### 7.3 接口签名

```java
@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    // 获取评论列表（树形结构）
    // GET /api/posts/{postId}/comments?page=1&size=20
    // Response: R<PageResult<CommentVO>>
    @GetMapping
    public R<PageResult<CommentVO>> listComments(@PathVariable Long postId,
                                                  @Valid PageQuery query);

    // 发表评论
    // POST /api/posts/{postId}/comments
    // Body: CommentCreateRequest { content, parentId?, replyToUserId? }
    // Response: R<CommentVO>
    @PostMapping
    public R<CommentVO> createComment(@PathVariable Long postId,
                                       @RequestBody @Valid CommentCreateRequest request);

    // 删除评论（仅作者或管理员）
    // DELETE /api/posts/{postId}/comments/{id}
    // Response: R<Void>
    @DeleteMapping("/{id}")
    public R<Void> deleteComment(@PathVariable Long postId, @PathVariable Long id);
}
```

### 7.4 DTO 定义

```java
@Data
public class CommentCreateRequest {
    @NotBlank @Size(max = 500)
    private String content;

    private Long parentId;          // 父评论 ID（楼中楼）
    private Long replyToUserId;     // 回复目标用户 ID
}

// CommentVO（树形结构）
@Data
public class CommentVO {
    private Long id;
    private String content;
    private AuthorVO author;
    private Long parentId;
    private AuthorVO replyToUser;   // 回复目标用户（楼中楼场景）
    private Integer likeCount;
    private Boolean isLiked;        // 当前用户是否已点赞
    private Boolean isOwner;
    private LocalDateTime createdAt;
    private List<CommentVO> replies; // 子回复列表（一级）
}
```

### 7.5 CommentService 核心逻辑

```java
@Service
public class CommentService {

    // 获取评论列表（树形）
    public PageResult<CommentVO> listComments(Long postId, PageQuery query) {
        // 1. 分页查询顶层评论（parent_id IS NULL），按时间正序
        // 2. 批量查询顶层评论的一级回复（parent_id IN 顶层ID列表）
        // 3. 查询作者信息、回复目标用户信息
        // 4. 查询当前用户的点赞状态
        // 5. 组装树形结构：顶层评论.replies = [子回复列表]
    }

    // 发表评论
    public CommentVO createComment(Long postId, CommentCreateRequest request) {
        // 1. 校验帖子存在且未删除
        // 2. 如果是楼中楼回复，校验父评论存在且属于该帖子
        // 3. 内容 XSS 过滤（纯文本，escape HTML）
        // 4. 插入评论
        // 5. 帖子评论数 +1（UPDATE post SET comment_count = comment_count + 1）
        // 6. 返回 CommentVO
    }

    // 删除评论
    public void deleteComment(Long postId, Long commentId) {
        // 1. 校验评论存在
        // 2. 校验权限（作者或管理员）
        // 3. 逻辑删除
        // 4. 帖子评论数 -1
    }
}
```

---

## 八、互动模块 (Interaction)

### 8.1 模块结构

```
interaction/
├── controller/InteractionController.java
├── service/LikeService.java
├── service/FavoriteService.java
├── mapper/LikeMapper.java
├── mapper/FavoriteMapper.java
├── entity/Like.java
├── entity/Favorite.java
└── dto/ToggleResponse.java
```

### 8.2 实体类

```java
@Data
@TableName("\"like\"")
public class Like {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;  // POST / COMMENT
    private Long targetId;
    private LocalDateTime createdAt;
}

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
    private LocalDateTime createdAt;
}
```

### 8.3 接口签名

```java
@RestController
@RequestMapping("/api")
public class InteractionController {

    // 点赞/取消点赞（幂等）
    // POST /api/likes
    // Body: { targetType: "POST", targetId: 123 }
    // Response: R<ToggleResponse>  { liked: true/false, count: 10 }
    @PostMapping("/likes")
    public R<ToggleResponse> toggleLike(@RequestBody @Valid LikeRequest request);

    // 收藏/取消收藏（幂等）
    // POST /api/favorites
    // Body: { postId: 123 }
    // Response: R<ToggleResponse>  { favorited: true/false, count: 5 }
    @PostMapping("/favorites")
    public R<ToggleResponse> toggleFavorite(@RequestBody @Valid FavoriteRequest request);

    // 获取我的收藏列表
    // GET /api/favorites?page=1&size=20
    // Response: R<PageResult<PostListVO>>
    @GetMapping("/favorites")
    public R<PageResult<PostListVO>> myFavorites(@Valid PageQuery query);
}
```

### 8.4 DTO 定义

```java
@Data
public class LikeRequest {
    @NotBlank @Pattern(regexp = "^(POST|COMMENT)$")
    private String targetType;
    @NotNull
    private Long targetId;
}

@Data
public class FavoriteRequest {
    @NotNull
    private Long postId;
}

@Data
@AllArgsConstructor
public class ToggleResponse {
    private Boolean active;  // true=已点赞/收藏，false=已取消
    private Integer count;   // 最新总数
}
```

### 8.5 LikeService 核心逻辑

```java
@Service
public class LikeService {

    @Transactional
    public ToggleResponse toggleLike(Long userId, String targetType, Long targetId) {
        // 1. 查询是否已点赞
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<Like>()
            .eq(Like::getUserId, userId)
            .eq(Like::getTargetType, targetType)
            .eq(Like::getTargetId, targetId);

        Like existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消点赞
            likeMapper.deleteById(existing.getId());
            // 目标计数 -1
            updateTargetCount(targetType, targetId, -1);
            int count = getTargetCount(targetType, targetId);
            return new ToggleResponse(false, count);
        } else {
            // 点赞
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            likeMapper.insert(like);
            // 目标计数 +1
            updateTargetCount(targetType, targetId, 1);
            int count = getTargetCount(targetType, targetId);
            return new ToggleResponse(true, count);
        }
    }

    private void updateTargetCount(String targetType, Long targetId, int delta) {
        if ("POST".equals(targetType)) {
            // UPDATE post SET like_count = like_count + delta WHERE id = targetId
            postMapper.updateLikeCount(targetId, delta);
        } else if ("COMMENT".equals(targetType)) {
            commentMapper.updateLikeCount(targetId, delta);
        }
    }
}
```

---

## 九、私信模块 (Message)

### 9.1 模块结构

```
message/
├── controller/MessageController.java        # REST API
├── controller/WebSocketController.java      # WebSocket 端点
├── service/MessageService.java
├── mapper/MessageMapper.java
├── entity/Message.java
├── dto/
│   ├── MessageSendRequest.java
│   ├── MessageVO.java
│   ├── ConversationVO.java
│   └── WsMessage.java
└── config/WebSocketConfig.java
```

### 9.2 实体类

```java
@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Integer deletedBySender;
    private Integer deletedByReceiver;
}
```

### 9.3 WebSocket 配置

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");  // 消息代理前缀
        config.setApplicationDestinationPrefixes("/app"); // 客户端发送前缀
        config.setUserDestinationPrefix("/user");         // 用户目标前缀
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS 降级支持
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // WebSocket 连接时的认证拦截器
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                    .getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从请求头提取 JWT Token 并验证
                    String token = accessor.getFirstNativeHeader("Authorization");
                    // 验证通过后设置 Principal
                }
                return message;
            }
        });
    }
}
```

### 9.4 接口签名

#### REST API

```java
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    // 获取会话列表（最近联系人）
    // GET /api/messages/conversations
    // Response: R<List<ConversationVO>>
    @GetMapping("/conversations")
    public R<List<ConversationVO>> listConversations();

    // 获取与某用户的聊天记录
    // GET /api/messages/conversations/{userId}?page=1&size=20
    // Response: R<PageResult<MessageVO>>
    @GetMapping("/conversations/{userId}")
    public R<PageResult<MessageVO>> getChatHistory(@PathVariable Long userId,
                                                    @Valid PageQuery query);

    // 获取未读消息总数
    // GET /api/messages/unread-count
    // Response: R<{ count }>
    @GetMapping("/unread-count")
    public R<Map<String, Integer>> getUnreadCount();

    // 标记某会话已读
    // PUT /api/messages/conversations/{userId}/read
    // Response: R<Void>
    @PutMapping("/conversations/{userId}/read")
    public R<Void> markAsRead(@PathVariable Long userId);
}
```

#### WebSocket 端点

```java
@Controller
public class WebSocketController {

    @Autowired private MessageService messageService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // 客户端发送私信
    // SEND /app/message/send
    // Header: Authorization: Bearer <token>
    // Body: WsMessage { receiverId, content }
    @MessageMapping("/message/send")
    public void handleMessage(WsMessage wsMessage, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        // 1. 校验接收者存在
        // 2. 内容校验（长度、XSS）
        // 3. 持久化到数据库
        MessageVO saved = messageService.saveMessage(senderId, wsMessage);

        // 4. 推送给接收者（/user/{receiverId}/queue/messages）
        messagingTemplate.convertAndSendToUser(
            String.valueOf(wsMessage.getReceiverId()),
            "/queue/messages",
            saved
        );

        // 5. 回显给发送者（确认发送成功）
        messagingTemplate.convertAndSendToUser(
            String.valueOf(senderId),
            "/queue/messages",
            saved
        );
    }
}
```

### 9.5 DTO 定义

```java
// WebSocket 消息
@Data
public class WsMessage {
    @NotNull private Long receiverId;
    @NotBlank @Size(max = 1000) private String content;
}

// 消息 VO
@Data
public class MessageVO {
    private Long id;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;
    private String receiverNickname;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

// 会话 VO
@Data
public class ConversationVO {
    private Long userId;            // 对方用户 ID
    private String nickname;
    private String avatar;
    private String lastMessage;     // 最后一条消息摘要
    private LocalDateTime lastTime; // 最后消息时间
    private Integer unreadCount;    // 未读消息数
}
```

### 9.6 MessageService 核心逻辑

```java
@Service
public class MessageService {

    // 保存消息（WebSocket 调用）
    public MessageVO saveMessage(Long senderId, WsMessage wsMessage) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(wsMessage.getReceiverId());
        msg.setContent(wsMessage.getContent());
        msg.setIsRead(false);
        messageMapper.insert(msg);
        return convertToVO(msg);
    }

    // 获取会话列表
    public List<ConversationVO> listConversations(Long userId) {
        // SQL: 按最后消息时间分组，获取每个联系人的最后一条消息和未读数
        // SELECT DISTINCT ON (other_user_id)
        //   other_user_id, content, created_at, unread_count
        // FROM (
        //   SELECT
        //     CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END as other_user_id,
        //     content, created_at,
        //     CASE WHEN receiver_id = #{userId} AND is_read = false THEN 1 ELSE 0 END as is_unread
        //   FROM message
        //   WHERE (sender_id = #{userId} AND deleted_by_sender = 0)
        //      OR (receiver_id = #{userId} AND deleted_by_receiver = 0)
        // ) t
        // ORDER BY other_user_id, created_at DESC
        return messageMapper.selectConversations(userId);
    }

    // 获取聊天记录
    public PageResult<MessageVO> getChatHistory(Long userId, Long otherUserId, PageQuery query) {
        // 查询双方之间的消息，按时间倒序分页
    }

    // 标记已读
    public void markAsRead(Long userId, Long otherUserId) {
        // UPDATE message SET is_read = true
        // WHERE sender_id = #{otherUserId} AND receiver_id = #{userId} AND is_read = false
        messageMapper.markAsRead(userId, otherUserId);
    }

    // 未读消息数
    public int getUnreadCount(Long userId) {
        return messageMapper.selectUnreadCount(userId);
    }
}
```

### 9.7 前端 WebSocket 连接管理

```javascript
// src/utils/websocket.js
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

let stompClient = null

export function connectWebSocket(token, onMessage) {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      // 订阅个人消息队列
      stompClient.subscribe('/user/queue/messages', (message) => {
        const body = JSON.parse(message.body)
        onMessage(body)
      })
    },
    onStompError: (frame) => {
      console.error('WebSocket 错误:', frame.headers['message'])
    }
  })
  stompClient.activate()
}

export function sendMessage(receiverId, content) {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: '/app/message/send',
      body: JSON.stringify({ receiverId, content })
    })
  }
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}
```

---

## 十、搜索模块 (Search)

### 10.1 模块结构

```
search/
├── controller/SearchController.java
├── service/SearchService.java
└── dto/SearchQuery.java
```

### 10.2 接口签名

```java
@RestController
@RequestMapping("/api/search")
public class SearchController {

    // 搜索帖子
    // GET /api/search?keyword=xxx&boardId=1&page=1&size=20
    // Response: R<PageResult<PostListVO>>
    @GetMapping
    public R<PageResult<PostListVO>> search(@Valid SearchQuery query);
}
```

### 10.3 SearchService 核心逻辑

```java
@Service
public class SearchService {

    public PageResult<PostListVO> search(SearchQuery query) {
        // 使用 PostgreSQL 全文检索 或 LIKE 查询
        // LIKE 方案（初期，简单可靠）：
        // SELECT * FROM post
        // WHERE deleted = 0 AND status = 1
        //   AND (title ILIKE '%keyword%' OR content ILIKE '%keyword%')
        //   AND board_id = #{boardId}  -- 可选
        // ORDER BY created_at DESC
        // LIMIT #{size} OFFSET #{offset}

        // 内容搜索时去除 HTML 标签后匹配
    }
}
```

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchQuery extends PageQuery {
    @NotBlank
    private String keyword;
    private Long boardId;  // 板块内搜索，可选
}
```

### 10.4 MyBatis XML 查询

```xml
<!-- mapper/PostMapper.xml -->
<select id="searchPosts" resultType="PostListVO">
    SELECT p.id, p.title,
           regexp_replace(p.content, '<[^>]*>', '', 'g') AS content_text,
           p.view_count, p.like_count, p.comment_count,
           p.is_pinned, p.is_featured, p.created_at,
           u.nickname AS author_nickname, u.avatar AS author_avatar,
           b.id AS board_id, b.name AS board_name
    FROM post p
    JOIN "user" u ON p.author_id = u.id
    JOIN board b ON p.board_id = b.id
    WHERE p.deleted = 0 AND p.status = 1
      <if test="keyword != null and keyword != ''">
        AND (p.title ILIKE CONCAT('%', #{keyword}, '%')
          OR regexp_replace(p.content, '&lt;[^>]*&gt;', '', 'g') ILIKE CONCAT('%', #{keyword}, '%'))
      </if>
      <if test="boardId != null">
        AND p.board_id = #{boardId}
      </if>
    ORDER BY p.created_at DESC
    LIMIT #{size} OFFSET #{offset}
</select>
```

---

## 十一、管理模块 (Admin)

### 11.1 模块结构

```
admin/
├── controller/AdminController.java
├── service/AdminService.java
└── dto/
    ├── UserAdminVO.java
    ├── DashboardVO.java
    └── AuditRequest.java
```

### 11.2 接口签名

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    // 用户管理 - 列表
    // GET /api/admin/users?keyword=&status=&page=1&size=20
    // Response: R<PageResult<UserAdminVO>>
    @GetMapping("/users")
    public R<PageResult<UserAdminVO>> listUsers(AdminUserQuery query);

    // 禁用/启用用户
    // PUT /api/admin/users/{id}/status
    // Body: { status: 0 }  // 0=禁用 1=启用
    // Response: R<Void>
    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id,
                                     @RequestBody @Valid StatusRequest request);

    // 内容审核 - 待审核帖子列表
    // GET /api/admin/posts/pending?page=1&size=20
    // Response: R<PageResult<PostListVO>>
    @GetMapping("/posts/pending")
    public R<PageResult<PostListVO>> listPendingPosts(@Valid PageQuery query);

    // 审核帖子（通过/拒绝）
    // PUT /api/admin/posts/{id}/audit
    // Body: AuditRequest { approved: true/false }
    // Response: R<Void>
    @PutMapping("/posts/{id}/audit")
    public R<Void> auditPost(@PathVariable Long id,
                              @RequestBody @Valid AuditRequest request);

    // 置顶/取消置顶
    // PUT /api/admin/posts/{id}/pin
    // Response: R<Void>
    @PutMapping("/posts/{id}/pin")
    public R<Void> togglePin(@PathVariable Long id);

    // 精华/取消精华
    // PUT /api/admin/posts/{id}/feature
    // Response: R<Void>
    @PutMapping("/posts/{id}/feature")
    public R<Void> toggleFeature(@PathVariable Long id);

    // 删除帖子（管理员强制删除）
    // DELETE /api/admin/posts/{id}
    // Response: R<Void>
    @DeleteMapping("/posts/{id}")
    public R<Void> deletePost(@PathVariable Long id);

    // 数据统计
    // GET /api/admin/dashboard
    // Response: R<DashboardVO>
    @GetMapping("/dashboard")
    public R<DashboardVO> getDashboard();
}
```

### 11.3 DTO 定义

```java
@Data
public class UserAdminVO {
    private Long id;
    private String studentNo;
    private String nickname;
    private String email;
    private String role;
    private Integer status;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime lockedUntil;
}

@Data
public class DashboardVO {
    private Long totalUsers;       // 总用户数
    private Long totalPosts;       // 总帖子数
    private Long totalComments;    // 总评论数
    private Long todayNewUsers;    // 今日新增用户
    private Long todayNewPosts;    // 今日新增帖子
    private Long activeUsers;      // 7日活跃用户数
    private List<BoardStat> boardStats; // 各板块帖子数统计
}

@Data
@AllArgsConstructor
public class BoardStat {
    private String boardName;
    private Long postCount;
}
```

### 11.4 AdminService 核心逻辑

```java
@Service
public class AdminService {

    // 数据统计
    public DashboardVO getDashboard() {
        // 1. 总用户数：SELECT COUNT(*) FROM "user" WHERE deleted = 0
        // 2. 总帖子数/评论数：类似
        // 3. 今日新增：WHERE created_at >= CURRENT_DATE
        // 4. 7日活跃：WHERE created_at >= NOW() - INTERVAL '7 days'
        // 5. 各板块统计：GROUP BY board_id
    }

    // 禁用/启用用户
    public void updateUserStatus(Long userId, Integer status) {
        // 1. 不能操作自己
        // 2. 不能操作其他管理员
        // 3. 更新 status 字段
    }

    // 审核帖子
    public void auditPost(Long postId, boolean approved) {
        if (approved) {
            // UPDATE post SET status = 1 WHERE id = postId
        } else {
            // UPDATE post SET status = -1 WHERE id = postId (逻辑删除)
        }
    }
}
```

---

## 十二、前端详细设计

### 12.1 路由配置

```javascript
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/home/Home.vue') },
      { path: 'board/:id', name: 'Board', component: () => import('@/views/post/PostList.vue') },
      { path: 'post/:id', name: 'PostDetail', component: () => import('@/views/post/PostDetail.vue') },
      { path: 'post/create', name: 'PostCreate', component: () => import('@/views/post/PostCreate.vue'), meta: { requiresAuth: true } },
      { path: 'post/:id/edit', name: 'PostEdit', component: () => import('@/views/post/PostCreate.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/profile/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'profile/:id', name: 'UserProfile', component: () => import('@/views/profile/Profile.vue') },
      { path: 'messages', name: 'Messages', component: () => import('@/views/message/Message.vue'), meta: { requiresAuth: true } },
      { path: 'search', name: 'Search', component: () => import('@/views/search/SearchResult.vue') },
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/auth/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/Register.vue') },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'content', name: 'ContentAudit', component: () => import('@/views/admin/ContentAudit.vue') },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && userStore.user?.role !== 'ADMIN') {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
```

### 12.2 Pinia 状态管理

#### UserStore

```javascript
// src/stores/user.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, refreshToken as refreshApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(studentNo, password) {
    const res = await loginApi({ studentNo, password })
    accessToken.value = res.data.accessToken
    user.value = res.data.user
    localStorage.setItem('accessToken', accessToken.value)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function refreshAccessToken() {
    const res = await refreshApi()
    accessToken.value = res.data.accessToken
    localStorage.setItem('accessToken', accessToken.value)
  }

  function logout() {
    accessToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
  }

  return { accessToken, user, isLoggedIn, login, refreshAccessToken, logout }
})
```

#### MessageStore

```javascript
// src/stores/message.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    const res = await getUnreadCount()
    unreadCount.value = res.data.count
  }

  function incrementUnread() {
    unreadCount.value++
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, incrementUnread, clearUnread }
})
```

### 12.3 Axios 封装（含 Token 自动刷新）

```javascript
// src/utils/request.js
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：注入 Access Token
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`
  }
  return config
})

// 响应拦截器：统一错误处理 + Token 刷新
let isRefreshing = false
let failedQueue = []

const processQueue = (error) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve()
  })
  failedQueue = []
}

request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code !== 0) {
      // 业务错误，用 ElMessage 提示
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return response.data
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新中，将请求加入队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => request(originalRequest))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const userStore = useUserStore()
        await userStore.refreshAccessToken()
        processQueue(null)
        return request(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        userStore.logout()
        router.push({ name: 'Login' })
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 其他错误
    const msg = error.response?.data?.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
```

### 12.4 API 接口定义

```javascript
// src/api/auth.js
import request from '@/utils/request'

export const sendCode = (email) => request.post('/auth/send-code', { email })
export const register = (data) => request.post('/auth/register', data)
export const verifyRegister = (data) => request.post('/auth/register/verify', data)
export const login = (data) => request.post('/auth/login', data)
export const refreshToken = () => request.post('/auth/refresh')
export const logout = () => request.post('/auth/logout')
export const forgotPassword = (email) => request.post('/auth/forgot-password', { email })
export const resetPassword = (data) => request.post('/auth/reset-password', data)

// src/api/post.js
export const getPosts = (params) => request.get('/posts', { params })
export const getPostDetail = (id) => request.get(`/posts/${id}`)
export const createPost = (data) => request.post('/posts', data)
export const updatePost = (id, data) => request.put(`/posts/${id}`, data)
export const deletePost = (id) => request.delete(`/posts/${id}`)

// src/api/comment.js
export const getComments = (postId, params) => request.get(`/posts/${postId}/comments`, { params })
export const createComment = (postId, data) => request.post(`/posts/${postId}/comments`, data)
export const deleteComment = (postId, id) => request.delete(`/posts/${postId}/comments/${id}`)

// src/api/interaction.js
export const toggleLike = (data) => request.post('/likes', data)
export const toggleFavorite = (data) => request.post('/favorites', data)
export const getMyFavorites = (params) => request.get('/favorites', { params })

// src/api/message.js
export const getConversations = () => request.get('/messages/conversations')
export const getChatHistory = (userId, params) => request.get(`/messages/conversations/${userId}`, { params })
export const getUnreadCount = () => request.get('/messages/unread-count')
export const markAsRead = (userId) => request.put(`/messages/conversations/${userId}/read`)

// src/api/admin.js
export const getDashboard = () => request.get('/admin/dashboard')
export const getUsers = (params) => request.get('/admin/users', { params })
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const getPendingPosts = (params) => request.get('/admin/posts/pending', { params })
export const auditPost = (id, approved) => request.put(`/admin/posts/${id}/audit`, { approved })
export const togglePin = (id) => request.put(`/admin/posts/${id}/pin`)
export const toggleFeature = (id) => request.put(`/admin/posts/${id}/feature`)
export const adminDeletePost = (id) => request.delete(`/admin/posts/${id}`)
```

### 12.5 核心页面设计

#### 导航栏 `Navbar.vue`

```
┌──────────────────────────────────────────────────────────────┐
│  [Logo] 校园论坛    [首页] [板块▼] [搜索框........] [🔍]      │
│                                              [消息(3)] [头像▼] │
└──────────────────────────────────────────────────────────────┘
```

- 左侧：Logo + 导航链接
- 中间：搜索框（回车跳转 `/search?keyword=xxx`）
- 右侧：未读消息角标（连接 WebSocket 实时更新）、用户头像下拉菜单

#### 帖子卡片 `PostCard.vue`

```
┌────────────────────────────────────────────────────┐
│ [置顶] [精华]  帖子标题                              │
│                                                     │
│ 内容摘要前 100 字...                                 │
│                                                     │
│ [头像] 昵称  ·  板块名  ·  2小时前  ·  👁 128  💬 15  👍 32 │
└────────────────────────────────────────────────────┘
```

#### 帖子详情 `PostDetail.vue`

```
┌──────────────────────────────────────────────────────┐
│  帖子标题                                    [置顶][精华]  │
│  [头像] 昵称  ·  2026-05-27 10:30  ·  板块名           │
│  ──────────────────────────────────────────────        │
│  富文本内容（渲染 HTML）                                 │
│  ──────────────────────────────────────────────        │
│  [👍 点赞 32]  [⭐ 收藏 5]  [💬 评论 15]               │
│                                                         │
│  ──────── 评论区 ────────                               │
│  [输入评论............] [发送]                          │
│                                                         │
│  [头像] 用户A · 1小时前                                 │
│  这是一条评论内容                                        │
│    [👍 3] [回复]                                        │
│                                                         │
│    [头像] 用户B → 用户A · 30分钟前                      │
│    这是一条回复                                          │
│      [👍 1] [回复]                                      │
└──────────────────────────────────────────────────────┘
```

#### 私信页面 `Message.vue`

```
┌──────────────────────┬────────────────────────────────┐
│ 会话列表              │ 聊天窗口                        │
│                      │                                │
│ ┌──────────────────┐ │  用户B                         │
│ │ [头像] 用户B      │ │  ──────────────────            │
│ │ 最后一条消息...   │ │        你好，书还在吗？ 10:30  │
│ │ 10:30     [3]    │ │  在的，什么时候方便？   10:32  │
│ └──────────────────┘ │        周六下午可以吗？  10:35  │
│                      │  ──────────────────            │
│ ┌──────────────────┐ │  [输入消息........] [发送]      │
│ │ [头像] 用户C      │ │                                │
│ │ 消息内容...       │ │                                │
│ │ 昨天              │ │                                │
│ └──────────────────┘ │                                │
└──────────────────────┴────────────────────────────────┘
```

#### 管理后台 Dashboard.vue

```
┌──────────────────────────────────────────────────────────┐
│  数据概览                                                  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│  │ 总用户  │ │ 总帖子  │ │ 今日新增│ │ 7日活跃 │            │
│  │  1,234 │ │  5,678 │ │   15   │ │   320  │            │
│  └────────┘ └────────┘ └────────┘ └────────┘            │
│                                                           │
│  各板块帖子分布（柱状图 / 表格）                             │
│  校园公告: ████ 120                                        │
│  二手交易: ████████████ 450                                │
│  学习交流: ████████ 280                                    │
│  ...                                                      │
└──────────────────────────────────────────────────────────┘
```

### 12.6 富文本编辑器集成（wangEditor）

```vue
<!-- src/components/RichEditor.vue -->
<template>
  <div class="rich-editor">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      mode="default"
    />
    <Editor
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      mode="default"
      @onCreated="handleCreated"
    />
  </div>
</template>

<script setup>
import { ref, shallowRef, onBeforeUnmount } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { getToken } from '@/utils/auth'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef(null)
const valueHtml = ref(props.modelValue)

const toolbarConfig = {
  excludeKeys: ['fullScreen', 'group-video'] // 排除全屏和视频
}

const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/posts/upload-image',
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      allowedFileTypes: ['image/*'],
      headers: {
        Authorization: `Bearer ${getToken()}`
      },
      // wangEditor 要求返回格式：{ errno: 0, data: { url } }
      customInsert(res, insertFn) {
        if (res.code === 0) {
          insertFn(res.data.url)
        }
      }
    }
  }
}

const handleCreated = (editor) => {
  editorRef.value = editor
}

// 监听内容变化
watch(valueHtml, (val) => {
  emit('update:modelValue', val)
})

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>
```

### 12.7 响应式断点

```scss
// src/assets/styles/global.scss

// 断点变量
$breakpoint-mobile: 768px;
$breakpoint-tablet: 1024px;

// 响应式 mixin
@mixin mobile {
  @media (max-width: #{$breakpoint-mobile - 1px}) { @content; }
}

@mixin tablet {
  @media (min-width: $breakpoint-mobile) and (max-width: #{$breakpoint-tablet - 1px}) { @content; }
}

@mixin desktop {
  @media (min-width: $breakpoint-tablet) { @content; }
}

// 布局示例
.main-layout {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  gap: 20px;

  .content { flex: 1; min-width: 0; }
  .sidebar { width: 280px; }

  @include mobile {
    flex-direction: column;
    padding: 10px;
    .sidebar { display: none; }  // 手机端隐藏侧边栏
  }

  @include tablet {
    .sidebar { width: 200px; }
  }
}
```

---

*文档结束*
