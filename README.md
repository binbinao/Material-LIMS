# Material LIMS - 材料实验室信息管理系统

Material Laboratory Information Management System，服务于材料实验的委托、分析、报告全流程管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus + Flowable 7 |
| 前端 | React 18 + TypeScript + Ant Design Pro 6 + Umi.js 4 |
| 数据库 | PostgreSQL 15 + Redis 7 |
| 文件存储 | MinIO |
| 工作流 | Flowable BPMN 2.0 |
| Word在线编辑 | Microsoft 365 Online (Graph API) |
| SSO | Azure AD + OAuth2.0 |

## 项目结构

```
material-lims/
├── lims-common/          # 通用模块（工具类、常量、异常）
├── lims-model/           # 数据模型（Entity、DTO、VO、枚举）
├── lims-dao/             # 数据访问层（Mapper）
├── lims-service/         # 业务逻辑层
├── lims-workflow/        # Flowable工作流模块
├── lims-admin/           # 系统管理模块
├── lims-web/             # Web层（Controller、启动入口）
├── lims-web-ui/          # 前端React项目（待创建）
├── docs/                 # 设计文档
│   ├── design/           # 技术方案
│   └── writing/          # 项目简报
└── docker-compose.yml    # 开发环境容器编排（兼容 Podman Compose）
```

## 快速开始

### 前置条件

- Java 17+
- Maven 3.9+
- Node.js 20 LTS
- Podman + podman-compose（或 Docker + Docker Compose）

### 1. 启动基础设施

```bash
podman-compose up -d postgres redis minio
```

### 2. 初始化数据库

```bash
psql -h localhost -U lims -d lims -f lims-web/src/main/resources/db/schema.sql
```

### 3. 启动后端

**方式 A：容器（推荐，与生产环境一致）**

```bash
podman-compose build lims-backend
podman-compose up -d lims-backend
```

**方式 B：本地 Maven 开发**

```bash
# 需先停止容器，避免 8080 端口冲突
podman-compose stop lims-backend
./mvnw spring-boot:run -pl lims-web
```

后端启动后访问：
- API: http://localhost:8080/api/v1/
- Swagger UI: http://localhost:8080/swagger-ui.html

### 4. 启动前端

```bash
cd lims-web-ui
npm install
npm run dev
```

前端访问：http://localhost:8000

## 配置

核心配置通过环境变量注入：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DB_HOST | PostgreSQL 主机 | localhost |
| DB_PORT | PostgreSQL 端口 | 5432 |
| DB_NAME | 数据库名 | lims |
| DB_USER | 数据库用户 | lims |
| DB_PASSWORD | 数据库密码 | lims |
| REDIS_HOST | Redis 主机 | localhost |
| MINIO_ENDPOINT | MinIO 地址 | http://localhost:9000 |
| AZURE_AD_TENANT_ID | Azure AD 租户ID | - |
| AZURE_AD_CLIENT_ID | Azure AD 客户端ID | - |
| AZURE_AD_CLIENT_SECRET | Azure AD 客户端密钥 | - |

## 设计文档

- [项目简报](docs/writing/2026-06-04-material-lims-brief.md)
- [技术方案设计文档](docs/design/material-lims-design.md)

## License

Proprietary - Internal Use Only
