# 决策沙盒模块

面向个人和小团队的条件推演工作台。使用顺序：创建场景 → 表单录入或自然语言提取 → 确认候选条件 → 查看节点与关系 → 推演基准/变更方案 → 编辑条件并重新推演 → 比较与恢复版本。不是交付预测或承诺。

## 边界与实现

- 前端业务仅在 `frontend/src/modules/decision-sandbox/`；`DecisionSandboxPage.vue` 不调用 `fetch`，网络请求集中在 `services/decisionService.ts`，Pinia 负责加载、错误、保存与版本状态。样式仅新增 `.decision-*`，复用已有公共按钮、弹窗、状态类，不改能力成长模块或共享 token。
- 模块二后端仅在 `backend/src/main/java/com/ican/assistant/modules/decisionsandbox/`。`DecisionRules` 独立负责数值计算；`core/ai/DecisionAiGateway` 只负责条件提取和文字解释。没有 `AI_API_KEY` 时转到确定性 `MockDecisionAiProvider`；若 AI 服务失败，也会回退到 mock。AI 的回答不作为数值输入。
- 后端共享骨架：`AssistantApplication`、`core/CorsConfig`、`core/ApiExceptionHandler`、`api/PlatformController`，供其他模块以后接入，但目前没有实现模块一后端接口。`core/ai/` 是可复用适配层；其他模块不要覆盖这些文件，应增量扩展。
- 当前首页模块清单由前端 `homeService` fallback 提供；后端 `/api/v1/modules` 可返回模块状态。模块二不读取模块一内部 store。

## 本地运行

前端需要 Node.js 20+；后端需要 Java 21、Maven 3.9+。先启动后端，再运行前端：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173/decision`。`demo` profile 使用文件型 H2，数据保存在 `backend/target/demo-db.mv.db`；首次空库会插入“60 天完成 AI 网页项目”及一条推演版本。该模式用于本机没有 Docker/MySQL 的演示，不能替代 MySQL 8.4 的真实环境验收。可用 `DEMO_DB_PATH` 改变文件位置，`APP_SEED_DEMO=false` 禁止种子数据。

MySQL 8.4 模式：在 `backend/` 依据 `.env.example` 配置环境变量（Compose 会读取同目录 `.env`），运行 `docker compose up -d`，随后 `mvn spring-boot:run`。默认数据库是 `ican_assistant`，Flyway 自动执行 `V1__decision_sandbox.sql`；`spring.jpa.hibernate.ddl-auto=validate` 只校验实体，不隐式改表。不要把真实密钥或密码提交到仓库。

| 环境变量 | 用途 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL JDBC 连接；默认 `localhost:3306/ican_assistant` |
| `MYSQL_ROOT_PASSWORD` | Docker Compose 初始化 MySQL root 密码 |
| `SERVER_PORT` | 后端端口，默认 `8000`；改端口需同步 Vite 代理 |
| `APP_CORS_ORIGINS` | 允许的前端来源列表，逗号分隔 |
| `APP_SEED_DEMO` | 空库是否插入演示场景，默认 `true` |
| `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` | 可选的兼容 Chat Completions / Structured Outputs 的 AI 服务；无密钥时使用 mock |
| `DEMO_DB_PATH` | H2 demo profile 的数据库文件前缀 |

AI 调用使用受约束的 JSON Schema 输出，设计参见 [OpenAI Structured Outputs 官方文档](https://developers.openai.com/api/docs/guides/structured-outputs)。真实密钥路径尚未在本机调用验证。

## API 与数据

所有接口以 `/api/v1/decisions` 为前缀：

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/` | 场景摘要列表 |
| POST | `/` | 创建场景；表单字段见下方 |
| GET | `/{id}` | 场景、结构化条件、节点关系与当前有效结果 |
| PUT | `/{id}` | 保存当前条件草稿，修订号加一；旧版本不删除 |
| POST | `/{id}/parse` | `{ "input": "..." }` → 候选条件；不会自动改写场景 |
| POST | `/{id}/simulate` | 规则计算两方案、解释并保存不可变版本 |
| GET | `/{id}/versions` | 版本及快照，按新到旧 |
| POST | `/{id}/compare` | `{ "leftVersionId": "...", "rightVersionId": "..." }` → 条件与结果差异 |
| POST | `/{id}/restore` | `{ "versionId": "..." }` → 将快照恢复为当前草稿；需重新推演保存新分支 |

创建/更新字段为 `title`、`goal`、`timeLimitDays`、`budgetYuan`、`peopleCount`、`hasServer`、`changeRequest`、`resources[]`、`relations[]`。资源可填写 `type/label/quantity/unit`；关系可填写 `id/from/to/label/confidence/assumption`。返回的节点和关系均包含来源、置信度、假设。当前条件与推演结果分别存储：`PUT` 持久化草稿，`simulate` 才生成版本；前端保存表单或确认候选后会紧接着调用 `simulate`。若推演失败，草稿保留，页面可重试，不会生成失败版本。

Flyway V1 创建 `decision_scenarios`、`decision_constraints`、`decision_resources`、`decision_nodes`、`decision_relations`、`decision_versions`、`decision_simulations` 七张表。版本保留完整场景快照及两项推演结果的 JSON；计算结果同时标注 `source=rule`，解释标注 `explanationSource=ai|mock`。

## 可解释规则

- 基准工作量为 72–90 人日；增加视觉/图像/识别功能额外 27–38 人日，其他变更额外 18–30 人日。每人每天按 65% 有效产能，工期取 `ceil(人日 ÷ (人数 × 0.65))`；无现有服务器加 8 天。
- 人日成本暂按 600 元；成本上限另加 5000 元预留，无服务器再加 8000 元。区间向上取整到千元。变更方案高负载人力区间上限增加一名临时协作者。
- 工期上限大于期限的 125%，或成本上限大于预算的 120%，判为高风险；超过期限/预算或缺少服务器为中风险，否则低风险。假设、资源占用和后续影响随结果返回。
- 这些参数仅用于可解释的演示规则；真实项目需以任务分解、团队能力、市场报价重新校准。改变预算或期限主要改变风险，不会凭空改变工作量或成本估算。

验收案例“3 人、10 万元、60 天、已有服务器，增加视觉识别功能”计算：基准 37–47 天、4.4–5.9 万元；变更 51–66 天、6.0–8.2 万元，风险中。将期限改为 40 天、预算改为 5.5 万元后变更风险为高；两次推演可在版本面板比较，恢复旧版后可生成新分支。

## 验证与状态

```bash
cd backend
mvn test
cd ../frontend
npm run build
npm run test:e2e
```

`test:e2e` 需要运行中的前后端，以及 Chrome（默认 Windows 安装路径；也可设置 `CHROME_PATH`）。可用 `DECISION_ORIGIN` 和 `DECISION_VISUAL_DIR` 覆盖 URL 与截图位置。浏览器测试覆盖创建、5 项候选确认、规则变化、版本比较/恢复、空数据、模拟计算失败、键盘 Enter/Escape；在 `1280×720` 与 `360×800` 量测页面横向溢出，并输出截图到 `frontend/test-results/decision-e2e/`。测试会在演示库中创建场景，不会自动删除。MySQL 与真实 AI 密钥路径仍需在对应环境联调。
