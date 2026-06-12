# 校园论坛网站 — 概要设计文档

> 版本：v1.0 | 日期：2026-05-27
> 基于：[需求文档 v1.0](./proposal.md)

---

## 一、系统架构

### 1.1 整体架构

采用前后端分离的单体架构，分为三层：

```
┌─────────────────────────────────────────────────────┐
│                   客户端层 (Client)                   │
│                                                       │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│   │   PC 浏览器   │  │  手机浏览器   │  │  平板浏览器   │  │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │
│          └────────────┬───┴─────────────────┘         │
└───────────────────────┼───────────────────────────────┘
                        │ HTTP / WebSocket
┌───────────────────────┼───────────────────────────────┐
│                  前端应用层 (Frontend)                   │
│                       │                               │
│   Vue 3 + Vite + Element Plus + Pinia + Vue Router    │
│   ┌─────────┬─────────┬─────────┬─────────┐          │
│   │  认证模块  │  帖子模块  │  私信模块  │  管理模块  │          │
│   └─────────┴─────────┴─────────┴─────────┘          │
└───────────────────────┼───────────────────────────────┘
                        │ REST API (Axios) / WebSocket
┌───────────────────────┼───────────────────────────────┐
│                  后端应用层 (Backend)                    │
│                       │                               │
│   Spring Boot 3 + Spring Security + MyBatis-Plus      │
│   ┌─────────────────────────────────────────┐        │
│   │            Controller 层 (API)           │        │
│   ├─────────────────────────────────────────┤        │
│   │            Service 层 (业务逻辑)          │        │
│   ├─────────────────────────────────────────┤        │
│   │            Mapper 层 (数据访问)           │        │
│   └─────────────────────────────────────────┘        │
└───────────────────────┼───────────────────────────────┘
                        │ JDBC
┌───────────────────────┼───────────────────────────────┐
│                   数据层 (Data)                         │
│                                                       │
│   ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│   │  PostgreSQL   │  │  本地文件系统   │  │   Redis     │ │
│   │  (主数据库)    │  │  (图片存储)    │  │ (缓存/Token)│ │
│   └──────────────┘  └──────────────┘  └────────────┘ │
└───────────────────────────────────────────────────────┘
```

### 1.2 请求处理流程

```
浏览器请求
    │
    ▼
Nginx (反向代理，可选)
    │
    ├─ 静态资源 ──→ 直接返回前端打包产物
    │
    └─ /api/** ──→ Spring Boot
                       │
                       ├─ Filter: CORS 跨域处理
                       ├─ Filter: JWT 认证校验
                       ├─ Controller: 参数校验、路由分发
                       ├─ Service: 业务逻辑处理
                       ├─ Mapper: 数据库操作
                       └─ 返回 JSON 响应
```

---

## 二、模块划分

### 2.1 后端模块

后端按业务职责划分为以下模块，各模块以 Spring Bean 形式存在，通过依赖注入协作：

| 模块 | 包路径 | 职责 |
|------|--------|------|
| **认证模块 (Auth)** | `com.campus.auth` | 用户注册、登录、登出、Token 刷新、邮箱验证 |
| **用户模块 (User)** | `com.campus.user` | 用户信息管理、头像上传、个人资料编辑 |
| **板块模块 (Board)** | `com.campus.board` | 板块 CRUD、板块列表、排序管理 |
| **帖子模块 (Post)** | `com.campus.post` | 帖子发布/编辑/删除、帖子列表/详情、置顶/精华 |
| **评论模块 (Comment)** | `com.campus.comment` | 评论发表/删除、楼中楼回复、评论列表 |
| **互动模块 (Interaction)** | `com.campus.interaction` | 点赞/取消点赞、收藏/取消收藏 |
| **私信模块 (Message)** | `com.campus.message` | 私信收发、会话管理、WebSocket 实时通信 |
| **搜索模块 (Search)** | `com.campus.search` | 帖子关键词搜索、板块内搜索、用户帖子搜索 |
| **管理模块 (Admin)** | `com.campus.admin` | 用户管理、内容审核、数据统计 |
| **公共模块 (Common)** | `com.campus.common` | 统一响应封装、全局异常处理、工具类、常量 |

### 2.2 前端模块

前端按页面和功能组织为 Vue 组件：

