# 拓界：AI 个人成长与决策工作台

“拓界”是一个面向个人成长与现实决策的响应式工作台。项目将自然语言转为结构化数据，再通过图谱、路径和比较视图让用户检查、修改并继续操作，而不是停留在一次性 AI 问答。

当前仓库包含能力成长前端原型，以及可运行的决策沙盒前后端。后端已具备 MySQL 8.4 配置与 Flyway 迁移；本机无 Docker/MySQL 时可使用文件型 H2 演示模式。

## 当前实现

- Vue 3 + TypeScript + Vite 前端基础工程；
- 全局工作台首页 `/`；
- 能力成长模块 `/ability`；
- 可编辑个人技能树、节点详情和可解释成长路径；
- 可按方向、阶段和技术项渲染的知识分类，并支持 AI/API 生成同结构内容；
- 新节点防重叠布局、同名候选合并和本地演示持久化；
- 决策沙盒 `/decision`：场景录入、自然语言候选条件确认、条件关系、规则推演、方案对比和版本回溯；
- 决策沙盒 Spring Boot API、JPA/Flyway 数据持久化和无密钥确定性 mock；
- 白色与浅色工作台视觉系统，以及桌面/360px 响应式布局；
- 模块一 API service fallback：后端暂不可用时仍可演示能力成长；决策沙盒需启动后端，避免把浏览器本地数据误当作数据库持久化。

## 技术基线

```text
前端：Vue 3 + TypeScript + Vite + Vue Router + Pinia
图标：lucide-vue-next
后端：Java 21 + Spring Boot 3.5 + JPA + MySQL 8.4 + Flyway（另有 H2 演示 profile）
接口前缀：/api/v1
```

## 目录结构

```text
frontend/
  src/
    app/                    路由与应用级结构
    core/                   共享类型和 API service
    modules/ability-growth/ 模块一前端
    modules/decision-sandbox/ 模块二前端
    pages/                  首页与模块入口
    styles/main.css         当前视觉 token 和公共样式
backend/
  src/main/java/com/ican/assistant/core/                共享 Web/CORS/错误处理及 AI 适配器
  src/main/java/com/ican/assistant/modules/decisionsandbox/ 模块二业务代码
  src/main/resources/db/migration/                      Flyway 迁移
plans/
  00-shared-integration.md  技术与协作合同
  00-ui-style-contract.md   前端视觉合同
  01-*.md                   模块一任务书
  02-*.md                   模块二任务书
  03-*.md                   第三模块骨架任务书
  04-integration-review.md  合并前验收表
```

## 本地运行

需要 Node.js 20 或更高版本。决策沙盒后端还需要 Java 21、Maven 3.9+；MySQL 模式使用 MySQL 8.4（可用 Docker Compose 启动）。

```bash
cd frontend
npm install
npm run dev
```

默认访问：

- 首页：`http://localhost:5173/`
- 能力成长：`http://localhost:5173/ability`
- 决策沙盒：`http://localhost:5173/decision`

生产构建：

```bash
cd frontend
npm run build
```

开发服务器会把 `/api` 代理到 `http://localhost:8000`。不使用 MySQL 时，在另一个终端启动文件型 H2 演示后端：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

首次启动会创建演示场景；场景和版本保存在 `backend/target/demo-db.mv.db`，重启后可恢复。MySQL、环境变量和 API 细节见 [决策沙盒模块文档](frontend/src/modules/decision-sandbox/README.md)。模块一在其后端暂不可用时仍通过自己的 service 层 fallback 演示。

## 协作入口

所有队员和 Codex 会话先阅读 [plans/README.md](plans/README.md)，再按其中顺序阅读共享技术合同、视觉合同、自己的模块任务书和合并验收表。

核心规则：

- 不重建现有前端工程，不覆盖其他模块；
- `/` 与 `/ability` 是当前视觉基准；
- 不修改共享视觉 token 来迁就单个模块；
- 新模块样式使用模块前缀；
- 通过 service 与 `/api/v1` 协作，组件不直接访问后端；
- 提交前实测 `1280 x 720`、`360 x 800` 并运行构建。

## 当前未完成

- 能力数据的服务端持久化与完整关系编辑；
- 第三模块最终方向；
- 生产部署、安全认证及 MySQL 8.4 的真实环境验收；决策沙盒已有规则、接口和浏览器自动化测试。

详细状态和 API 边界见 [frontend/README.md](frontend/README.md)。
