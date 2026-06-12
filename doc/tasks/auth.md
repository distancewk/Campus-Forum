# 认证模块 (Auth) — 任务清单

> 前置依赖：common 模块全部完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `User` 实体类（@TableName("\"user\"")，id/studentNo/nickname/password/email/avatar/bio/role/status/loginFail/lockedUntil/createdAt/updatedAt/deleted）
- [ ] 1.2 创建 `RegisterRequest` DTO（@NotBlank studentNo + @Pattern 学号格式、@Email + @Pattern 学校邮箱后缀、@Size password）
- [ ] 1.3 创建 `SendCodeRequest` DTO（@NotBlank @Email email）
- [ ] 1.4 创建 `VerifyCodeRequest` DTO（email + @Size(6) code）
- [ ] 1.5 创建 `LoginRequest` DTO（studentNo + password）
- [ ] 1.6 创建 `LoginResponse` DTO（accessToken + UserInfoVO）
- [ ] 1.7 创建 `UserInfoVO`（id/studentNo/nickname/avatar/role）
- [ ] 1.8 创建 `ResetPasswordRequest` DTO（email + code + newPassword）
- [ ] 1.9 创建 `RefreshTokenResponse` DTO（accessToken）

---

## 任务 2：Mapper 层

- [ ] 2.1 创建 `UserMapper`（extends BaseMapper<User>）
- [ ] 2.2 实现 `selectByStudentNo(studentNo)` → User
- [ ] 2.3 实现 `selectByEmail(email)` → User
- [ ] 2.4 编写 Mapper 单元测试（@MybatisTest + H2）

---

## 任务 3：发送验证码

- [ ] 3.1 创建 `AuthController.sendVerifyCode()` → POST /api/auth/send-code
- [ ] 3.2 创建 `AuthService.sendVerifyCode()` 逻辑：
  - [ ] 3.2.1 校验邮箱格式（学校后缀）
  - [ ] 3.2.2 检查 Redis 60 秒限流（key: `verify:limit:{email}`）
  - [ ] 3.2.3 生成 6 位随机验证码
  - [ ] 3.2.4 存入 Redis（key: `verify:code:{email}:REGISTER`，TTL 300s）
  - [ ] 3.2.5 调用 EmailUtil 异步发送
- [ ] 3.3 编写单元测试：限流逻辑、验证码生成、Redis 存储
- [ ] 3.4 编写集成测试：POST /api/auth/send-code 返回成功

---

## 任务 4：注册

- [ ] 4.1 创建 `AuthController.register()` → POST /api/auth/register
- [ ] 4.2 创建 `AuthService.register()` 逻辑：
  - [ ] 4.2.1 校验学号是否已注册
  - [ ] 4.2.2 校验邮箱是否已注册
  - [ ] 4.2.3 暂存注册信息到 Redis（key: `register:pending:{email}`，TTL 300s）
- [ ] 4.3 创建 `AuthController.verifyAndComplete()` → POST /api/auth/register/verify
- [ ] 4.4 创建 `AuthService.verifyAndComplete()` 逻辑：
  - [ ] 4.4.1 从 Redis 取验证码比对
  - [ ] 4.4.2 从 Redis 取暂存的注册信息
  - [ ] 4.4.3 密码 BCrypt 加密
  - [ ] 4.4.4 插入 user 表
  - [ ] 4.4.5 生成 Access Token + Refresh Token
  - [ ] 4.4.6 Refresh Token 写入 HttpOnly Cookie
  - [ ] 4.4.7 返回 LoginResponse
- [ ] 4.5 编写单元测试：学号/邮箱重复检测、验证码比对、密码加密
- [ ] 4.6 编写集成测试：完整注册流程（发验证码 → 注册 → 校验返回 Token）

---

## 任务 5：登录

- [ ] 5.1 创建 `AuthController.login()` → POST /api/auth/login
- [ ] 5.2 创建 `AuthService.login()` 逻辑：
  - [ ] 5.2.1 根据 studentNo 查询用户（不存在返回 415）
  - [ ] 5.2.2 检查账号状态（禁用返回 410）
  - [ ] 5.2.3 检查锁定状态（locked_until > NOW 返回 416）
  - [ ] 5.2.4 BCrypt 校验密码
  - [ ] 5.2.5 密码错误：login_fail++，连续 5 次设置 locked_until = NOW + 15min
  - [ ] 5.2.6 密码正确：login_fail 归 0
  - [ ] 5.2.7 生成 Access Token + Refresh Token
  - [ ] 5.2.8 Refresh Token 写入 HttpOnly Cookie
  - [ ] 5.2.9 返回 LoginResponse
- [ ] 5.3 编写单元测试：密码校验、锁定逻辑（5 次失败锁定 15 分钟）、禁用检测
- [ ] 5.4 编写集成测试：正常登录、密码错误、账号锁定、账号禁用

---

## 任务 6：Token 刷新

- [ ] 6.1 创建 `AuthController.refresh()` → POST /api/auth/refresh
- [ ] 6.2 创建 `AuthService.refreshToken()` 逻辑：
  - [ ] 6.2.1 从 Cookie 提取 Refresh Token
  - [ ] 6.2.2 校验签名和有效期
  - [ ] 6.2.3 提取 userId，查询用户存在且未禁用
  - [ ] 6.2.4 生成新 Access Token
  - [ ] 6.2.5 可选：Rotation 生成新 Refresh Token 替换旧的
  - [ ] 6.2.6 返回新 Access Token
- [ ] 6.3 编写集成测试：正常刷新、Refresh Token 过期、用户被禁用

---

## 任务 7：登出与忘记密码

- [ ] 7.1 创建 `AuthController.logout()` → POST /api/auth/logout
- [ ] 7.2 登出逻辑：清除 Refresh Token Cookie
- [ ] 7.3 创建 `AuthController.forgotPassword()` → POST /api/auth/forgot-password
- [ ] 7.4 忘记密码逻辑：发送重置验证码（复用 sendVerifyCode，type=RESET_PASSWORD）
- [ ] 7.5 创建 `AuthController.resetPassword()` → POST /api/auth/reset-password
- [ ] 7.6 重置密码逻辑：校验验证码 → BCrypt 加密新密码 → 更新 user 表
- [ ] 7.7 编写集成测试：完整重置密码流程

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4 → 任务 5 → 任务 6 → 任务 7（基本顺序）
- 任务 6 可与任务 7 并行
