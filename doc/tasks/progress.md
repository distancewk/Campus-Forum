# 校园论坛 — 总体进度

> 最后更新：2026-07-26
> 当前可发布前端项目为 **campus-forum-frontend-new**（旧 `campus-forum-frontend` 已于 Phase 0 删除）。

---

## 阶段性完成状态

- [x] **Phase 0（P0 ×3）** — 已完成 2026-07-26：板块软删除 SQL bug、删除废弃旧前端、补充 .gitignore。
- [x] **Phase 1（P1 ×11）** — 已完成 2026-07-26：评论计数、登录锁定竞态、管理员密码改由环境变量注入、formatTime 公共函数、WebSocket 改用 Pinia、后端测试、前端 Vitest、CI、Docker、.env.example、部署文档。
- [x] **Phase 2（P2 ×11）** — 已完成 2026-07-26：N+1 修复、DOMPurify 白名单统一、邮箱域名配置化、CORS 收紧、ESLint/Prettier、渐进式 TypeScript、Playwright E2E、版本管理规范、updated_at 触发器、文档同步。

---

## 后端模块

- [x] [公共模块 (Common)](common.md) — 10 个任务
- [x] [认证模块 (Auth)](auth.md) — 7 个任务
- [x] [用户模块 (User)](user.md) — 4 个任务
- [x] [板块模块 (Board)](board.md) — 4 个任务（含逻辑删除修复）
- [x] [帖子模块 (Post)](post.md) — 5 个任务
- [x] [评论模块 (Comment)](comment.md) — 4 个任务（含计数修复）
- [x] [互动模块 (Interaction)](interaction.md) — 4 个任务
- [x] [私信模块 (Message)](message.md) — 6 个任务（含 N+1 修复）
- [x] [搜索模块 (Search)](search.md) — 4 个任务
- [x] [管理模块 (Admin)](admin.md) — 4 个任务

## 前端模块（campus-forum-frontend-new）

- [x] [公共组件与布局 (Layout)](layout.md) — App.vue / components / router（9 个任务）
- [x] [认证页面 (Auth)](fe-auth.md) — Login / Register / ForgotPassword（4 个任务）
- [x] [首页 (Home)](fe-home.md) — Home.vue（3 个任务）
- [x] [帖子模块 (Post)](fe-post.md) — PostList / PostCreate / PostDetail（5 个任务）
- [x] [个人中心 (Profile)](fe-profile.md) — Profile.vue（5 个任务）
- [x] [私信模块 (Message)](fe-message.md) — Message.vue（Pinia 驱动，5 个任务）
- [x] [搜索模块 (Search)](fe-search.md) — SearchResult.vue（3 个任务）
- [x] [管理后台 (Admin)](fe-admin.md) — Dashboard / UserManage / ContentAudit / AiKnowledge（5 个任务）
- [x] [AI 模块 (AI)](fe-ai.md) — AiAsk.vue + api/ai.js + adminAi.js（新增）

> 实际前端含 **16 个 Vue 页面** 与 **10 个 API 模块**（auth / user / post / board / interaction / message / search / admin / ai / adminAi）。

## E2E 测试

- [x] [E2E 测试](e2e.md) — Playwright 核心链路（登录→发帖→评论→私信→搜索）

---

## 模块依赖关系

```
后端：
common → auth → user ─────────────────────┐
common → board ───────────────────────────┤
common → post (依赖 user, board) ─────────┤
common → comment (依赖 user, post) ───────┤
common → interaction (依赖 user, post, comment) ──→ 全部后端完成
common → message (依赖 user) ─────────────┤
common → search (依赖 post) ──────────────┤
common → admin (依赖 user, post, board) ──┘

前端：
layout → fe-auth ──┐
layout → fe-home ──┤
layout → fe-post ──┤
layout → fe-profile ──→ 全部前端完成（含 AI 模块）
layout → fe-message ──┤
layout → fe-search ───┤
layout → fe-admin ────┘

全部后端 + 全部前端 → e2e
```

## 建议开发顺序

### 第一阶段（基础框架 + 核心功能）
1. common 模块
2. auth 模块
3. user 模块
4. board 模块
5. post 模块
6. layout 模块（前端）
7. fe-auth 模块
8. fe-home 模块
9. fe-post 模块

### 第二阶段（交互功能）
10. comment 模块
11. interaction 模块
12. search 模块
13. fe-profile 模块
14. fe-search 模块

### 第三阶段（实时通信 + 管理）
15. message 模块
16. admin 模块
17. fe-message 模块
18. fe-admin 模块
19. AI 模块（AiAsk / 知识库）

### 第四阶段（测试）
20. E2E 测试（Playwright）
