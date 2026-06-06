# Material LIMS 部署运维手册

适用版本：1.0.0（Phase 5 收尾）。

## 一、依赖服务

| 服务 | 版本 | 必需 | 用途 |
|------|------|------|------|
| PostgreSQL | 15+ | 必需 | 主数据库 |
| Redis | 7+ | 推荐 | 缓存（i18n / 节假日 / 基础数据），未启用时缓存自动降级到本进程内存 |
| MinIO | RELEASE.2024+ | 推荐 | 报告文件 / 知识库附件存储；未启用时文件落盘到 `${java.io.tmpdir}/lims-files/` |
| LibreOffice | 7.x | 可选 | Word→PDF 转换；未安装时 PDF 端点降级返回原 docx |
| Azure AD | - | 生产必需 | SSO / OAuth2 登录；dev profile 走占位符 |

## 二、Docker Compose 一键部署

仓库根目录已提供 `docker-compose.yml`。

```bash
# 1. 拷贝环境变量
cp .env.example .env
# 编辑 .env，至少设置：
#   DB_PASSWORD / AZURE_AD_TENANT_ID / AZURE_AD_CLIENT_ID / AZURE_AD_CLIENT_SECRET

# 2. 构建并启动
docker compose up -d --build

# 3. 初始化数据库（首次部署）
docker compose exec lims-web psql $DATABASE_URL -f /app/db/schema.sql

# 4. 健康检查
curl http://localhost:8080/actuator/health
```

## 三、必备环境变量清单

后端（`lims-web`）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD` | localhost / 5432 / lims / lims / lims | PostgreSQL 连接 |
| `REDIS_HOST / REDIS_PORT` | localhost / 6379 | Redis 连接 |
| `SPRING_CACHE_TYPE` | simple | 生产环境改为 `redis` |
| `MINIO_ENDPOINT / MINIO_ACCESS_KEY / MINIO_SECRET_KEY / MINIO_BUCKET` | http://localhost:9000 / minioadmin / minioadmin / lims | MinIO 连接 |
| `AZURE_AD_TENANT_ID / AZURE_AD_CLIENT_ID / AZURE_AD_CLIENT_SECRET / AZURE_AD_REDIRECT_URI` | - | SSO |
| `EXTERNAL_PARTS_BASE_URL / EXTERNAL_SUPPLIERS_BASE_URL` | 空 | 零部件 / 供应商 API 上游 |
| `EXTERNAL_API_MOCK_ENABLED` | false | 上游不可用时改 true 启用 mock |
| `JWT_SECRET` | - | JWT 签名密钥，生产必须 32 字节随机 |
| `SPRING_PROFILES_ACTIVE` | dev | 生产改为 `prod` |

前端（`lims-web-ui`）通过反向代理转发 `/api/*` 到 `lims-web:8080`。

## 四、备份与恢复

### 数据库

```bash
# 全量备份（每日，保留 30 天）
docker compose exec postgres pg_dump -U lims -F c lims > backup/lims_$(date +%F).dump

# 恢复
docker compose exec -T postgres pg_restore -U lims -d lims --clean --if-exists < backup/lims_2026-06-05.dump
```

### MinIO

```bash
# mc 镜像同步到异地
mc mirror --overwrite minio-prod/lims s3-backup/lims
```

### 节假日表

每年 12 月人工维护下一年节假日，通过管理端 `基础数据 → 节假日 → 批量导入` 上传 CSV。

## 五、常见运维操作

### 清空缓存

```bash
# Redis
docker compose exec redis redis-cli FLUSHDB

# 应用进程级（重启即可）
docker compose restart lims-web
```

### 查看审计日志

后端管理员账号登录 → `系统管理 → 操作日志`，支持按用户、模块、动作、时间区间过滤；点击「View」可见完整请求载荷。

### 用户角色调整

`系统管理 → 用户管理 → 编辑角色`，可启停账号、调整 ADMIN/MANAGER/TECHNICIAN/ENGINEER/REQUESTER 多角色。

## 六、灾难恢复 RTO / RPO

| 故障类型 | RTO | RPO | 处理 |
|---------|-----|-----|------|
| 应用进程异常 | 5 min | 0 | docker compose restart |
| PostgreSQL 损坏 | 30 min | 24 h | 从最近 dump 恢复 |
| MinIO 损坏 | 30 min | 24 h | 从异地镜像恢复 |
| 整机房宕机 | 2 h | 24 h | 异地备份机启动 |

## 七、监控指标

- 应用：`/actuator/health` `/actuator/metrics` `/actuator/prometheus`
- 关键指标：`http_server_requests_seconds`、`hikaricp_connections_active`、`jvm_memory_used_bytes`
- 告警阈值：HTTP 5xx 率 > 1%、DB 连接池 active > 18（max 20）、JVM Old Gen > 85%
