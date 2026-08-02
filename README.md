> 📄 **Deep-dive case study with metrics, highlights, and architecture**: [binbinao.github.io/resume/projects/material-lims/](https://binbinao.github.io/resume/projects/material-lims/)

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
├── lims-web-ui/          # 前端React项目
│   ├── src/
│   │   ├── components/   # 可复用组件
│   │   │   ├── LogoutButton/          # 左下角退出按钮
│   │   │   ├── LogoutConfirmModal/    # 退出确认对话框
│   │   │   └── EnhancedLogoutButton/  # 增强退出按钮
│   │   ├── layouts/       # 布局组件
│   │   │   └── CustomLayout/           # 自定义布局（集成左下角退出）
│   │   ├── utils/         # 工具函数
│   │   │   └── auth.ts    # 认证工具（支持多种退出方式）
│   │   └── pages/         # 页面组件
│   └── config/            # 配置文件
├── docs/                 # 设计文档
│   ├── design/           # 技术方案
│   ├── writing/          # 项目简报
│   └── runbook/          # 操作手册
│       ├── 左下角退出功能指南.md
│       └── typescript-reviewer-guide.md
└── docker-compose.yml    # 开发环境容器编排（兼容 Podman Compose）
```

## 用户体验增强特性

### 🎯 左下角退出功能

Material LIMS 引入了创新的左下角退出功能，提供更直观的用户体验：

**核心特性**:
- **直观定位**: 退出按钮固定在界面左下角，符合用户操作习惯
- **双重保障**: 保留右上角用户菜单的同时，增加便捷退出入口
- **智能确认**: 退出前显示确认对话框，防止误操作
- **状态管理**: 支持并发控制，避免重复退出操作
- **错误处理**: 完善的错误恢复机制，确保退出流程可靠性

**技术亮点**:
- 响应式设计，适配不同屏幕尺寸
- Material Design 风格，与整体界面保持一致
- 支持键盘导航和屏幕阅读器
- 完整的单元测试覆盖

**详细文档**: [左下角退出功能指南](docs/runbook/左下角退出功能指南.md)

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
