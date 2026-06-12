# E2E 测试 — 任务清单

> 前置依赖：所有前端模块和后端模块完成

---

## 任务 1：Playwright 环境搭建

- [ ] 1.1 安装 Playwright（`pnpm add -D @playwright/test`）
- [ ] 1.2 创建 `playwright.config.js`：
  - [ ] baseURL: 'http://localhost:5173'
  - [ ] 配置浏览器（chromium、firefox、webkit）
  - [ ] 配置超时、重试、截图策略
  - [ ] 配置 webServer（自动启动 dev server）
- [ ] 1.3 创建 `tests/` 目录结构
- [ ] 1.4 创建测试辅助函数：
  - [ ] `helpers/auth.js` — 登录/注册辅助（快速创建测试用户）
  - [ ] `helpers/api.js` — 直接调用 API 准备测试数据

---

## 任务 2：认证流程 E2E

- [ ] 2.1 `tests/auth/register.spec.js`：
  - [ ] 完整注册流程（发码 → 输入验证码 → 注册成功）
  - [ ] 注册失败场景（学号已存在、邮箱已存在、验证码错误）
- [ ] 2.2 `tests/auth/login.spec.js`：
  - [ ] 正常登录 → 跳转首页
  - [ ] 密码错误提示
  - [ ] 账号禁用提示
  - [ ] 登出后跳转登录页
- [ ] 2.3 `tests/auth/reset-password.spec.js`：
  - [ ] 忘记密码完整流程

---

## 任务 3：帖子流程 E2E

- [ ] 3.1 `tests/post/create.spec.js`：
  - [ ] 登录 → 选板块 → 填标题 → 编辑富文本 → 发布 → 跳转详情页
  - [ ] 未登录发帖跳转登录页
- [ ] 3.2 `tests/post/detail.spec.js`：
  - [ ] 帖子详情完整展示
  - [ ] 浏览量递增验证
- [ ] 3.3 `tests/post/edit-delete.spec.js`：
  - [ ] 作者编辑帖子
  - [ ] 作者删除帖子（确认弹窗）
  - [ ] 非作者无法看到编辑/删除按钮

---

## 任务 4：互动流程 E2E

- [ ] 4.1 `tests/interaction/like.spec.js`：
  - [ ] 点赞帖子 → 计数 +1，按钮高亮
  - [ ] 取消点赞 → 计数 -1，按钮恢复
  - [ ] 点赞评论
- [ ] 4.2 `tests/interaction/favorite.spec.js`：
  - [ ] 收藏帖子 → 计数 +1
  - [ ] 取消收藏 → 计数 -1
  - [ ] 我的收藏列表展示

---

## 任务 5：评论流程 E2E

- [ ] 5.1 `tests/comment/comment.spec.js`：
  - [ ] 发表评论 → 评论出现在列表中
  - [ ] 回复评论（楼中楼）
  - [ ] 删除自己的评论
  - [ ] 非作者无法删除他人评论
  - [ ] 评论后帖子评论数更新

---

## 任务 6：私信流程 E2E

- [ ] 6.1 `tests/message/message.spec.js`：
  - [ ] 打开私信页面 → 会话列表展示
  - [ ] 点击会话 → 聊天记录加载
  - [ ] 发送消息 → 消息出现在聊天窗口
  - [ ] 未读消息数更新
- [ ] 6.2 使用两个浏览器上下文模拟双人实时通信

---

## 任务 7：搜索流程 E2E

- [ ] 7.1 `tests/search/search.spec.js`：
  - [ ] Navbar 搜索框输入关键词 → 回车 → 跳转搜索结果页
  - [ ] 搜索结果展示匹配帖子
  - [ ] 板块内搜索筛选

---

## 任务 8：管理后台 E2E

- [ ] 8.1 `tests/admin/dashboard.spec.js`：
  - [ ] 管理员登录 → 访问管理后台 → 数据概览展示
- [ ] 8.2 `tests/admin/user-manage.spec.js`：
  - [ ] 用户列表展示
  - [ ] 禁用/启用用户
- [ ] 8.3 `tests/admin/content-audit.spec.js`：
  - [ ] 待审核帖子列表
  - [ ] 通过/拒绝审核
  - [ ] 置顶/精华切换
  - [ ] 板块管理增删改
- [ ] 8.4 `tests/admin/access-control.spec.js`：
  - [ ] 普通用户访问管理后台 → 跳转首页
  - [ ] 未登录访问管理后台 → 跳转登录页

---

## 任务 9：响应式 E2E

- [ ] 9.1 `tests/responsive/responsive.spec.js`：
  - [ ] PC 布局（1280px）→ 侧边栏可见
  - [ ] 平板布局（768px）→ 侧边栏窄版
  - [ ] 手机布局（375px）→ 侧边栏隐藏，汉堡菜单
  - [ ] 私信页面手机端左右面板切换

---

## 任务 10：CI 集成（可选）

- [ ] 10.1 配置 `package.json` scripts：`"test:e2e": "playwright test"`
- [ ] 10.2 配置 GitHub Actions 或其他 CI 运行 E2E 测试
- [ ] 10.3 生成测试报告（HTML Reporter）

---

## 依赖关系

- 任务 1 → 任务 2-9（环境搭建后各测试文件可并行编写）
- 任务 10 依赖所有测试完成
