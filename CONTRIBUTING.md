# 贡献指南（Contributing）

本项目（校园论坛 Campus Forum）采用 Spring Boot 后端 + Vue 3 前端。请按以下约定协作，以保证多人开发时的可维护性与发布可控性。

## 1. 分支策略

- `main`：**受保护且可发布**的分支，禁止直接推送，只能通过 PR 合入。
- 功能开发：从 `main` 切出 `feature/<short-desc>`（如 `feature/ai-knowledge`）。
- 缺陷修复：从 `main` 切出 `fix/<short-desc>`（如 `fix/login-race`）。
- 完成后向 `main` 发起 **Pull Request**，至少 **1 名 reviewer 批准** 后方可合并。
- 合并后及时删除已合入的特性分支。

```bash
git checkout main
git pull
git checkout -b feature/your-desc
# ... 开发、提交 ...
git push -u origin feature/your-desc
# 在平台/GitHub 上发起 PR 到 main
```

## 2. 提交信息规范（Conventional Commits）

提交信息使用 **Conventional Commits**，格式：

```
<type>: <subject>
```

- `type` 取值：`feat` / `fix` / `docs` / `refactor` / `test` / `chore`
- `subject` 简短描述（中文或英文均可，建议 ≤ 50 字），结尾不加句号。
- 必要时可在空一行后追加正文说明「为什么」。

示例：

```
feat: 新增 AI 知识库问答页面
fix: 修复登录锁定计数竞态条件
docs: 补充部署文档与 .env.example
chore: 升级前端依赖并格式化
```

> 仓库已配置 `.commitlintrc.json`，CI 会校验 `type` 合法性，非法提交将被拒绝。

## 3. 版本号规范（Semantic Versioning）

采用 **语义化版本** `MAJOR.MINOR.PATCH`：

- **MAJOR**：不兼容的接口/行为变更（破坏性升级）。
- **MINOR**：向后兼容的新功能（新增板块、新增 AI 模块等）。
- **PATCH**：向后兼容的缺陷修复（P0/P1 修复等）。

## 4. 发布流程

在达到有意义的里程碑（如一轮 P0/P1/P2 修复完成、重大功能上线）时发版：

1. 在对应清单文件（如后端 `pom.xml`、前端 `package.json`）中 **bump 版本号**。
2. 在 `CHANGELOG.md` 的 `Unreleased` 内容定稿为新版本条目。
3. 打带注释的标签：

   ```bash
   git tag -a v1.1.0 -m "feat: AI 问答与知识库；P2 质量提升"
   git push origin --tags
   ```

4. 在 `main` 上合并并触发 CI/CD（Docker 镜像、部署）。

## 5. 其他约定

- 后端改动需保证 `mvn test` 通过；前端需保证 `pnpm lint && pnpm test` 通过。
- 密钥、密码、Token 一律走环境变量（见 `.env.example`），**禁止硬编码进源码或迁移文件**。
- 文档随代码同步更新（参见 `doc/optimization-plan.md` 与 `doc/tasks/progress.md`）。
