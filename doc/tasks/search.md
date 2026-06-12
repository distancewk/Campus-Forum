# 搜索模块 (Search) — 任务清单

> 前置依赖：common、post 模块完成

---

## 任务 1：DTO 定义

- [ ] 1.1 创建 `SearchQuery`（extends PageQuery，@NotBlank keyword，boardId 可选）

---

## 任务 2：Mapper 层

- [ ] 2.1 在 `PostMapper` 中新增 `searchPosts(query)` → PageResult<PostListVO>
- [ ] 2.2 SQL 逻辑：
  - [ ] `title ILIKE '%keyword%'` 或 `content（去除HTML标签后）ILIKE '%keyword%'`
  - [ ] 可选 boardId 筛选
  - [ ] 联查 author、board
  - [ ] 按时间倒序分页
- [ ] 2.3 编写 MyBatis XML 查询
- [ ] 2.4 编写 Mapper 单元测试：关键词匹配、板块内搜索、空结果

---

## 任务 3：Service 层

- [ ] 3.1 创建 `SearchService.search(query)` — 调用 Mapper 查询
- [ ] 3.2 搜索结果内容摘要生成（去除 HTML 标签，取前 100 字，关键词高亮可选）
- [ ] 3.3 编写单元测试：摘要生成、关键词高亮

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/search` → search(@Valid SearchQuery)（公开接口）
- [ ] 4.2 编写集成测试：
  - [ ] 关键词搜索返回匹配帖子
  - [ ] 板块内搜索
  - [ ] 空关键词返回 400
  - [ ] 无匹配结果返回空列表
  - [ ] 分页参数正确生效

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
