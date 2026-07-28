# 本地开发与 UAT 环境搭建

> 适用场景：开发自测、产品/业务方验收测试(UAT)、Demo 演示。
>
> 与 [`deployment.md`](deployment.md) 的区别：**`deployment.md` 讲全容器化生产部署**；本文档讲**本地开发栈**——基础设施容器化(便于环境一致性)+ 前后端以进程方式本地运行(便于热重载、查看日志、断点调试)。

## 一、架构概览

```
┌─────────────────────┐  proxy /api  ┌─────────────────────┐
│  lims-web-ui dev    │ ───────────▶  │  lims-web (Spring)  │
│  umi dev :8001      │               │  :8080 dev profile  │
└─────────────────────┘               └──────────┬──────────┘
                                                  │
                       ┌──────────────────────────┼──────────────────────────┐
                       ▼                          ▼                          ▼
              ┌──────────────┐           ┌──────────────┐           ┌──────────────┐
              │  postgres:15 │           │   redis:7    │           │   minio:8.5  │
              │     :5432    │           │    :6379     │           │  :9000/9001  │
              │   (podman)   │           │   (podman)   │           │   (podman)   │
              └──────────────┘           └──────────────┘           └──────────────┘
                       │
                       └── Flyway 自动管理 V1..V16 迁移
```

- **dev profile** 启用 [`DevAuthFilter`](../../lims-web/src/main/java/com/lims/web/security/DevAuthFilter.java)：根据 `X-Dev-User` 请求头注入虚拟身份，**绕过 Azure AD 登录**。
- 数据库/Redis/MinIO 全部跑在 podman 容器内，统一 `localhost` 端口暴露，避免污染主机环境。
- 前端 dev server 代理 `/api` → `http://localhost:8080`，浏览器无跨域问题。

## 二、前置条件

| 工具 | 版本 | 验证命令 | 说明 |
|---|---|---|---|
| **podman** | 5.x | `podman --version` | 容器运行时;若用 Docker 可替换为 `docker` |
| **podman-compose** | 1.6+ | `podman-compose --version` | 与 `docker compose` 命令兼容 |
| **JDK** | 17+ (实测 21 可用) | `java -version` | Spring Boot 3.2 兼容 17–21;21 会输出 `release version 17` 编译目标 |
| **Maven** | 通过 `./mvnw` | `./mvnw --version` | 项目自带 wrapper,无需全局安装 |
| **Node.js** | 18+ | `node -v` | 前端 dev server |
| **npm** | 9+ | `npm -v` | |

macOS 推荐使用 Homebrew 安装:

```bash
brew install podman podman-compose
brew install --cask temurin@17    # 若已有 JDK 21 可跳过
```

> **首次使用 podman 需要初始化虚拟机**:
> ```bash
> podman machine init
> podman machine start
> ```

## 三、启动顺序

### 3.1 启动基础设施容器

```bash
# 仓库根目录
cd /path/to/Material-LIMS

# 后台启动 postgres / redis / minio
podman-compose up -d postgres redis minio
```

预期输出:

```
 Container material-lims_postgres_1  Started
 Container material-lims_redis_1     Started
 Container material-lims_minio_1     Started
```

等待就绪(10–30 秒):

```bash
podman ps --format "table {{.Names}}\t{{.Status}}"
# 期望: postgres 显示 "(healthy)"、redis 显示 "(healthy)"、minio 显示 "(running)"
```

### 3.2 准备环境变量文件

```bash
# 项目根目录已有 .env.example 模板
cp .env.example .env

# 关键变量(其他保持默认):
#   SPRING_PROFILES_ACTIVE=dev   ← 启用 DevAuthFilter
#   POSTGRES_PASSWORD=lims_dev_password
#   MINIO_ACCESS_KEY=minioadmin
#   MINIO_SECRET_KEY=minioadmin
```

### 3.3 启动后端(后台)

```bash
# 加载 .env 并启动 Spring Boot
set -a && source .env && set +a
./mvnw spring-boot:run -pl lims-web
```

首次启动 30s–3min(下载依赖 + Maven 编译 + Spring/Flowable 初始化)。看到这条日志即成功:

```
Tomcat started on port 8080 (http)
Started LimsApplication in N.NNN seconds
```

> **后台运行**:本文档以交互式为例;若用 `run_in_background` 跑 Claude Code / CI,可把整条命令丢到后台,日志会写入 `tasks/<id>.output`。

### 3.4 启动前端(后台)

```bash
cd lims-web-ui
npm install      # 首次需要,约 1-2 分钟
npm run dev
```

期望输出(Umi 4.6.59):

