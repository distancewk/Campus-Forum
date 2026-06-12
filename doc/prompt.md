# 校园论坛网站 — Vibe Coding 执行 Prompt

> 本 Prompt 用于驱动 AI Agent 自动完成校园论坛网站的全栈开发。
> 输入文档：[需求文档](./proposal.md) · [详细设计](./detailed-design.md) · [任务清单](./tasks/)

---

## 一、项目概述

构建一个校园论坛网站，为在校学生提供信息聚合、交流互动的专属平台。

### 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + wangEditor |
| 后端 | Spring Boot 3 + MyBatis-Plus + PostgreSQL + Redis + Spring Security + JWT |
| 实时通信 | WebSocket (STOMP over SockJS) |
| 数据库迁移 | Flyway |
| 接口文档 | Knife4j (OpenAPI 3) |
| E2E 测试 | Playwright |

### 项目结构

```
Campus Forum/
├── campus-forum-backend/          # 后端 Spring Boot 项目
│   ├── src/main/java/com/campus/
│   │   ├── CampusForumApplication.java
│   │   ├── common/                # 公共模块
│   │   │   ├── response/          # R<T>, PageResult<T>
│   │   │   ├── enums/             # ResultCode
│   │   │   ├── exception/         # BusinessException, GlobalExceptionHandler
│   │   │   ├── util/              # JwtUtil, SecurityUtil, FileUtil, EmailUtil, RedisUtil
│   │   │   └── config/            # SecurityConfig, CorsConfig, MyBatisPlusConfig
│   │   ├── auth/                  # 认证模块
│   │   ├── user/                  # 用户模块
│   │   ├── board/                 # 板块模块
│   │   ├── post/                  # 帖子模块
│   │   ├── comment/               # 评论模块
│   │   ├── interaction/           # 互动模块（点赞/收藏）
│   │   ├── message/               # 私信模块
│   │   ├── search/                # 搜索模块
│   │   └── admin/                 # 管理模块
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── log4j2.xml
│       ├── db/migration/          # Flyway SQL
│       └── mapper/                # MyBatis XML
├── campus-forum-frontend/         # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                   # API 接口定义
│   │   ├── assets/styles/         # 全局样式
│   │   ├── components/            # 公共组件
│   │   │   ├── layout/            # MainLayout, Navbar, Sidebar, AdminLayout
│   │   │   ├── PostCard.vue
│   │   │   ├── RichEditor.vue
│   │   │   └── CommentList.vue
│   │   ├── router/                # 路由配置
│   │   ├── stores/                # Pinia 状态管理
│   │   ├── utils/                 # request.js, auth.js, websocket.js
│   │   └── views/                 # 页面视图
│   │       ├── auth/              # Login, Register, ResetPassword
│   │       ├── home/              # Home
│   │       ├── post/              # PostList, PostDetail, PostCreate
│   │       ├── profile/           # Profile
│   │       ├── message/           # Message
│   │       ├── search/            # SearchResult
│   │       └── admin/             # Dashboard, UserManage, ContentAudit
│   └── vite.config.js
└── doc/                           # 文档
    ├── proposal.md
    ├── detailed-design.md
    ├── tasks/
    └── prompt.md
```

---

## 二、执行模式

### 主 Agent（协调者）

负责：
1. 按依赖顺序调度子 Agent 执行各模块
2. 跟踪整体进度（更新 `doc/tasks/progress.md`）
3. 处理模块间的依赖关系和接口对齐
4. 在每个模块完成后验证基本可用性

### 子 Agent（执行者）

由主 Agent 按需生成，每个子 Agent 负责一个模块的完整实现：
1. 阅读该模块的任务清单（`doc/tasks/{module}.md`）
2. 阅读详细设计中对应模块的章节
3. 按任务清单逐项实现代码
4. 编写单元测试和集成测试
5. 运行测试确保通过
6. 完成后向主 Agent 报告结果

### 执行流程