| 模块 | 目录 | 职责 |
|------|------|------|
| **认证模块** | `views/auth/` | 登录、注册、忘记密码页面 |
| **首页模块** | `views/home/` | 首页帖子流、板块导航、公告展示 |
| **帖子模块** | `views/post/` | 帖子列表、帖子详情、发帖/编辑页 |
| **个人中心** | `views/profile/` | 个人信息、我的帖子、我的收藏 |
| **私信模块** | `views/message/` | 会话列表、聊天窗口（WebSocket） |
| **搜索模块** | `views/search/` | 搜索结果页 |
| **管理后台** | `views/admin/` | 用户管理、内容管理、数据统计 |
| **公共组件** | `components/` | 导航栏、帖子卡片、评论组件、分页器等 |
| **状态管理** | `stores/` | 用户状态、帖子缓存、未读消息计数 |
| **网络层** | `utils/` | Axios 封装（拦截器、Token 刷新）、WebSocket 管理 |

---

## 三、模块间关系与通信

### 3.1 模块依赖关系

```
                    ┌──────────┐
                    │  Common   │ ← 所有模块依赖
                    └────┬─────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐     ┌────▼────┐     ┌────▼────┐
   │  Auth   │     │  Board  │     │  Admin  │
   └────┬────┘     └────┬────┘     └────┬────┘
        │                │                │
   ┌────▼────┐     ┌────▼────┐          │
   │  User   │◄────│  Post   │◄─────────┘
   └────┬────┘     └────┬────┘
        │           ┌────┼────────┐
        │      ┌────▼──┐ │  ┌────▼────┐
        │      │Comment│ │  │  Search │
        │      └───────┘ │  └─────────┘
        │           ┌────▼────┐
        ├──────────►│Interaction│
        │           └─────────┘
   ┌────▼────┐
   │ Message │
   └─────────┘
```

**依赖说明：**
- **Auth → User**：注册时创建用户记录，登录时查询用户信息
- **Post → Board**：帖子归属于某个板块
- **Post → User**：帖子有作者信息
- **Comment → Post**：评论归属于某个帖子
- **Comment → Comment**：楼中楼回复引用父评论
- **Interaction → User / Post / Comment**：点赞/收藏关联用户与目标
- **Message → User**：私信关联发送者和接收者
- **Search → Post**：搜索基于帖子数据
- **Admin → User / Post / Board**：管理功能操作各业务表

### 3.2 后端内部通信

| 场景 | 通信方式 | 说明 |
|------|----------|------|
| Controller → Service | 方法调用（依赖注入） | 同步调用，标准 Spring Bean 协作 |
| Service → Mapper | 方法调用（MyBatis-Plus） | ORM 数据库操作 |
| 认证校验 | Spring Security Filter Chain | 请求进入 Controller 前完成认证 |
| 私信推送 | WebSocket (STOMP) | 服务端主动推送消息到客户端 |

### 3.3 前后端通信

| 场景 | 协议 | 说明 |
|------|------|------|
| 普通 API 请求 | HTTP REST (JSON) | 通过 Axios 发送，携带 JWT Access Token |
| Token 刷新 | HTTP REST | Access Token 过期时自动用 Refresh Token 换取新 Token |
| 私信实时通信 | WebSocket (STOMP over SockJS) | 建立长连接，实时收发私信 |
| 文件上传 | HTTP Multipart | 图片通过 FormData 上传 |

---

## 四、认证方案设计

### 4.1 双 Token 机制

```
┌──────────┐    登录请求     ┌──────────┐
│  前端     │──────────────►│  后端     │
│          │               │          │
│          │◄──────────────│          │
│          │  Access Token  │          │
│          │  Refresh Token │          │
└──────────┘               └──────────┘

┌──────────┐   携带 Access   ┌──────────┐
│  前端     │─── Token ──────►│  后端     │
│          │               │          │
│          │   正常响应      │          │
│          │◄──────────────│          │
└──────────┘               └──────────┘

┌──────────┐   Access Token  ┌──────────┐
│  前端     │─── 过期 401 ───►│  后端     │
│          │               │          │
│          │   携带 Refresh  │          │
│          │─── Token ──────►│          │
│          │               │          │
│          │◄──────────────│          │
│          │  新 Access Token│          │
└──────────┘               └──────────┘
```

| Token | 有效期 | 存储位置 | 用途 |
|-------|--------|----------|------|
| Access Token | 2 小时 | 前端内存（Pinia） | 请求 API 时的身份凭证 |
| Refresh Token | 7 天 | HttpOnly Cookie | Access Token 过期后静默刷新 |

### 4.2 Token 刷新流程

1. Axios 响应拦截器捕获 401 错误
2. 将失败请求加入队列，标记为"刷新中"
3. 使用 Refresh Token 请求 `/api/auth/refresh`
4. 刷新成功：更新 Access Token，重试队列中的请求
5. 刷新失败：清除状态，跳转登录页

