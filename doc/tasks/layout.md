# 前端 — 公共组件与布局 (Layout) — 任务清单

> 前置依赖：无（本模块是其他前端模块的基础）

---

## 任务 1：项目初始化

- [ ] 1.1 使用 Vite 创建 Vue 3 项目（`npm create vite@latest campus-forum-frontend -- --template vue`）
- [ ] 1.2 安装依赖：vue-router、pinia、axios、element-plus、@element-plus/icons-vue、@wangeditor/editor、@wangeditor/editor-for-vue、sockjs-client、@stomp/stompjs、sass
- [ ] 1.3 配置 `vite.config.js`（代理 /api → localhost:8080，代理 /ws → localhost:8080）
- [ ] 1.4 配置 Element Plus 全局引入 + 中文化
- [ ] 1.5 创建目录结构（views/、components/、stores/、utils/、api/、assets/styles/）
- [ ] 1.6 验证：`pnpm dev` 启动成功

---

## 任务 2：全局样式与 SCSS

- [ ] 2.1 创建 `assets/styles/global.scss`（reset、字体、颜色变量）
- [ ] 2.2 定义响应式断点 mixin（mobile < 768px、tablet 768-1024px、desktop > 1024px）
- [ ] 2.3 在 `main.js` 中引入全局样式

---

## 任务 3：Axios 封装

- [ ] 3.1 创建 `utils/request.js`（axios.create，baseURL: '/api'，timeout: 10000）
- [ ] 3.2 请求拦截器：从 userStore 获取 accessToken，注入 Authorization header
- [ ] 3.3 响应拦截器：
  - [ ] 业务错误（code !== 0）：ElMessage.error(message)
  - [ ] 401 错误：Token 刷新队列机制（isRefreshing 标记 + failedQueue 队列）
  - [ ] 刷新成功：更新 Token，重试队列中的请求
  - [ ] 刷新失败：清除状态，跳转登录页
- [ ] 3.4 创建 `utils/auth.js`（getToken / setToken / removeToken 工具函数）

---

## 任务 4：Pinia 状态管理

- [ ] 4.1 创建 `stores/user.js`：
  - [ ] state: accessToken、user
  - [ ] getters: isLoggedIn
  - [ ] actions: login、refreshAccessToken、logout
  - [ ] 持久化：localStorage 存取
- [ ] 4.2 创建 `stores/message.js`：
  - [ ] state: unreadCount
  - [ ] actions: fetchUnreadCount、incrementUnread、clearUnread

---

## 任务 5：API 接口定义

- [ ] 5.1 创建 `api/auth.js`（sendCode、register、verifyRegister、login、refreshToken、logout、forgotPassword、resetPassword）
- [ ] 5.2 创建 `api/user.js`（getCurrentUser、updateProfile、uploadAvatar、getUserById）
- [ ] 5.3 创建 `api/board.js`（getBoards）
- [ ] 5.4 创建 `api/post.js`（getPosts、getPostDetail、createPost、updatePost、deletePost）
- [ ] 5.5 创建 `api/comment.js`（getComments、createComment、deleteComment）
- [ ] 5.6 创建 `api/interaction.js`（toggleLike、toggleFavorite、getMyFavorites）
- [ ] 5.7 创建 `api/message.js`（getConversations、getChatHistory、getUnreadCount、markAsRead）
- [ ] 5.8 创建 `api/admin.js`（getDashboard、getUsers、updateUserStatus、getPendingPosts、auditPost、togglePin、toggleFeature、adminDeletePost）
- [ ] 5.9 创建 `api/search.js`（search）

---

## 任务 6：路由配置

- [ ] 6.1 创建 `router/index.js`（createRouter + createWebHistory）
- [ ] 6.2 配置所有路由（懒加载 import）
- [ ] 6.3 配置路由守卫：
  - [ ] requiresAuth → 检查 isLoggedIn，否则跳转 /login?redirect=xxx
  - [ ] requiresAdmin → 检查 role === 'ADMIN'，否则跳转首页
- [ ] 6.4 路由切换时更新页面标题

---

## 任务 7：WebSocket 管理

- [ ] 7.1 创建 `utils/websocket.js`：
  - [ ] connectWebSocket(token, onMessage) — 创建 STOMP over SockJS 连接
  - [ ] 订阅 /user/queue/messages
  - [ ] 自动重连（reconnectDelay: 5000）
  - [ ] sendMessage(receiverId, content) — 发送到 /app/message/send
  - [ ] disconnectWebSocket() — 断开连接
- [ ] 7.2 在 userStore.login 成功后连接 WebSocket
- [ ] 7.3 在 userStore.logout 时断开 WebSocket

---

## 任务 8：布局组件

- [ ] 8.1 创建 `components/layout/MainLayout.vue`：
  - [ ] 顶部导航栏（Navbar）
  - [ ] 主内容区（router-view）
  - [ ] 响应式布局（PC 端 sidebar + content，移动端单栏）
- [ ] 8.2 创建 `components/layout/Navbar.vue`：
  - [ ] 左侧：Logo + 导航链接（首页、板块下拉菜单）
  - [ ] 中间：搜索框（回车跳转 /search?keyword=xxx）
  - [ ] 右侧：未读消息角标（连接 messageStore）、用户头像下拉菜单（个人中心、管理后台入口、登出）
  - [ ] 移动端：汉堡菜单折叠
- [ ] 8.3 创建 `components/layout/Sidebar.vue`：
  - [ ] 板块列表（从 API 获取）
  - [ ] 当前板块高亮
  - [ ] 移动端隐藏
- [ ] 8.4 创建 `components/layout/AdminLayout.vue`：
  - [ ] 管理后台侧边导航（数据概览、用户管理、内容审核）
  - [ ] 管理后台顶栏（返回前台、登出）

---

## 任务 9：公共组件

- [ ] 9.1 创建 `components/PostCard.vue`：
  - [ ] 展示：置顶/精华标签、标题、摘要、作者头像/昵称、板块名、时间、浏览/评论/点赞数
  - [ ] 点击跳转帖子详情
- [ ] 9.2 创建 `components/RichEditor.vue`：
  - [ ] 集成 wangEditor（Toolbar + Editor）
  - [ ] 配置图片上传（/api/posts/upload-image，返回 { url } 格式）
  - [ ] v-model 双向绑定内容
  - [ ] 组件销毁时销毁编辑器实例
- [ ] 9.3 创建 `components/CommentList.vue`：
  - [ ] 评论树形渲染（顶层 + 一级回复）
  - [ ] 回复输入框
  - [ ] 点赞/回复操作
  - [ ] 删除按钮（仅作者可见）
- [ ] 9.4 创建 `components/Pagination.vue`（或直接使用 el-pagination）

---

## 依赖关系

- 任务 1 → 任务 2、3、4、5 可并行
- 任务 6 依赖任务 5
- 任务 7 依赖任务 4
- 任务 8 依赖任务 3、6
- 任务 9 依赖任务 3、5
- **本模块全部完成后，其他前端页面模块才可开始**