```
Phase 1: 后端基础
  ├── [子Agent] common 模块（项目初始化、Flyway、公共类、Security、JWT、工具类）
  ├── [子Agent] auth 模块（注册、登录、Token 刷新、登出、忘记密码）
  ├── [子Agent] user 模块（个人信息、头像上传）
  └── [子Agent] board 模块（板块 CRUD）

Phase 2: 后端核心功能
  ├── [子Agent] post 模块（帖子 CRUD、热度分、图片上传）
  ├── [子Agent] comment 模块（评论树形结构、楼中楼）
  ├── [子Agent] interaction 模块（点赞/收藏）
  └── [子Agent] search 模块（帖子搜索）

Phase 3: 后端高级功能
  ├── [子Agent] message 模块（WebSocket 私信、会话管理）
  └── [子Agent] admin 模块（用户管理、内容审核、数据统计）

Phase 4: 前端基础
  ├── [子Agent] layout 模块（项目初始化、Axios、Pinia、路由、布局组件、公共组件）

Phase 5: 前端页面
  ├── [子Agent] fe-auth 模块（登录、注册、忘记密码页面）
  ├── [子Agent] fe-home 模块（首页帖子流、板块导航）
  ├── [子Agent] fe-post 模块（帖子详情、评论区、发帖页）
  ├── [子Agent] fe-profile 模块（个人中心、我的帖子、收藏）
  ├── [子Agent] fe-search 模块（搜索结果页）
  ├── [子Agent] fe-message 模块（私信页面、实时通信）
  └── [子Agent] fe-admin 模块（管理后台页面）

Phase 6: E2E 测试
  └── [子Agent] e2e 模块（Playwright 端到端测试）
```

### 并行策略

- Phase 1 内：common 完成后，auth / board 可并行；auth 完成后 user 可开始
- Phase 2 内：post 依赖 user + board；comment 依赖 post；interaction 依赖 comment；search 依赖 post
- Phase 3 内：message 依赖 user；admin 依赖 user + post + board；两者可并行
- Phase 5 内：所有前端页面模块依赖 layout 完成后可并行

---

## 三、编码规范

### 后端规范

1. **包结构**：`com.campus.{module}.{layer}`，layer = controller / service / mapper / entity / dto / config / util
2. **命名**：
   - 实体类：与表名一致（PascalCase），如 `User`、`Post`
   - DTO：`{模块}{动作}{Request/Response/VO}`，如 `RegisterRequest`、`PostListVO`
   - Mapper 方法：`select{Xxx}` / `insert{Xxx}` / `update{Xxx}` / `delete{Xxx}`
3. **统一响应**：所有接口返回 `R<T>`，业务错误通过 `BusinessException` 抛出
4. **分页**：请求继承 `PageQuery`，响应使用 `PageResult<T>`
5. **认证**：通过 `SecurityUtil.getCurrentUserId()` 获取当前用户 ID
6. **校验**：使用 Jakarta Validation 注解（`@NotBlank`、`@Size`、`@Pattern` 等）
7. **事务**：写操作涉及多表时使用 `@Transactional`
8. **XSS 防护**：富文本内容使用 Jsoup 清理，纯文本内容 escape HTML
9. **逻辑删除**：使用 MyBatis-Plus `@TableLogic` 注解，`deleted` 字段 0/1

### 前端规范

1. **组件**：Composition API (`<script setup>`)，单文件组件 `.vue`
2. **状态管理**：Pinia，按功能拆分 store（user / message）
3. **API 调用**：统一通过 `api/` 目录下的模块，使用 `utils/request.js` 封装的 Axios 实例
4. **路由**：懒加载 `() => import(...)`，路由守卫控制权限
5. **样式**：SCSS，使用响应式断点 mixin（mobile / tablet / desktop）
6. **UI 组件**：Element Plus，中文 locale
7. **富文本**：wangEditor，图片上传走 `/api/posts/upload-image`
8. **WebSocket**：STOMP over SockJS，登录后自动连接，登出断开

### 测试规范

1. **后端单元测试**：JUnit 5 + Mockito，Mock 外部依赖（Redis、邮件、文件系统）
2. **后端集成测试**：`@SpringBootTest` + `@AutoConfigureMockMvc`，使用 Testcontainers 或 H2 内存库
3. **前端**：关键组件的基本渲染测试（可选）
4. **E2E 测试**：Playwright，覆盖核心用户流程

---

## 四、数据库概要