### 4.3 权限模型

| 角色 | 权限范围 |
|------|----------|
| 未登录用户 | 浏览帖子列表、帖子详情（只读） |
| 普通学生 | 发帖、评论、点赞、收藏、私信、编辑/删除自己的内容 |
| 管理员 | 全部权限 + 用户管理、内容审核、板块管理、数据统计 |

---

## 五、WebSocket 通信设计

### 5.1 技术方案

使用 **STOMP over SockJS** 作为 WebSocket 通信协议：

- **SockJS**：提供降级方案（当浏览器不支持 WebSocket 时自动降级为长轮询）
- **STOMP**：提供消息帧格式，支持订阅/发布模式

### 5.2 通信模型

```
用户 A                    服务器                    用户 B
  │                         │                        │
  │── 连接 WebSocket ──────►│                        │
  │                         │◄── 连接 WebSocket ─────│
  │                         │                        │
  │── 发送私信 ────────────►│                        │
  │                         │── 推送给用户 B ────────►│
  │                         │                        │
  │                         │◄── 发送私信 ───────────│
  │◄── 推送给用户 A ────────│                        │
```

### 5.3 STOMP 端点设计

| 端点 | 路径 | 说明 |
|------|------|------|
| 连接端点 | `/ws` | WebSocket 握手端点 |
| 发送目标 | `/app/message/send` | 客户端发送私信 |
| 订阅目标 | `/user/queue/messages` | 用户订阅自己的消息队列（用户隔离） |

### 5.4 消息格式

```json
{
  "type": "PRIVATE_MESSAGE",
  "senderId": 1001,
  "receiverId": 1002,
  "content": "你好，这本书还在吗？",
  "timestamp": "2026-05-27T10:30:00Z"
}
```

---

## 六、数据流概览

### 6.1 用户注册流程

```
前端填写注册表单
    │
    ▼
POST /api/auth/register
    │
    ▼
AuthController
    │
    ├─ 参数校验（学号、邮箱格式、密码强度）
    ├─ 检查学号/邮箱是否已注册
    ├─ 发送邮箱验证码（异步）
    │
    ▼
用户收到验证码，前端提交验证码
    │
    ▼
POST /api/auth/register/verify
    │
    ├─ 校验验证码
    ├─ 密码 BCrypt 加密
    ├─ 写入 user 表
    └─ 返回注册成功
```

### 6.2 帖子发布流程

```
前端填写帖子内容（富文本 + 图片）
    │
    ▼
POST /api/posts (multipart/form-data)
    │
    ▼
PostController
    │
    ├─ JWT 认证校验
    ├─ 参数校验（标题、内容、板块ID）
    ├─ 图片保存到本地文件系统
    │
    ▼
PostService.createPost()
    │
    ├─ 构建 Post 实体
    ├─ 写入 post 表
    └─ 返回帖子详情
```

### 6.3 私信收发流程

```
用户 A 打开与用户 B 的聊天窗口
    │
    ▼
建立 WebSocket 连接
    │
    ▼
订阅 /user/queue/messages
    │
    ▼
用户 A 输入消息并发送
    │
    ▼
POST /app/message/send (WebSocket)
    │
    ▼
MessageController (WebSocket)
    │
    ├─ 校验发送者身份
    ├─ 写入 message 表
    ├─ 通过 SimpMessagingTemplate 推送给用户 B
    │
    ▼
用户 B 收到实时推送，界面更新
```

### 6.4 热度排序算法

帖子热度评分公式（用于"热门"排序）：

```
score = (点赞数 × 3 + 评论数 × 2 + 收藏数 × 1) / (小时龄 + 2)^1.5
```

- 新帖有初始权重加成，随时间衰减
- 高互动帖子排名靠前
- 置顶帖始终排在最前（不受热度影响）

---

## 七、技术选型及理由

### 7.1 前端技术栈

| 技术 | 版本 | 选择理由 |
|------|------|----------|
| Vue 3 | 3.4+ | 响应式框架，Composition API 逻辑复用能力强，社区活跃 |
| Vite | 5.x | 开发环境秒级启动，HMR 热更新快，构建产物小 |
| Element Plus | 2.x | Vue 3 生态最成熟的 UI 组件库，覆盖表单、表格、对话框等常用场景 |
| Pinia | 2.x | Vue 官方推荐状态管理，TypeScript 友好，支持模块化 |
| Vue Router | 4.x | Vue 官方路由，支持嵌套路由、路由守卫 |
| Axios | 1.x | 支持拦截器（Token 注入/刷新）、请求取消，社区成熟 |
| SockJS + @stomp/stompjs | — | WebSocket 客户端，SockJS 提供降级兼容，STOMP 提供消息协议 |

