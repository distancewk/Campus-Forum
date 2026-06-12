# 评论模块 (Comment) — 任务清单

> 前置依赖：common、user、post 模块完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `Comment` 实体类（id/content/authorId/postId/parentId/replyToUserId/likeCount/status/createdAt/deleted）
- [ ] 1.2 创建 `CommentCreateRequest` DTO（@NotBlank content @Size max 500，parentId 可选，replyToUserId 可选）
- [ ] 1.3 创建 `CommentVO`（id/content/author/parentId/replyToUser/likeCount/isLiked/isOwner/createdAt/replies 子回复列表）

---

## 任务 2：Mapper 层

- [ ] 2.1 创建 `CommentMapper`（extends BaseMapper<Comment>）
- [ ] 2.2 实现 `selectTopLevelComments(postId, page, size)` → 分页查询顶层评论（parent_id IS NULL，按时间正序）
- [ ] 2.3 实现 `selectRepliesByParentIds(parentIds)` → 批量查询一级回复
- [ ] 2.4 实现 `updateLikeCount(commentId, delta)`
- [ ] 2.5 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `CommentService.listComments(postId, query)` — 树形结构组装：
  - [ ] 分页查询顶层评论
  - [ ] 批量查询子回复（避免 N+1）
  - [ ] 查询作者信息、回复目标用户
  - [ ] 查询当前用户点赞状态
  - [ ] 组装 CommentVO.replies
- [ ] 3.2 创建 `CommentService.createComment(postId, request)` — 校验帖子存在、校验父评论存在、XSS 过滤、插入评论、帖子评论数 +1
- [ ] 3.3 创建 `CommentService.deleteComment(postId, commentId)` — 校验权限、逻辑删除、帖子评论数 -1
- [ ] 3.4 编写单元测试：树形组装逻辑、评论数联动、权限校验

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/posts/{postId}/comments` → listComments(@PathVariable, @Valid PageQuery)（公开接口）
- [ ] 4.2 `POST /api/posts/{postId}/comments` → createComment(@PathVariable, @Valid @RequestBody)（需认证）
- [ ] 4.3 `DELETE /api/posts/{postId}/comments/{id}` → deleteComment(@PathVariable postId, @PathVariable id)（需认证）
- [ ] 4.4 编写集成测试：
  - [ ] 获取帖子评论列表（树形结构验证）
  - [ ] 发表顶层评论
  - [ ] 发表楼中楼回复
  - [ ] 删除自己的评论
  - [ ] 非作者删除评论返回 403
  - [ ] 评论后帖子评论数正确递增
  - [ ] 删除评论后帖子评论数正确递减

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
