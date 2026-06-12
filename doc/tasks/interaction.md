# 互动模块 (Interaction) — 任务清单

> 前置依赖：common、user、post、comment 模块完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `Like` 实体类（@TableName("\"like\"")，id/userId/targetType/targetId/createdAt）
- [ ] 1.2 创建 `Favorite` 实体类（id/userId/postId/createdAt）
- [ ] 1.3 创建 `LikeRequest` DTO（@NotBlank @Pattern "POST|COMMENT" targetType，@NotNull targetId）
- [ ] 1.4 创建 `FavoriteRequest` DTO（@NotNull postId）
- [ ] 1.5 创建 `ToggleResponse`（active boolean，count int）

---

## 任务 2：Mapper 层

- [ ] 2.1 创建 `LikeMapper`（extends BaseMapper<Like>）
- [ ] 2.2 创建 `FavoriteMapper`（extends BaseMapper<Favorite>）
- [ ] 2.3 实现 `selectCountByTarget(targetType, targetId)` → int
- [ ] 2.4 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `LikeService.toggleLike(userId, targetType, targetId)` — 幂等切换：
  - [ ] 查询是否已点赞
  - [ ] 已点赞 → 删除记录，目标计数 -1
  - [ ] 未点赞 → 插入记录，目标计数 +1
  - [ ] @Transactional 保证原子性
  - [ ] 返回 ToggleResponse
- [ ] 3.2 创建 `FavoriteService.toggleFavorite(userId, postId)` — 幂等切换（逻辑同上）
- [ ] 3.3 创建 `FavoriteService.listMyFavorites(userId, query)` — 分页查询用户收藏的帖子
- [ ] 3.4 编写单元测试：幂等性验证（连续点赞/取消）、计数联动、跨类型点赞

---

## 任务 4：Controller 层

- [ ] 4.1 `POST /api/likes` → toggleLike(@Valid LikeRequest)（需认证）
- [ ] 4.2 `POST /api/favorites` → toggleFavorite(@Valid FavoriteRequest)（需认证）
- [ ] 4.3 `GET /api/favorites` → myFavorites(@Valid PageQuery)（需认证）
- [ ] 4.4 编写集成测试：
  - [ ] 点赞帖子 → 返回 active=true，帖子 like_count +1
  - [ ] 再次点赞（取消）→ 返回 active=false，帖子 like_count -1
  - [ ] 点赞评论
  - [ ] 收藏帖子 → 返回 active=true
  - [ ] 再次收藏（取消）→ 返回 active=false
  - [ ] 获取我的收藏列表
  - [ ] 未登录点赞返回 401

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
