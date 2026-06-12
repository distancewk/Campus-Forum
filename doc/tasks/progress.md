# 校园论坛 — 总体进度

> 最后更新：2026-05-29

---

## 后端模块

- [x] [公共模块 (Common)](common.md) — 10 个任务
- [x] [认证模块 (Auth)](auth.md) — 7 个任务
- [x] [用户模块 (User)](user.md) — 4 个任务
- [x] [板块模块 (Board)](board.md) — 4 个任务
- [x] [帖子模块 (Post)](post.md) — 5 个任务
- [x] [评论模块 (Comment)](comment.md) — 4 个任务
- [x] [互动模块 (Interaction)](interaction.md) — 4 个任务
- [x] [私信模块 (Message)](message.md) — 6 个任务
- [x] [搜索模块 (Search)](search.md) — 4 个任务
- [x] [管理模块 (Admin)](admin.md) — 4 个任务

## 前端模块

- [ ] [公共组件与布局 (Layout)](layout.md) — 9 个任务
- [ ] [认证页面 (Auth)](fe-auth.md) — 4 个任务
- [ ] [首页 (Home)](fe-home.md) — 3 个任务
- [ ] [帖子模块 (Post)](fe-post.md) — 5 个任务
- [ ] [个人中心 (Profile)](fe-profile.md) — 5 个任务
- [ ] [私信模块 (Message)](fe-message.md) — 5 个任务
- [ ] [搜索模块 (Search)](fe-search.md) — 3 个任务
- [ ] [管理后台 (Admin)](fe-admin.md) — 5 个任务

## E2E 测试

- [ ] [E2E 测试](e2e.md) — 10 个任务

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
layout → fe-profile ──→ 全部前端完成
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

### 第四阶段（测试）
19. E2E 测试
