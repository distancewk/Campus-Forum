# 板块模块 (Board) — 任务清单

> 前置依赖：common 模块完成

---

## 任务 1：实体与 DTO 定义

- [ ] 1.1 创建 `Board` 实体类（@TableName("board")，id/name/description/icon/sortOrder/status/createdAt）
- [ ] 1.2 创建 `BoardVO`（id/name/description/icon/postCount）
- [ ] 1.3 创建 `BoardCreateRequest` DTO（@NotBlank name，@Size description/icon/sortOrder）
- [ ] 1.4 创建 `BoardUpdateRequest` DTO（name/description/icon/sortOrder/status，均为可选字段）

---

## 任务 2：Mapper 层

- [ ] 2.1 创建 `BoardMapper`（extends BaseMapper<Board>）
- [ ] 2.2 实现 `selectBoardListWithPostCount()` → List<BoardVO>（联查帖子数）
- [ ] 2.3 编写 Mapper 单元测试

---

## 任务 3：Service 层

- [ ] 3.1 创建 `BoardService.listBoards()` — 查询启用状态的板块列表，按 sortOrder 排序
- [ ] 3.2 创建 `BoardService.createBoard(request)` — 校验名称唯一，插入数据库
- [ ] 3.3 创建 `BoardService.updateBoard(id, request)` — 校验板块存在，更新字段
- [ ] 3.4 创建 `BoardService.deleteBoard(id)` — 检查板块下是否有帖子，有则拒绝删除
- [ ] 3.5 编写单元测试：列表排序、名称唯一校验、删除保护逻辑

---

## 任务 4：Controller 层

- [ ] 4.1 `GET /api/boards` → listBoards()（公开接口）
- [ ] 4.2 `POST /api/admin/boards` → createBoard()（@PreAuthorize ADMIN）
- [ ] 4.3 `PUT /api/admin/boards/{id}` → updateBoard()（@PreAuthorize ADMIN）
- [ ] 4.4 `DELETE /api/admin/boards/{id}` → deleteBoard()（@PreAuthorize ADMIN）
- [ ] 4.5 编写集成测试：
  - [ ] 公开获取板块列表
  - [ ] 管理员创建/编辑/删除板块
  - [ ] 普通用户访问管理接口返回 403
  - [ ] 删除有帖子的板块返回错误
  - [ ] 创建重复名称板块返回错误

---

## 依赖关系

- 任务 1 → 任务 2 → 任务 3 → 任务 4（顺序依赖）
