# Campus Forum 优化方案

> Status: Phase 0 ✅ / Phase 1 ✅ / Phase 2 ✅ (completed 2026-07-26)

> 基于第一性原理缺陷分析（共 25 个缺陷：P0×3、P1×11、P2×11）制定。
> 目标：在不推翻现有架构的前提下，按优先级补齐正确性、安全性、可维护性、可测试性、可部署性、数据一致性六个维度。

---

## 一、实施路线图

| 阶段 | 目标 | 内容 | 预估工时 |
|------|------|------|----------|
| **Phase 0** | 立即止血 | 3 个 P0（SQL bug、旧前端、.gitignore） | 0.5 天 |
| **Phase 1** | 核心修复 | 11 个 P1（计数逻辑、竞态、密码、重复代码、测试、CI/CD、Docker） | 3 天 |
| **Phase 2** | 质量提升 | 11 个 P2（N+1、XSS 白名单、配置化、规范工具、文档同步等） | 2.5 天 |

原则：**先修会让功能不可用或泄露数据的（P0/P1 安全类），再补工程化与文档**。

---

## 二、Phase 0 — P0 修复

### 2.1 修复 `selectBoardStats` 引用不存在的 `b.deleted` 列（P0）

根因：`board` 表缺少 `deleted` 列，但 `PostMapper.xml:215` 写了 `WHERE b.deleted = 0`，且 `Board.java` 无 `@TableLogic`。最彻底的修复是**补齐 board 的逻辑删除，与其他表保持一致**（顺带解决 P2 中 "board 表无逻辑删除" 问题）。

**① 新增 Flyway 迁移 `V5__board_soft_delete.sql`：**
```sql
-- 为 board 补齐逻辑删除列，与其他核心表保持一致
ALTER TABLE board ADD COLUMN deleted SMALLINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uk_board_name_active ON board(name) WHERE deleted = 0;

-- 管理员初始密码从迁移文件移除（见 3.3 改为运行时从环境变量注入）
-- 此处仅将历史硬编码哈希置为占位符，真实密码由 AdminInitializer 在启动时覆盖
UPDATE "user" SET password = '$2a$10$CHANGE_ME_ADMIN_PASSWORD_ROTATED_AT_DEPLOY'
WHERE student_no = 'admin001';
```

