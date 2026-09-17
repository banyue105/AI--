# 拓界：AI 个人成长与决策工作台

“拓界”将自然语言转为结构化数据，再通过图谱、路径和比较视图，让用户检查、修改并继续操作。

当前已加入 Java 21、Spring Boot、MySQL 和 MyBatis 后端，首页、用户资料、能力图谱及知识方向已有对应 API。能力成长可保存到数据库；决策沙盒仍为占位页，第三模块方向待定。自然语言解析和知识方向生成目前使用明确标注的本地规则与模板，尚未连接真实模型。

## 当前实现

- Vue 3 + TypeScript + Vite + Vue Router + Pinia 前端。
- 首页 `/`：用户概览、当前目标、最近活动和模块入口。
- 能力成长 `/ability`：图谱、节点增改、等级与证据展示、候选能力确认及成长路径。
- 服务端保存用户资料、目标文本、技能、关系和实践证据。
- 根据提交的能力与前置关系生成成长路径，校验不存在的节点和循环依赖。
- 知识方向预置前端、后端和网络工程；新方向通过本地模板生成，保存到数据库后刷新可读取，同方向生成使用稳定 ID。
- 数据库迁移、参数校验、统一错误响应、开发环境 CORS 和 MySQL Compose 配置。
- 仅在尚未连接后端的离线演示场景下使用 service 层 fallback；服务器拒绝的请求会显示错误。

## 技术基线

| 部分 | 当前技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia |
| 图标 | lucide-vue-next |
| 后端 | Java 21、Spring Boot 3.5.16、Spring Web、Bean Validation |
| 数据访问 | MyBatis Spring Boot Starter 3.0.5、MySQL 8.4、Flyway |
| 测试 | JUnit 5、Spring Boot Test、MockMvc、H2；前端 Node 测试 |
| 接口前缀 | `/api/v1` |

## 目录结构

```text
frontend/
  src/app/                       路由与应用结构
  src/core/api/                  共享 API client 和全局 service
  src/modules/ability-growth/    能力成长页面、状态和 service
  src/pages/                     首页与模块入口
  src/styles/main.css            视觉 token 和公共样式
  tests/api-services.test.cjs     API 与离线演示行为测试
backend/
  pom.xml                        Java 21 / Spring Boot / MyBatis 依赖
  docker-compose.yml             本地 MySQL 8.4
  .env.example                   开发配置示例
  src/main/java/com/ican/assistant/
    api/                         首页、用户资料、模块清单、健康检查
    core/ai/                     本地解析和知识生成适配层
    modules/abilitygrowth/       能力接口、MyBatis Mapper、成长路径规则
    modules/knowledge/           知识目录接口与持久化
  src/main/resources/db/migration/  Flyway 表结构与种子数据
plans/                           技术合同、视觉合同、任务书与验收表
```

## 本地运行

需要 JDK 21、Docker Compose，以及 Node.js 20.19+ 或 22.12+。Maven Wrapper 首次运行需要下载 Maven 和项目依赖。

先在 PowerShell 中配置并启动后端：

```powershell
cd backend
Copy-Item .env.example .env
```

编辑 `backend/.env`，设置自己的 `DB_PASSWORD` 和 `MYSQL_ROOT_PASSWORD`。已有 `.env` 时直接检查配置，不要覆盖原文件。

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

Linux/macOS 使用 `cp .env.example .env`，设置密码后执行 `docker compose up -d --wait` 和 `./mvnw spring-boot:run`。MySQL 默认监听 `127.0.0.1:3307`，API 默认监听 `127.0.0.1:8000`。健康检查：[GET /api/v1/health](http://127.0.0.1:8000/api/v1/health)。完整配置和接口说明见 [backend/README.md](backend/README.md)。

另开终端启动前端：

```bash
cd frontend
npm install
npm run dev
```

访问 [首页](http://localhost:5173/)、[能力成长](http://localhost:5173/ability) 或 [决策沙盒占位页](http://localhost:5173/decision)。Vite 将 `/api` 代理到 `http://localhost:8000`。

## 构建与测试

前端：

```bash
cd frontend
npm run build
node --test tests/api-services.test.cjs
```

若受限 Windows 环境中的 Vite 配置打包遇到目录权限问题，可使用 `npm run build -- --configLoader native`。本轮此前端构建方式和 11 项 API service 测试通过；已检查 1280×720、360×800 浏览器布局，以及真实保存、刷新读取、保存错误反馈和首次离线演示。

后端：

```powershell
cd backend
.\mvnw.cmd test
```

后端 28 项测试分别在 H2 的 MySQL 模式和隔离 MySQL 9.6 实例上通过，可执行 JAR 打包成功。已验证数据库和 API 重启后仍能读取新增技能与知识方向。Compose 使用 MySQL 8.4，配置校验通过，但本机 Docker 引擎未运行，尚未实测该容器版本。可通过 `TEST_DB_*` 变量切换独立测试库，见后端 README。

## 协作入口

先阅读 [plans/README.md](plans/README.md)，再阅读共享技术合同、视觉合同、自己的模块任务书和验收表。

- 沿用现有工程与模块边界。
- `/` 与 `/ability` 是当前视觉基准；共享视觉 token 由集成人维护。
- 新模块样式使用模块前缀；组件通过 service 访问 `/api/v1`。
- 数据访问使用 MyBatis Mapper/Service；表结构变更新增 Flyway migration。
- 提交前检查 `1280 x 720`、`360 x 800`，运行相关构建与测试。

## 当前边界

- 固定使用 `demo-user` 演示用户，未实现登录、鉴权和多用户隔离。
- 真实 AI provider 尚未接入；知识生成的技能摘要已接收，但当前模板不进行个性化模型推理。
- 关系与实践证据已有后端接口，前端专用编辑表单仍待补齐；目标截止日期编辑、删除及版本管理未实现。
- 决策沙盒业务、第三模块方向、生产部署仍待完成。
- 数据库是连接后端后的事实来源；离线演示数据不会自动导入数据库。

前端行为和接口接入说明见 [frontend/README.md](frontend/README.md)。