```
  ╔════════════════════════════════════════════════════╗
  ║ App listening at:                                  ║
  ║  >   Local: http://localhost:8001                  ║
  ║  > Network: http://192.168.x.x:8001                ║
  ╚════════════════════════════════════════════════════╝
```

> ⚠️ **8000 端口冲突**:Umi 默认监听 8000;若被占用(常见:本机跑着 `uvicorn` / 其他服务)会自动切到 8001。**不需要改 Umi 配置**,直接访问提示的端口即可。

## 四、健康检查与端口表

| 服务 | 端口 | 健康检查命令 | 期望 |
|---|---|---|---|
| 前端 | 8001 (或 8000) | `curl -I http://localhost:8001` | HTTP 200 |
| 后端 API | 8080 | `curl -H "X-Dev-User: dev" http://localhost:8080/api/v1/brands` | `{"code":200,...}` |
| Swagger UI | 8080 | `curl -I http://localhost:8080/swagger-ui.html` | HTTP 302 → /swagger-ui/index.html |
| OpenAPI | 8080 | `curl http://localhost:8080/v3/api-docs` | HTTP 200 |
| Postgres | 5432 | `podman exec material-lims_postgres_1 pg_isready -U lims` | `accepting connections` |
| Redis | 6379 | `podman exec material-lims_redis_1 redis-cli ping` | `PONG` |
| MinIO | 9000 / 9001 | `curl -I http://localhost:9000/minio/health/live` | HTTP 200 |

后端日志关键词(grep 快速诊断):

```bash
# Tomcat 已启动
grep "Tomcat started on port 8080" <backend_log>

# Flyway 迁移完成
grep "Flyway Community Edition" <backend_log>     # 启动行
grep "Successfully applied" <backend_log>          # 历史迁移
grep "Migrating schema" <backend_log>              # 当前迁移(若有新版本)

# Flowable 流程部署
grep "Auto-Deploying process" <backend_log>
grep "request-process.bpmn20.xml" <backend_log>    # 验证关键流程已加载
```

## 五、DevAuth 用户身份切换

dev profile 下 [`DevAuthFilter`](../../lims-web/src/main/java/com/lims/web/security/DevAuthFilter.java) 解析 `X-Dev-User` 请求头,映射到 V2 seed 里的真实用户:

| X-Dev-User 值 | 模拟身份 | 角色 | 业务场景 |
|---|---|---|---|
| `dev` (默认) | Dev User | **ALL ROLES** | 跨角色操作,无权限拦截 |
| `admin` | Admin User | ADMIN | 用户管理、审计日志 |
| `manager` | Manager User | MANAGER | 委托分派、报告审批(四眼原则) |
| `engineer` | Engineer User | ENGINEER | 报告创建/修订 |
| `tech` | Technician User | TECHNICIAN | 样品接收、状态推进 |
| `requester` | Requester User | REQUESTER | 创建委托、查看进度 |

**浏览器侧切换**:

1. 打开 DevTools → `Application` → `Local Storage` → 选中 lims-web-ui origin
2. 删除或编辑 `X-Dev-User` 键(若前端未注入该键,使用浏览器扩展 "ModHeader" 注入请求头更方便)
3. 刷新页面

**API 直连切换** (`curl` 示例):

```bash
# 以 manager 身份查询分派任务
curl -H "X-Dev-User: manager" http://localhost:8080/api/v1/requests?status=SUBMITTED

# 切换到 engineer 后同一接口返回不同结果(基于角色过滤)
curl -H "X-Dev-User: engineer" http://localhost:8080/api/v1/requests?status=SUBMITTED
```

**dev 密码**(登录页 fallback,非主流程):所有 6 个用户密码统一为 `password`(由 V16 迁移覆盖)。

## 六、典型 UAT 流程

### 6.1 端到端冒烟测试(5 分钟)

走完委托 → 报告 → 审批 → 修订 → 完成 的最短链路:

```bash
# 1. 用 E2E 套件跑(API 级,9 个场景,~8s)
cd lims-web-ui
npx playwright test                       # 默认配置
npx playwright show-report               # 查看报告

# 2. 浏览器手动验证(模拟真实 UAT)
#    ① 打开 http://localhost:8001
#    ② X-Dev-User: requester → 创建委托 E2E-PART-001
#    ③ 切换到 manager → 分派工程师
#    ④ 切换到 tech → 接收样品 + 完成分析任务
#    ⑤ 切换到 engineer → 创建报告 + 提交审批
#    ⑥ 切换到 manager → 批准 → 驳回 → 再批准(走修订流程)
#    ⑦ 切换回 requester → 查看终态
```

### 6.2 单功能验证(按页面)

