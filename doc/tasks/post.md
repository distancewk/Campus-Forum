# 帖子模块 (Post) — 任务清单

> 前置依赖：common、user、board 模块完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `Post` 实体类（id/title/content/authorId/boardId/viewCount/likeCount/commentCount/favCount/hotScore/isPinned/isFeatured/status/createdAt/updatedAt/deleted）
- [ ] 1.2 创建 `PostQuery`（extends PageQuery，boardId/sort/keyword）
- [ ] 1.3 创建 `PostCreateRequest` DTO（@NotBlank title @Size max 100，@NotBlank content，@NotNull boardId）
- [ ] 1.4 创建 `PostUpdateRequest` DTO（title @Size max 100，content，均为可选）
- [ ] 1.5 创建 `PostListVO`（id/title/summary/authorNickname/authorAvatar/boardId/boardName/viewCount/likeCount/commentCount/isPinned/isFeatured/createdAt）
- [ ] 1.6 创建 `PostVO`（完整详情，含 isLiked/isFavorited/isOwner 标记）
- [ ] 1.7 创建 `AuthorVO`（如 user 模块未创建则在此创建）

---

## 任务 2：Mapper 层

- [ ] 2.1 创建 `PostMapper`（extends BaseMapper<Post>）
- [ ] 2.2 实现 `selectPostList(query)` → PageResult<PostListVO>（联查 author/board，支持 boardId 筛选、三种排序）
- [ ] 2.3 实现 `selectPostDetail(postId)` → PostVO（联查 author/board）
- [ ] 2.4 实现 `incrementViewCount(postId)`（view_count = view_count + 1）
- [ ] 2.5 实现 `updateLikeCount(postId, delta)`（like_count = like_count + delta）
- [ ] 2.6 实现 `updateCommentCount(postId, delta)`
- [ ] 2.7 实现 `updateFavCount(postId, delta)`
- [ ] 2.8 实现 `recalculateHotScore()`（批量更新近 7 天帖子热度分）
- [ ] 2.9 编写 MyBatis XML 映射文件（复杂查询用 XML）
- [ ] 2.10 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `PostService.listPosts(query)` — 分页查询，按 sort 参数排序
- [ ] 3.2 创建 `PostService.getPostDetail(postId)` — 查询详情 + 浏览量 +1 + 查询当前用户点赞/收藏状态
- [ ] 3.3 创建 `PostService.createPost(request)` — 校验板块、XSS 过滤（Jsoup）、插入数据库
- [ ] 3.4 创建 `PostService.updatePost(postId, request)` — 校验权限（仅作者）、更新字段
- [ ] 3.5 创建 `PostService.deletePost(postId)` — 校验权限（作者或管理员）、逻辑删除
- [ ] 3.6 创建 `PostService.uploadImage(file)` — 调用 FileUtil 上传，返回 { url } 格式（wangEditor 要求）
- [ ] 3.7 内容摘要生成工具方法：去除 HTML 标签，取前 100 字
- [ ] 3.8 编写单元测试：XSS 过滤、摘要生成、权限校验、计数更新

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/posts` → listPosts(@Valid PostQuery)（公开接口）
- [ ] 4.2 `GET /api/posts/{id}` → getPostDetail(@PathVariable)（公开接口）
- [ ] 4.3 `POST /api/posts` → createPost(@Valid @RequestBody)（需认证）
- [ ] 4.4 `PUT /api/posts/{id}` → updatePost(@PathVariable, @Valid @RequestBody)（需认证）
- [ ] 4.5 `DELETE /api/posts/{id}` → deletePost(@PathVariable)（需认证）
- [ ] 4.6 `POST /api/posts/upload-image` → uploadImage(@RequestParam MultipartFile)（需认证）
- [ ] 4.7 编写集成测试：
  - [ ] 公开获取帖子列表（分页、排序、板块筛选）
  - [ ] 公开获取帖子详情（浏览量递增）
  - [ ] 登录用户发帖
  - [ ] 未登录发帖返回 401
  - [ ] 作者编辑自己的帖子
  - [ ] 非作者编辑帖子返回 403
  - [ ] 作者删除自己的帖子
  - [ ] 管理员删除任意帖子
  - [ ] 上传图片（校验格式和大小）

---

## 任务 5：热度分定时任务

- [ ] 5.1 创建 `HotScoreScheduler`（@Component, @Scheduled fixedRate=600000）
- [ ] 5.2 实现 `recalculateHotScore()` — 调用 Mapper 批量更新 SQL
- [ ] 5.3 热度公式：`(like_count*3 + comment_count*2 + fav_count*1) / POWER(hours+2, 1.5)`
- [ ] 5.4 在启动类添加 `@EnableScheduling`
- [ ] 5.5 编写单元测试：验证公式计算结果正确性

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
- 任务 5 依赖任务 2，可与任务 3、4 并行
