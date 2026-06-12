# 用户模块 (User) — 任务清单

> 前置依赖：common、auth 模块完成（User 实体已在 auth 中创建）

---

## 任务 1：DTO 定义

- [ ] 1.1 创建 `UpdateProfileRequest` DTO（@Size nickname 2-20，@Size bio max 100）
- [ ] 1.2 创建 `UserProfileVO`（id/studentNo脱敏/nickname/avatar/bio/postCount/likeCount/createdAt）
- [ ] 1.3 创建 `AuthorVO`（id/nickname/avatar，帖子和评论中复用）

---

## 任务 2：Mapper 层

- [ ] 2.1 在 `UserMapper` 中新增 `selectProfileById(id)` → UserProfileVO（含发帖数、获赞数联查）
- [ ] 2.2 新增 `updateAvatar(id, avatarPath)` 方法
- [ ] 2.3 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `UserService.getCurrentUser()` — 查询当前用户，学号脱敏处理（2021****0001）
- [ ] 3.2 创建 `UserService.getUserById(id)` — 查询指定用户公开信息
- [ ] 3.3 创建 `UserService.updateProfile(request)` — 更新昵称和简介
- [ ] 3.4 创建 `UserService.uploadAvatar(file)` — 调用 FileUtil 上传头像，更新 user 表
- [ ] 3.5 编写单元测试：脱敏逻辑、更新逻辑、Mock FileUtil 测试头像上传

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/users/me` → getCurrentUser()
- [ ] 4.2 `PUT /api/users/me` → updateProfile(@Valid UpdateProfileRequest)
- [ ] 4.3 `POST /api/users/me/avatar` → uploadAvatar(@RequestParam MultipartFile)
- [ ] 4.4 `GET /api/users/{id}` → getUserById(@PathVariable Long id)
- [ ] 4.5 编写集成测试：
  - [ ] 获取当前用户信息（需认证）
  - [ ] 更新个人信息（昵称、简介）
  - [ ] 上传头像（校验文件类型和大小）
  - [ ] 未登录访问 /me 返回 401
  - [ ] 查看其他用户公开信息

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
