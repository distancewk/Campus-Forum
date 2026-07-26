# 测试流程（Testing）

本文档说明 Campus Forum 的测试分层、本地运行方式、覆盖率与 CI。

## 测试金字塔

| 层级 | 技术 | 是否需要外部依赖 | 运行速度 |
| --- | --- | --- | --- |
| 单元测试（主导） | JUnit 5 + Mockito | 无（hermetic） | 快 |
| 组件/接口测试 | `@WebMvcTest` + MockMvc | 无（Redis 等已 mock） | 快 |
| 集成测试 | （预留，见下方“已知缺口”） | 需要 Postgres + Redis | 慢 |

当前后端共 **31 个测试类 / 92 个用例**，全部 hermetic：纯 Mockito 单元 + 一个 `@WebMvcTest` 安全测试（已 mock 掉 Redis 依赖）。
因此 `mvn test` 可在**任何环境**直接运行，无需启动数据库或缓存。

## 本地运行

### 后端（推荐）

```bash
bash scripts/test-backend.sh      # 等价于 cd campus-forum-backend && mvn test
# 或仅运行某个类
cd campus-forum-backend && mvn test -Dtest=AsyncModerationServiceTest
```

覆盖率报告生成在 `campus-forum-backend/target/site/jacoco/index.html`，浏览器打开即可查看行/分支覆盖。

### 前端

```bash
bash scripts/test-frontend.sh      # npm install + lint + test + build
# 仅跑单测
cd campus-forum-frontend-new && npm test
```

### 一站式

```bash
bash scripts/test.sh               # 后端 + 前端
```

## 如何新增测试

- **纯逻辑/服务**：直接用 Mockito（`@ExtendWith(MockitoExtension.class)`），不要依赖 Spring 上下文。
- **需要 Spring Security 的 Web 层**：用 `@WebMvcTest(Controller.class)` + `@Import(SecurityConfig.class)`；
  凡依赖 Redis 的 Bean（`RateLimitInterceptor`、`RefreshTokenStore` 等）用 `@MockBean` 注入，保持上下文 hermetic。
- **避免 `LambdaUpdateWrapper`/`LambdaQueryWrapper` 在脱离 Spring 的单元件测试中**：MyBatis-Plus 需要实体 lambda
  缓存，单测中不可用。涉及更新/删除请改用 Mapper 上的显式 `@Update` 方法（参考 `BoardMapper.logicalDeleteById`）。
- 测试命名：`*Test.java` 由 Surefire 运行；集成测试约定 `*IT.java` / `*IntegrationTest.java`，由 Failsafe 运行（当前未启用）。

## CI（GitHub Actions）

配置文件：`.github/workflows/ci.yml`，在**每次推送与 PR**时触发，含两个并行 Job：

1. **backend**：JDK 17（Temurin）+ Maven 缓存 → `mvn -B test`（含 JaCoCo 覆盖率）→
   上传 `backend-jacoco` 产物。
2. **frontend**：Node 22 + npm 缓存 → `npm install` → `lint` → `typecheck`（非阻塞）→ `test` → `build` →
   上传 `frontend-dist` 产物。

使用了 `concurrency`（同分支取消旧运行）与最小权限 `permissions: contents: read`。
前端使用 `npm install` 而非 `npm ci`，因为当前 `package-lock.json` 与 `package.json` 未完全同步（见下）。

## 已知问题 / 待办

### 1. 前端 `typecheck` 当前 broken（`continue-on-error`）

`package.json` 中 `typescript@7.0.2` 与 `vue-tsc@2.2.12` 不兼容，导致 `npm run typecheck` 报
`ERR_PACKAGE_PATH_NOT_EXPORTED: ./lib/tsc`。CI 中已设为非阻塞，不会让流水线变红，但类型检查实际未生效。
**修复方向**：将 `vue-tsc` 升级到支持 TypeScript 7 的版本（或把 `typescript` 降到与 `vue-tsc@2.2.12` 兼容的版本），
对齐后再去掉 CI 里的 `continue-on-error`。

### 2. `package-lock.json` 与 `package.json` 不同步

`npm ci` 会直接报错（缺多个 devDependency）。当前 CI 与本地脚本使用 `npm install` 规避。
**修复方向**：在依赖对齐后执行一次 `npm install` 重新生成 lockfile，之后即可切回 `npm ci`（更可复现）。

### 3. 集成测试缺口（无全栈集成测试）

目前没有启动真实 Postgres + Redis 的集成测试。原因是 Flyway 迁移 **V4 依赖 `pgvector` 扩展**
（`CREATE EXTENSION IF NOT EXISTS vector`），普通 `postgres:16` 镜像没有该扩展，会导致应用上下文启动失败。

**后续接入方式**（已在 CI 预留结构）：新增 `*IT.java`（`@SpringBootTest(webEnvironment = RANDOM_PORT)`），
使用 Testcontainers 拉起 `pgvector/pgvector:pg16`（或 CI service 用同名镜像）+ `redis:7`，通过
`@DynamicPropertySource` 注入连接串，并在 `pom.xml` 增加 `maven-failsafe-plugin` 的 `integration` profile
（Surefire 已排除 `*IT`）。这样默认 `mvn test` 仍快且 hermetic，集成测试仅在 `mvn verify -Pintegration` 时运行。

## 提交与代码规范

- 提交信息遵循 Conventional Commits（见 `.commitlintrc.json`，CI 会校验 `type`）。
- 后端改动需保证 `mvn test` 通过；前端需保证 `npm run lint && npm test` 通过（typecheck 待修复）。
- 详见 `CONTRIBUTING.md`。