### 7.2 后端技术栈

| 技术 | 版本 | 选择理由 |
|------|------|----------|
| Spring Boot | 3.x | Java 主流框架，自动配置简化开发，生态完善 |
| Spring Security | 6.x | 成熟的安全框架，与 Spring Boot 无缝集成，支持 JWT 过滤链 |
| MyBatis-Plus | 3.5+ | 在 MyBatis 基础上增强，内置 CRUD、分页、条件构造器，减少样板代码 |
| PostgreSQL | 16+ | 开源关系型数据库，支持 JSONB、全文检索，性能优于 MySQL（复杂查询场景） |
| JWT (jjwt) | 0.12+ | 轻量级 Token 方案，无状态认证，适合前后端分离架构 |
| Spring WebSocket | — | Spring 原生 WebSocket 支持，STOMP 协议集成，与 Security 无缝协作 |
| Knife4j | 4.x | Swagger 增强 UI，自动生成接口文档，便于前后端联调 |
| BCrypt | — | Spring Security 内置，自适应哈希，抗暴力破解 |
| JavaMailSender | — | Spring 内置邮件发送，用于注册验证码和密码找回 |

### 7.3 开发工具

| 工具 | 用途 |
|------|------|
| Maven | 后端依赖管理和构建 |
| pnpm | 前端依赖管理（比 npm 更快，磁盘占用更小） |
| Git | 版本控制 |
| Postman / Knife4j | 接口调试和文档 |

---

## 八、项目结构

### 8.1 后端目录结构

```
campus-forum-backend/
├── pom.xml
├── src/main/java/com/campus/
│   ├── CampusForumApplication.java          # 启动类
│   ├── auth/                                # 认证模块
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── dto/                             # 注册/登录请求/响应 DTO
│   │   └── security/                        # JWT 过滤器、Token 工具类
│   ├── user/                                # 用户模块
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── mapper/UserMapper.java
│   │   ├── entity/User.java
│   │   └── dto/
│   ├── board/                               # 板块模块
│   │   ├── controller/BoardController.java
│   │   ├── service/BoardService.java
│   │   ├── mapper/BoardMapper.java
│   │   └── entity/Board.java
│   ├── post/                                # 帖子模块
│   │   ├── controller/PostController.java
│   │   ├── service/PostService.java
│   │   ├── mapper/PostMapper.java
│   │   ├── entity/Post.java
│   │   └── dto/
│   ├── comment/                             # 评论模块
│   │   ├── controller/CommentController.java
│   │   ├── service/CommentService.java
│   │   ├── mapper/CommentMapper.java
│   │   └── entity/Comment.java
│   ├── interaction/                         # 互动模块
│   │   ├── controller/InteractionController.java
│   │   ├── service/LikeService.java
│   │   ├── service/FavoriteService.java
│   │   ├── mapper/
│   │   └── entity/
│   ├── message/                             # 私信模块
│   │   ├── controller/MessageController.java      # REST API
│   │   ├── controller/WebSocketController.java    # WebSocket 端点
│   │   ├── service/MessageService.java
│   │   ├── mapper/MessageMapper.java
│   │   └── entity/Message.java
│   ├── search/                              # 搜索模块
│   │   ├── controller/SearchController.java
│   │   └── service/SearchService.java
│   ├── admin/                               # 管理模块
│   │   ├── controller/AdminController.java
│   │   └── service/AdminService.java
│   └── common/                              # 公共模块
│       ├── config/                          # 全局配置（CORS、Security、WebSocket、MyBatis）
│       ├── response/                        # 统一响应体 R<T>
│       ├── exception/                       # 全局异常处理器、自定义业务异常
│       ├── enums/                           # 枚举（状态码、角色等）
│       └── utils/                           # 工具类
├── src/main/resources/
│   ├── application.yml                      # 主配置文件
│   ├── application-dev.yml                  # 开发环境配置
│   └── mapper/                              # MyBatis XML 映射文件（复杂查询用）
└── src/test/                                # 单元测试
```

### 8.2 前端目录结构