### 核心表（8 张）

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | student_no(唯一), nickname, password(BCrypt), email(唯一), role(STUDENT/ADMIN), status, login_fail, locked_until |
| `board` | 板块表 | name, description, icon, sort_order, status |
| `post` | 帖子表 | title, content(HTML), author_id→user, board_id→board, view/like/comment/fav_count, hot_score, is_pinned, is_featured, status |
| `comment` | 评论表 | content, author_id→user, post_id→post, parent_id→comment(楼中楼), reply_to_user_id, like_count |
| `like` | 点赞表 | user_id→user, target_type(POST/COMMENT), target_id, UNIQUE(user_id, target_type, target_id) |
| `favorite` | 收藏表 | user_id→user, post_id→post, UNIQUE(user_id, post_id) |
| `message` | 私信表 | sender_id→user, receiver_id→user, content, is_read, deleted_by_sender/receiver |
| `verify_code` | 验证码表 | email, code, type(REGISTER/RESET_PASSWORD), used, expires_at |

### Flyway 迁移文件

- `V1__init_schema.sql`：建表 + 索引
- `V2__init_data.sql`：预设板块 + 管理员账号

---

## 五、API 概要

### 公开接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/send-code` | 发送注册验证码 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/register/verify` | 验证码校验完成注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/refresh` | 刷新 Token |
| POST | `/api/auth/logout` | 登出 |
| POST | `/api/auth/forgot-password` | 忘记密码 |
| POST | `/api/auth/reset-password` | 重置密码 |
| GET | `/api/boards` | 获取板块列表 |
| GET | `/api/posts` | 获取帖子列表（分页、排序、筛选） |
| GET | `/api/posts/{id}` | 获取帖子详情 |
| GET | `/api/posts/{postId}/comments` | 获取评论列表 |
| GET | `/api/search` | 搜索帖子 |

### 需认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/me` | 获取当前用户信息 |
| PUT | `/api/users/me` | 更新个人信息 |
| POST | `/api/users/me/avatar` | 上传头像 |
| GET | `/api/users/{id}` | 获取用户公开信息 |
| POST | `/api/posts` | 发布帖子 |
| PUT | `/api/posts/{id}` | 编辑帖子 |
| DELETE | `/api/posts/{id}` | 删除帖子 |
| POST | `/api/posts/upload-image` | 上传帖子图片 |
| POST | `/api/posts/{postId}/comments` | 发表评论 |
| DELETE | `/api/posts/{postId}/comments/{id}` | 删除评论 |
| POST | `/api/likes` | 点赞/取消点赞 |
| POST | `/api/favorites` | 收藏/取消收藏 |
| GET | `/api/favorites` | 我的收藏列表 |
| GET | `/api/messages/conversations` | 会话列表 |
| GET | `/api/messages/conversations/{userId}` | 聊天记录 |
| GET | `/api/messages/unread-count` | 未读消息数 |
| PUT | `/api/messages/conversations/{userId}/read` | 标记已读 |

### 管理员接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/status` | 禁用/启用用户 |
| GET | `/api/admin/posts/pending` | 待审核帖子 |
| PUT | `/api/admin/posts/{id}/audit` | 审核帖子 |
| PUT | `/api/admin/posts/{id}/pin` | 置顶/取消置顶 |
| PUT | `/api/admin/posts/{id}/feature` | 精华/取消精华 |
| DELETE | `/api/admin/posts/{id}` | 管理员删除帖子 |
| GET | `/api/admin/dashboard` | 数据统计 |
| POST | `/api/admin/boards` | 创建板块 |
| PUT | `/api/admin/boards/{id}` | 更新板块 |
| DELETE | `/api/admin/boards/{id}` | 删除板块 |

---

## 六、前端页面路由

| 路由 | 页面 | 权限 |
|------|------|------|
| `/` | 首页（帖子流 + 板块导航） | 公开 |
| `/board/:id` | 板块页（帖子列表） | 公开 |
| `/post/:id` | 帖子详情（内容 + 评论） | 公开 |
| `/post/create` | 发帖页 | 登录 |
| `/post/:id/edit` | 编辑帖子 | 登录 |
| `/login` | 登录页 | 公开 |
| `/register` | 注册页 | 公开 |
| `/profile` | 个人中心 | 登录 |
| `/profile/:id` | 他人主页 | 公开 |
| `/messages` | 私信页 | 登录 |
| `/search` | 搜索结果 | 公开 |
| `/admin` | 管理后台 - 数据概览 | 管理员 |
| `/admin/users` | 管理后台 - 用户管理 | 管理员 |
| `/admin/content` | 管理后台 - 内容审核 | 管理员 |

