# 管理模块 (Admin) — 任务清单

> 前置依赖：common、user、post、board 模块完成

---

## 任务 1：DTO 定义

- [ ] 1.1 创建 `UserAdminVO`（id/studentNo/nickname/email/role/status/postCount/createdAt/lockedUntil）
- [ ] 1.2 创建 `DashboardVO`（totalUsers/totalPosts/totalComments/todayNewUsers/todayNewPosts/activeUsers/boardStats）
- [ ] 1.3 创建 `BoardStat`（boardName/postCount）
- [ ] 1.4 创建 `StatusRequest` DTO（@In(0,1) status）
- [ ] 1.5 创建 `AuditRequest` DTO（@NotNull approved boolean）
- [ ] 1.6 创建 `AdminUserQuery`（extends PageQuery，keyword/status 可选）

---

## 任务 2：Mapper 层

- [ ] 2.1 在 `UserMapper` 中新增 `selectAdminUserList(query)` → PageResult<UserAdminVO>（含发帖数）
- [ ] 2.2 新增 `selectTotalUsers()` / `selectTodayNewUsers()` / `selectActiveUsers(days)` 统计方法
- [ ] 2.3 在 `PostMapper` 中新增 `selectTotalPosts()` / `selectTodayNewPosts()` / `selectPendingPosts(query)` 统计方法
- [ ] 2.4 在 `CommentMapper` 中新增 `selectTotalComments()` 统计方法
- [ ] 2.5 新增 `selectBoardStats()` → List<BoardStat>（各板块帖子数 GROUP BY）
- [ ] 2.6 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `AdminService.listUsers(query)` — 分页查询用户列表
- [ ] 3.2 创建 `AdminService.updateUserStatus(userId, status)` — 校验不能操作自己、不能操作其他管理员
- [ ] 3.3 创建 `AdminService.listPendingPosts(query)` — 查询待审核帖子（status=0）
- [ ] 3.4 创建 `AdminService.auditPost(postId, approved)` — 通过设 status=1，拒绝逻辑删除
- [ ] 3.5 创建 `AdminService.togglePin(postId)` — 切换置顶状态
- [ ] 3.6 创建 `AdminService.toggleFeature(postId)` — 切换精华状态
- [ ] 3.7 创建 `AdminService.deletePost(postId)` — 管理员强制删除
- [ ] 3.8 创建 `AdminService.getDashboard()` — 聚合统计数据
- [ ] 3.9 编写单元测试：权限校验（不能禁用自己/其他管理员）、审核逻辑、统计计算

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/admin/users` → listUsers(@Valid AdminUserQuery)
- [ ] 4.2 `PUT /api/admin/users/{id}/status` → updateUserStatus(@PathVariable, @Valid StatusRequest)
- [ ] 4.3 `GET /api/admin/posts/pending` → listPendingPosts(@Valid PageQuery)
- [ ] 4.4 `PUT /api/admin/posts/{id}/audit` → auditPost(@PathVariable, @Valid AuditRequest)
- [ ] 4.5 `PUT /api/admin/posts/{id}/pin` → togglePin(@PathVariable)
- [ ] 4.6 `PUT /api/admin/posts/{id}/feature` → toggleFeature(@PathVariable)
- [ ] 4.7 `DELETE /api/admin/posts/{id}` → deletePost(@PathVariable)
- [ ] 4.8 `GET /api/admin/dashboard` → getDashboard()
- [ ] 4.9 编写集成测试：
  - [ ] 管理员获取用户列表
  - [ ] 管理员禁用/启用用户
  - [ ] 管理员不能禁用自己
  - [ ] 普通用户访问管理接口返回 403
  - [ ] 管理员审核帖子（通过/拒绝）
  - [ ] 管理员置顶/精华帖子
  - [ ] 管理员删除帖子
  - [ ] 获取数据统计面板

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