```
campus-forum-frontend/
├── package.json
├── vite.config.js
├── src/
│   ├── main.js                              # 入口文件
│   ├── App.vue                              # 根组件
│   ├── router/                              # 路由配置
│   │   └── index.js
│   ├── stores/                              # Pinia 状态管理
│   │   ├── user.js                          # 用户状态（登录信息、Token）
│   │   ├── post.js                          # 帖子相关状态
│   │   └── message.js                       # 私信状态（未读数）
│   ├── views/                               # 页面组件
│   │   ├── auth/
│   │   │   ├── Login.vue
│   │   │   └── Register.vue
│   │   ├── home/
│   │   │   └── Home.vue
│   │   ├── post/
│   │   │   ├── PostList.vue
│   │   │   ├── PostDetail.vue
│   │   │   └── PostCreate.vue
│   │   ├── profile/
│   │   │   └── Profile.vue
│   │   ├── message/
│   │   │   └── Message.vue
│   │   ├── search/
│   │   │   └── SearchResult.vue
│   │   └── admin/
│   │       ├── UserManage.vue
│   │       ├── ContentAudit.vue
│   │       └── Dashboard.vue
│   ├── components/                          # 公共组件
│   │   ├── layout/
│   │   │   ├── Navbar.vue                   # 顶部导航栏
│   │   │   ├── Sidebar.vue                  # 侧边栏（板块列表）
│   │   │   └── Footer.vue
│   │   ├── PostCard.vue                     # 帖子卡片
│   │   ├── CommentList.vue                  # 评论列表
│   │   ├── RichEditor.vue                   # 富文本编辑器
│   │   └── Pagination.vue                   # 分页器
│   ├── utils/                               # 工具函数
│   │   ├── request.js                       # Axios 封装（拦截器 + Token 刷新）
│   │   ├── websocket.js                     # WebSocket 连接管理
│   │   └── auth.js                          # Token 存取工具
│   ├── api/                                 # API 接口定义
│   │   ├── auth.js
│   │   ├── user.js
│   │   ├── post.js
│   │   ├── comment.js
│   │   ├── message.js
│   │   └── admin.js
│   └── assets/                              # 静态资源
│       └── styles/
│           └── global.scss
├── public/
└── index.html
```

---

## 九、安全设计

### 9.1 认证安全

| 措施 | 实现方式 |
|------|----------|
| 密码加密 | BCrypt（Spring Security 默认），每次生成不同盐值 |
| Token 安全 | Access Token 短有效期（2h），Refresh Token 存 HttpOnly Cookie 防 XSS |
| 登录防暴力 | 连续 5 次密码错误锁定账号 15 分钟 |
| 邮箱验证 | 验证码 5 分钟过期，一次性使用 |

### 9.2 接口安全

| 措施 | 实现方式 |
|------|----------|
| CORS | 后端配置 `CorsFilter`，仅允许前端域名 |
| XSS 防护 | 前端输入转义，后端富文本内容过滤（Jsoup） |
| SQL 注入 | MyBatis-Plus 参数化查询，禁止拼接 SQL |
| 权限校验 | Spring Security `@PreAuthorize` 注解，方法级权限控制 |
| 限流防刷 | 基于 IP 的接口限流（可用 Guava RateLimiter 或 Redis） |

### 9.3 数据安全

| 措施 | 实现方式 |
|------|----------|
| 敏感数据 | 密码不可逆加密，手机号/邮箱脱敏展示 |
| 软删除 | 帖子、评论采用软删除（标记状态），不物理删除 |
| 操作日志 | 管理员操作记录审计日志 |

---

## 十、数据库索引策略

| 表 | 索引 | 说明 |
|----|------|------|
| `user` | `uk_student_no` (学号) | 注册/登录查询 |
| `user` | `uk_email` (邮箱) | 注册/登录查询 |
| `post` | `idx_board_id` (板块ID) | 板块帖子列表 |
| `post` | `idx_author_id` (作者ID) | 用户帖子列表 |
| `post` | `idx_created_at` (创建时间) | 时间排序 |
| `post` | `idx_board_pinned` (板块ID + 置顶) | 置顶帖查询 |
| `comment` | `idx_post_id` (帖子ID) | 帖子评论列表 |
| `comment` | `idx_parent_id` (父评论ID) | 楼中楼回复 |
| `like` | `uk_user_target` (用户ID + 目标类型 + 目标ID) | 唯一约束，防重复点赞 |
| `favorite` | `uk_user_post` (用户ID + 帖子ID) | 唯一约束，防重复收藏 |
| `message` | `idx_sender_receiver` (发送者 + 接收者) | 会话查询 |
| `message` | `idx_receiver_read` (接收者 + 已读状态) | 未读消息查询 |

---

*文档结束*