---

## 七、质量标准

### 代码质量

- [ ] 后端：所有 Service 方法有对应的单元测试
- [ ] 后端：所有 Controller 接口有对应的集成测试
- [ ] 后端：通过 Maven 编译无错误无警告
- [ ] 后端：MyBatis XML 查询语法正确
- [ ] 前端：组件无 TypeScript/ESLint 错误
- [ ] 前端：API 调用有错误处理和 loading 状态
- [ ] 前端：响应式布局在三个断点下正常显示

### 功能完整性

- [ ] 用户注册 → 邮箱验证码 → 登录 → Token 刷新 → 登出 完整流程
- [ ] 帖子发布 → 浏览 → 编辑 → 删除 完整流程
- [ ] 评论发表 → 楼中楼回复 → 删除 完整流程
- [ ] 点赞/收藏 幂等切换 + 计数联动
- [ ] 私信 WebSocket 实时收发 + 未读数更新
- [ ] 搜索关键词匹配 + 板块内筛选
- [ ] 管理员用户管理 + 内容审核 + 数据统计
- [ ] 前后端联调：所有页面功能可正常使用

### 安全性

- [ ] 密码 BCrypt 加密存储
- [ ] JWT Token 认证 + HttpOnly Cookie 存储 Refresh Token
- [ ] 富文本 XSS 过滤（Jsoup）
- [ ] 输入参数校验（前后端双重）
- [ ] 权限控制：普通用户仅操作自己的数据
- [ ] CORS 配置正确
- [ ] 文件上传：类型和大小校验

---

## 八、子 Agent 指令模板

当主 Agent 调度子 Agent 执行某个模块时，使用以下指令格式：

```
你是校园论坛项目的开发者。请实现 {模块名} 模块。

## 你的任务

1. 阅读任务清单：doc/tasks/{module}.md
2. 阅读详细设计中对应的章节：doc/detailed-design.md（第 {N} 章）
3. 按任务清单逐项实现，每完成一项勾选对应的 checkbox
4. 编写单元测试和集成测试
5. 运行测试确保通过

## 技术要求

- 后端：Spring Boot 3 + MyBatis-Plus + PostgreSQL
- 前端：Vue 3 + Element Plus + Pinia
- 包名/路径：遵循项目结构（见下方）
- 所有接口返回 R<T>，异常通过 BusinessException 抛出
- 详细设计文档中有完整的代码示例，直接参考实现

## 项目路径

- 后端根目录：campus-forum-backend/
- 前端根目录：campus-forum-frontend/

## 依赖模块（已完成）

{列出已实现的依赖模块}

## 完成标准

- [ ] 所有任务清单项已勾选
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 代码无编译错误
```

---

## 九、启动与验证

### 后端启动

```bash
cd campus-forum-backend
# 确保 PostgreSQL 和 Redis 已启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 验证：访问 http://localhost:8080/doc.html (Knife4j)
```

### 前端启动

```bash
cd campus-forum-frontend
pnpm install
pnpm dev
# 验证：访问 http://localhost:5173
```

### E2E 测试

```bash
cd campus-forum-frontend
pnpm test:e2e
```

---

## 十、注意事项

1. **数据库**：开发环境使用 PostgreSQL，确保 `campus_forum` 数据库已创建
2. **Redis**：验证码和限流依赖 Redis，确保 Redis 服务已启动
3. **邮件**：开发环境可使用 Mailtrap 等测试邮件服务，或 Mock EmailUtil
4. **文件上传**：上传目录 `./uploads/`，需确保目录存在且有写权限
5. **WebSocket**：前端 SockJS 连接 `/ws` 端点，需确保后端 WebSocket 配置正确
6. **CORS**：开发环境前端 `localhost:5173`，后端 `localhost:8080`，CORS 需放行
7. **管理员账号**：`V2__init_data.sql` 中预设，首次使用需修改密码

---

*Prompt 结束。按照上述执行模式，由主 Agent 调度子 Agent 按阶段顺序完成开发。*