**② `Board.java` 增加 `deleted` 字段并启用逻辑删除：**
```java
import com.baomidou.mybatisplus.annotation.TableLogic;

@Data
@TableName("board")
public class Board {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    /** 1=启用 0=禁用 */
    private Integer status;
    /** 0=未删除 1=已删除（逻辑删除） */
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

**③ `BoardService.java:110` 删除改为逻辑删除：**
```java
// 原：boardMapper.deleteById(id);
// 改为：
LambdaUpdateWrapper<Board> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(Board::getId, id).set(Board::getDeleted, 1);
boardMapper.update(null, wrapper);
```
`PostMapper.xml:215` 的 `WHERE b.deleted = 0` 现在合法，无需改动。

> 注意：已执行过 V2 的库需用上面的 `UPDATE` 语句旋转管理员密码（哈希已进 Git 历史，应视为已泄露，必须改密）。

### 2.2 删除废弃旧前端项目（P0）

`campus-forum-frontend/` 是未运行的 Vite 空脚手架（仅 7 个模板文件，`vue-router` 版本误写为 `^5.1.0`），已被根目录 `.gitignore` 标为 inactive。直接删除目录：
```bash
rm -rf "campus-forum-frontend"
```
并在 `doc/` 文档中统一将"前端"指向 `campus-forum-frontend-new`。

### 2.3 新前端补充 `.gitignore`（P0）

在 `campus-forum-frontend-new/` 下新建 `.gitignore`：
```
node_modules/
dist/
dist-ssr/
*.local
.env
.env.*
!.env.example
.vscode/*
!.vscode/extensions.json
.DS_Store
logs/
*.log
```

---

## 三、Phase 1 — P1 修复

### 3.1 修复评论删除计数逻辑（P1）

`CommentService.deleteComment` 无条件递减计数，但待审核评论（status=0）创建时并未计数。`CommentService.java:172-177` 改为：
```java
// 3. 逻辑删除
comment.setDeleted(1);
commentMapper.updateById(comment);

// 4. 仅当该评论曾计入帖子计数（非待审核状态）时才 -1，与创建逻辑一致
boolean wasPublished = comment.getStatus() != null && comment.getStatus() != 0;
if (wasPublished) {
    postMapper.updateCommentCount(postId, -1);
}
```

### 3.2 修复登录锁定竞态条件（P1）

`AuthService.handleLoginFail` 用内存旧值 `user.getLoginFail() + 1` 判断锁定，并发下不准确。`AuthService.java:349-364` 改为原子递增后从 DB 读取真实值：
```java
private void handleLoginFail(User user) {
    // 1. 原子递增（DB 端完成，无竞态）
    LambdaUpdateWrapper<User> inc = new LambdaUpdateWrapper<>();
    inc.eq(User::getId, user.getId()).setSql("login_fail = login_fail + 1");
    userMapper.update(null, inc);

    // 2. 读取数据库真实失败次数（消除并发竞态）
    User refreshed = userMapper.selectById(user.getId());
    if (refreshed.getLoginFail() >= MAX_LOGIN_FAIL) {
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        LambdaUpdateWrapper<User> lock = new LambdaUpdateWrapper<>();
        lock.eq(User::getId, user.getId()).set(User::getLockedUntil, lockedUntil);
        userMapper.update(null, lock);
        log.warn("用户连续登录失败{}次，已锁定: userId={}", refreshed.getLoginFail(), user.getId());
    }
}
```
> fail-closed：最坏情况是用户被提前锁定，不影响安全。

### 3.3 管理员密码迁移至环境变量（P1）

不再在迁移文件中硬编码哈希。新增启动初始化器 `AdminInitializer.java`：
```java
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    @Value("${campus.admin.student-no:admin001}") private String studentNo;
    @Value("${campus.admin.email:}") private String email;
    @Value("${campus.admin.password:}") private String password;

    @Override
    public void run(String... args) {
        if (password == null || password.isBlank()) return; // 未配置则不创建
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo));
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        if (admin == null) {
            admin = new User();
            admin.setStudentNo(studentNo);
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setNickname("系统管理员");
            admin.setEmail(email);
            admin.setPassword(hash);
            userMapper.insert(admin);
        } else if (!BCrypt.checkpw(password, admin.getPassword())) {
            admin.setPassword(hash);          // 环境变量改密后自动同步
            userMapper.updateById(admin);
        }
    }
}
```
配合 `application.yml` 增加 `campus.admin.*` 配置项（值走环境变量），并将 V2 中历史哈希旋转为占位（见 2.1）。

### 3.4 提取 `formatTime` 公共函数（P1）

新建 `campus-forum-frontend-new/src/utils/format.js`：
```js
export function formatTime(input) {
  if (!input) return ''
  const d = input instanceof Date ? input : new Date(input)
  if (Number.isNaN(d.getTime())) return ''
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)} 天前`
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
```
在 PostCard、CommentList、PostDetail、Message、AiAsk、Profile 共 6 处 `import { formatTime } from '@/utils/format'` 并替换本地实现。

### 3.5 移除 WebSocket 全局变量 hack（P1）

`websocket.js` 不应调用 `window.__messageCallback`。改为在收到消息时写入 Pinia `message` store，`Message.vue` 从 store 响应式读取。

`stores/message.js` 增加：
```js
export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const latest = ref(null)
  function pushMessage(msg) { latest.value = msg; unreadCount.value++ }
  function clearUnread() { unreadCount.value = 0 }
  return { unreadCount, latest, pushMessage, clearUnread }
})
```
`utils/websocket.js` 收到私信时：
```js
import { useMessageStore } from '@/stores/message'
// ...
const msg = JSON.parse(body)
useMessageStore().pushMessage(msg)
```
`Message.vue`：
```js
import { useMessageStore } from '@/stores/message'
const messageStore = useMessageStore()
// 用 computed(() => messageStore.latest) 驱动 UI，删除 onMounted/onUnmounted 的 window.__messageCallback 绑定
```

### 3.6 补充后端模块测试（P1）

为 `board / search / message / user` 四个零测试模块各补一个 Service 单测（沿用现有 JUnit5 + Mockito + AssertJ 风格）。示例 `BoardServiceTest`：
```java
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {
    @Mock BoardMapper boardMapper;
    @InjectMocks BoardService boardService;

    @Test
    void deleteBoard_should_logic_delete_not_physical() {
        boardService.deleteBoard(1L);
        ArgumentCaptor<LambdaUpdateWrapper<Board>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(boardMapper).update(isNull(), cap.capture());
        assertTrue(cap.getValue().getSqlSet().contains("deleted"));
    }
}
```

### 3.7 Controller 集成测试（P1）

新增 `@WebMvcTest` 安全测试，验证未认证访问受保护接口返回 401、非管理员访问 `/api/admin/**` 返回 403：
```java
@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc
class SecurityTest {
    @Autowired MockMvc mvc;
    @Test void unauthenticated_getProtected_returns401() throws Exception {
        mvc.perform(get("/api/posts/1/comments")).andExpect(status().isUnauthorized());
    }
}
```

### 3.8 前端单元测试（P1）

引入 Vitest + @vue/test-utils，`package.json` 增加：
```json
"scripts": { "test": "vitest run", "test:watch": "vitest" },
"devDependencies": { "vitest": "^2.1.0", "@vue/test-utils": "^2.4.6", "jsdom": "^25.0.0" }
```
优先覆盖 `utils/request.js`（Token 刷新队列、401 重试）和 `utils/format.js`。

### 3.9 CI/CD（P1）

新增 `.github/workflows/ci.yml`：
```yaml
name: CI
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres: { image: postgres:16, env: { POSTGRES_PASSWORD: test }, ports: ["5432:5432"] }
      redis: { image: redis:7, ports: ["6379:6379"] }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - run: mvn -q -f campus-forum-backend test
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: { version: 9 }
      - run: pnpm -C campus-forum-frontend-new install
      - run: pnpm -C campus-forum-frontend-new lint
      - run: pnpm -C campus-forum-frontend-new test
```

### 3.10 Docker 容器化（P1）

**后端 `campus-forum-backend/Dockerfile`（多阶段）：**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

**前端 `campus-forum-frontend-new/Dockerfile`（构建 + Nginx）：**
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json pnpm-lock.yaml* ./
RUN corepack enable && pnpm install
COPY . .
RUN pnpm build
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

**`nginx.conf`（SPA 路由回退）：**
```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  location / { try_files $uri $uri/ /index.html; }
  location /api/ { proxy_pass http://backend:8080; }
  location /ws/  { proxy_pass http://backend:8080; proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; }
}
```

**`docker-compose.yml`：**
```yaml
services:
  postgres: { image: postgres:16, environment: { POSTGRES_PASSWORD: ${DB_PASSWORD} }, volumes: [pgdata:/var/lib/postgresql/data] }
  redis: { image: redis:7 }
  backend: { build: ./campus-forum-backend, environment: [SPRING_PROFILES_ACTIVE=prod, JWT_ACCESS_SECRET, JWT_REFRESH_SECRET, DB_PASSWORD], depends_on: [postgres, redis] }
  frontend: { build: ./campus-forum-frontend-new, ports: ["80:80"], depends_on: [backend] }
volumes: { pgdata: {} }
```

### 3.11 部署文档 + `.env.example`（P1）

根目录新增 `.env.example`：
```bash
# 后端
DB_PASSWORD=
JWT_ACCESS_SECRET=
JWT_REFRESH_SECRET=
CAMPUS_ADMIN_STUDENT_NO=admin001
CAMPUS_ADMIN_EMAIL=admin@your-school.edu.cn
CAMPUS_ADMIN_PASSWORD=
CAMPUS_SCHOOL_EMAIL_DOMAIN=@your-school.edu.cn
# 前端
VITE_API_BASE_URL=https://your-domain.com
```
新增 `doc/deploy.md`：环境依赖、Docker 启动命令、Nginx 配置、首次改密步骤、备份策略。

---

## 四、Phase 2 — P2 改进

### 4.1 消除 `MessageService.convertToVO` 的 N+1（P2）
`selectChatHistory` 已 JOIN user 表，应在 SQL 中直接 `SELECT` 出发送者/接收者字段并在 `convertToVO` 使用，避免对每条消息再 `selectById`。

### 4.2 `PostDetail.vue` DOMPurify 白名单统一（P2）
复用 `RichEditor.vue` 的 `ALLOWED_TAGS / ALLOWED_ATTR` 配置，抽成 `src/utils/sanitize.js` 导出 `sanitizeHtml()`，两处统一引用。

### 4.3 学校邮箱域名配置化（P2）
将 `AuthService.validateSchoolEmail()` 与 `RegisterRequest` 中的 `@xxx.edu.cn` 改为读取 `CAMPUS_SCHOOL_EMAIL_DOMAIN` 配置项（见 3.11）。

### 4.4 CORS 收紧（P2）
`CorsConfig` 中 `allowedOriginPatterns` 明确列出前端域名，`allowedHeaders` 改为显式头列表（非 `*`），保留 `allowCredentials(true)`。

### 4.5 引入 ESLint + Prettier（P2）
前端新增 `.eslintrc.cjs` + `.prettierrc` + `lint` 脚本；后端可加 Spotless（Maven 插件）统一格式。

### 4.6 渐进引入 TypeScript（P2）
先在 `api/`、`stores/` 层加 `.ts` 与类型定义，组件层逐步迁移；Vite 原生支持混用，可分批进行。

### 4.7 E2E 测试落地（P2）
基于 `doc/tasks/e2e.md` 的 10 个任务，用 Playwright 实现核心链路（登录→发帖→评论→私信→搜索），接入 CI 可选步骤。

### 4.8 版本管理规范（P2）
启用常规提交（Conventional Commits）+ 语义化版本；后续功能分支开发，重要节点打 `git tag`（如 `v1.1.0`）。

### 4.9 `updated_at` 数据库触发器（P2）
为每个表加 `BEFORE UPDATE` 触发器自动 `SET NEW.updated_at = NOW()`，双保险防止应用漏写。

### 4.10 进度文档同步（P2）
更新 `doc/tasks/progress.md` 至当前状态，补充 AI 功能与前端新项目的进度，后续随迭代维护。

---

## 五、验证清单

- [ ] `mvn -f campus-forum-backend test` 全绿，含新增 board/message/user/search 测试
- [ ] 启动后访问 `/api/admin/boards/stats` 不再报 SQL 异常
- [ ] 删除待审核评论后帖子 `comment_count` 不变化；删除已发布评论后 -1
- [ ] 连续 5 次错误密码后账号锁定（并发压测验证）
- [ ] 管理员密码已从 Git 历史哈希旋转，启动时由环境变量注入
- [ ] 前端 `pnpm lint && pnpm test` 通过
- [ ] `docker compose up` 一键拉起四服务，前端可访问
- [ ] CI 流水线在 PR 上自动运行前后端测试
- [ ] `.env.example` 与 `doc/deploy.md` 齐全