| 页面 | 验证点 | 切换身份 |
|---|---|---|
| `/request/create` | 表单提交,priority 枚举,分析项多选 | `requester` |
| `/request/kanban` | 看板拖拽,状态色块 | `manager` |
| `/request/detail` | 流程时间线,样品信息 | `requester` 或 `manager` |
| `/report/edit` | Word 模板渲染,MinIO 上传 | `engineer` |
| `/report/revisions` | 版本对比,V1.0 → V2.0 升级 | `manager` + `engineer` |
| `/equipment/repairs` | 维修工单 CRUD | `tech` |
| `/knowledge` | 文件上传,目录树 | `engineer` |
| `/dashboard/request` | ECharts 统计图渲染 | `manager` |
| `/admin/users` | 角色修改,启停 | `admin` |
| `/admin/logs` | 审计日志过滤 | `admin` |
| **侧边栏底部用户菜单** | 头像 + 退出登录入口 | 任意身份 |

## 七、常见问题排错

### 7.1 后端启动失败

| 症状 | 原因 | 解决 |
|---|---|---|
| `Connection refused: localhost:5432` | postgres 容器没起或没 ready | `podman ps`;等 `(healthy)` 出现再启后端 |
| `PSQLException: password authentication failed` | `.env` 密码与容器不一致 | 确认 `POSTGRES_PASSWORD=lims_dev_password` |
| `FlywayValidateException: Migration checksum mismatch` | V*.sql 被修改过 | 重新生成 baseline 或恢复文件:`git checkout HEAD -- lims-web/src/main/resources/db/migration/` |
| `BeanDefinitionStoreException: Failed to parse mapper` | MyBatis-Plus mapper 路径错 | 全量清理:`./mvnw clean` 后再启动 |
| `JAVA_HOME` 报错 | 未设 JAVA_HOME | macOS 自带 Temurin 无需设置;Linux 需 `export JAVA_HOME=$(/usr/libexec/java_home)` |

### 7.2 前端启动失败

| 症状 | 原因 | 解决 |
|---|---|---|
| `EACCES: permission denied, mkdir '/usr/local/lib/node_modules'` | npm 全局权限问题 | 用 `npm install --prefix=./node_modules` 或 `nvm` |
| `MFSU worker init failed` | 缓存损坏 | `rm -rf lims-web-ui/.umi* lims-web-ui/node_modules/.cache` |
| `Port 8000 is in use` | 与其他服务冲突 | Umi 自动切 8001,接受即可;或 `lsof -nP -iTCP:8000` 查进程 |
| `Proxy error: ECONNREFUSED 127.0.0.1:8080` | 后端没启或挂了 | 启动后端后刷新页面 |

### 7.3 数据 / 状态异常

| 症状 | 原因 | 解决 |
|---|---|---|
| 登录报"用户不存在" | 数据库表是空 schema,Flyway 没跑 | 检查后端日志 `Successfully applied`;手动执行 `psql -f lims-web/src/main/resources/db/schema.sql` |
| 角色权限错误(403) | 切换 X-Dev-User 后旧 token 还在 | 浏览器清 LocalStorage + 刷新;后端 DevAuthFilter 不发 token,前端应清旧 session |
| 报告文件下载 404 | MinIO 容器没起或 bucket 缺失 | 访问 :9001 控制台用 minioadmin/minioadmin 登录,确认 `lims` bucket 存在 |
| 数据库里看不到种子用户 | V2 迁移没跑 | `psql` 查 `flyway_schema_history` 表,确认 version=2 success=t |

## 八、关闭 / 清理

```bash
# 1. 停前后端(前台启动时 Ctrl+C;后台时用进程管理)
#    或按 PID:
kill <JAVA_PID> <NODE_PID>

# 2. 停容器(保留数据卷,下次 podman-compose up 直接复用)
podman-compose stop postgres redis minio

# 3. 删容器 + 数据卷(完全清理,下次启动会重新初始化)
podman-compose down -v

# 4. 清 Flyway 锁(若迁移卡死)
podman exec material-lims_postgres_1 psql -U lims -d lims \
  -c "UPDATE flyway_schema_history SET success = true WHERE success = false;"
```

## 九、相关文档

- [`deployment.md`](deployment.md) — 全容器化生产部署
- [`api-summary.md`](api-summary.md) — REST 接口目录 + 角色矩阵
- [`user-manual.md`](user-manual.md) — 终端用户手册
- [`左下角退出功能指南.md`](左下角退出功能指南.md) — Commit 4 涉及的退出登录功能说明
- [`ADR-003-dual-profile-security-strategy.md`](../adr/ADR-003-dual-profile-security-strategy.md) — dev / prod 双 profile 安全策略
