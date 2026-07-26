# Campus Forum 部署文档

## 一、前置条件（裸机/本地）

- Java 17（运行 Spring Boot）
- Node.js 22（构建前端）
- PostgreSQL 16（业务数据库）
- Redis 7（缓存 / 会话）

## 二、环境变量配置

复制示例文件并按需填写：

```bash
cp .env.example .env
# 编辑 .env，至少填写下列字段：
#   DB_PASSWORD、JWT_ACCESS_SECRET、JWT_REFRESH_SECRET、CAMPUS_ADMIN_PASSWORD
```

`docker compose` 会自动读取同目录下的 `.env`。

## 三、使用 Docker 启动全部服务

在仓库根目录执行：

```bash
docker compose up --build
```

- `postgres`：持久化卷 `pgdata`
- `redis`：7
- `backend`：构建 `campus-forum-backend`，`SPRING_PROFILES_ACTIVE=prod`
- `frontend`：构建 `campus-forum-frontend-new`，Nginx 监听 80，代理 `/api/` 与 `/ws/` 到 backend:8080

访问 `http://<host>/` 即可。

## 四、Nginx 说明

前端镜像内置 `nginx.conf`：

- `location /`：`try_files` 支持 Vue SPA 前端路由
- `location /api/`：反向代理到 `http://backend:8080`
- `location /ws/`：开启 `Upgrade`/`Connection` 头，支持 WebSocket

如需 HTTPS，请在宿主机 Nginx 或云负载均衡上终止 TLS，再反代到本容器 80 端口。

## 五、首次部署必做

1. 在 `.env` 中设置 `CAMPUS_ADMIN_PASSWORD`。
2. 轮换历史管理员密码哈希：启动后调用管理员重置接口（或直连数据库更新 admin 用户密码），避免使用仓库中遗留的默认哈希。
3. 备份数据库：

```bash
pg_dump -U postgres -h localhost campus_forum > backup_$(date +%F).sql
```

## 六、不使用 Docker 的本地运行（开发）

后端：

```bash
cd campus-forum-backend
mvn spring-boot:run   # 需本地 PostgreSQL / Redis，且已设置对应环境变量
```

前端：

```bash
cd campus-forum-frontend-new
npm install
npm run dev           # 开发服务器，默认 http://localhost:5173
# 生产构建：npm run build -> 产物在 dist/
```
