# 历史任务书：第三模块待定阶段的可插拔骨架与预研（已归档）

> 2026-09-28：用户已确认采用“实践验证与复盘”方向，当前开发依据为 [模块三开发文档](03-practice-review-prompt.md)。以下保留为历史记录，其中“方向待定”“只做占位”“提交候选方向”等要求已失效。复用仓库已有基础设施，不重复执行本任务书。

你是本项目第三位队员。第三个业务模块目前尚未确定，因此本阶段禁止擅自选择一个完整产品方向，也禁止仿照其他模块重复做能力图谱、决策推演、任务管理、问答或资料总结。

请先完整阅读 `plans/00-shared-integration.md`、`plans/00-ui-style-contract.md` 和 `plans/04-integration-review.md`，再运行并查看 `/` 与 `/ability`。你的目标是为未来第三模块保留稳定入口、验证 Vue3 + Java Spring Boot 模块化架构，并提交一份基于证据的候选方向分析。

## 比赛背景

项目面向 2026 年 iCAN 大学生创新创业大赛 AI 应用创新挑战赛，优先适配软件赛道。比赛要求作品可运行、AI 在具体场景中发挥核心作用，演示视频不超过 5 分钟，应用方案不超过 20 页，提交截止 2026 年 9 月 30 日。评审重点是创新性、技术实现、实用价值、用户体验和展示效果。

## 阶段一：实现前后端基础骨架

负责维护不属于单一业务模块的基础设施：

- `backend/pom.xml` 和 Spring Boot 项目骨架；
- `backend/src/main/java/com/ican/assistant/AssistantApplication.java` Spring Boot 入口；
- `backend/docker-compose.yml`，提供统一的 MySQL 8.4 本地服务；
- `/api/v1/health`、`/api/v1/profile`、`/api/v1/modules` 基础接口；
- Spring Data JPA + MySQL 数据库连接和初始化；
- Java DTO/record 基础 schema；
- Flyway 数据库迁移和初始 seed；
- CORS、环境变量和统一错误响应；
- `backend/src/main/java/com/ican/assistant/core/ai/` 的 AI provider 接口和 deterministic mock provider；
- `backend/README.md`；
- 根目录 `.gitignore`，排除 `.env`、MySQL 数据目录、缓存和密钥。

不要把模块1或模块2的业务表写入基础设施文件；业务模型由对应模块负责人维护。

## 阶段二：实现占位模块

在 `frontend/src/modules/module3-placeholder/` 中实现：

- 模块 manifest：标题“第三模块（待确定）”、状态 `pending`；
- 独立路由 `/module3`；
- 移动端页面，清楚显示当前模块尚未确定；
- 一个可替换的 `Module3Content` 容器；
- 空状态、加载状态和错误状态组件；
- 不要出现具体的业务承诺或伪造功能；
- 不要阻塞主页、模块1或模块2的运行。

占位页也必须属于同一产品：浅灰背景、白色或无框内容面、统一返回按钮、蓝色线性 Lucide 图标、左对齐眉题与标题。占位信息控制在一个紧凑区域，不使用营销 hero、插画、渐变、三张功能卡或不存在的指标。若复用现有 `.placeholder-page`，不要改变共享 token；新样式使用 `.module3-*` 前缀。

在 `backend/src/main/java/com/ican/assistant/modules/module3placeholder/` 中只实现健康检查或空 service，不创建具体业务表。占位模块必须能在未来通过替换内部页面和 Controller，而不修改全局路由和主页模块注册方式。

## 阶段三：验证通用模块接口

补充一个轻量的模块注册机制：

```ts
export interface ModuleEntry {
  manifest: ModuleManifest;
  render: () => JSX.Element;
}
```

主页可以通过 `GET /api/v1/modules` 或前端静态清单读取模块清单，但不要把模块业务逻辑写入主页。第三模块入口的显示由 manifest 的 `status` 控制。

验证以下行为：

- 模块可以单独注册和取消注册；
- 状态为 pending 时显示占位页面；
- 状态为 prototype/ready 时可以替换标题和入口；
- 一个模块发生渲染错误时不会导致全局主页崩溃；
- 移动端页面在 360px 宽度下不溢出。

## 阶段四：候选方向预研

在 `plans/module3-options.md` 中写出 3 个候选模块，每个候选必须按照以下格式：

```text
名称
服务对象
输入信息
AI 处理什么
网页展示什么
无法被一次性问答替代的系统能力
最小可行演示
所需数据及获取难度
与模块1/2的耦合点
对应比赛评分项
主要风险
```

候选方向必须满足：

- 可以使用用户提供的数据或可控 mock 数据；
- AI 是处理链路中的核心，但不能只是聊天、总结、润色或推荐；
- 网页端有明确的可视化结果和交互；
- 与能力成长和决策沙盒保持低耦合；
- 能在 5 分钟视频中展示一个闭环；
- 有明确的原创技术或结构化数据模型。

不要在本阶段实现候选业务功能，也不要通过爬虫或未经授权的数据源增加工作量。

## 阶段五：测试和文档

至少完成：

- 占位模块路由测试或手动验收记录；
- 模块注册和错误隔离的测试；
- 移动端布局检查；
- 模块替换说明；
- `README`，说明如何将占位模块替换为最终业务模块。

## 交付物

- `frontend/src/modules/module3-placeholder/`；
- `backend/src/main/java/com/ican/assistant/modules/module3placeholder/`；
- 模块注册/manifest 机制的必要改动；
- Spring Boot 基础骨架、AI provider 接口、MySQL/Flyway 初始化和基础 API；
- `plans/module3-options.md`；
- 测试或验收记录；
- 启动和集成说明；
- 确认 `npm run build` 成功。
- 按 `plans/04-integration-review.md` 完成 `1280 x 720` 和 `360 x 800` 验收。

## 不要做的事情

- 不要自行拍板第三模块最终方向；
- 不要修改模块1和模块2的内部业务代码；
- 不要把占位页做成一个新的聊天功能；
- 不要引入外部实时数据、登录系统或超出基础骨架范围的复杂后端；
- 不要为了填满页面添加没有业务价值的图表。
- 不要引入第二套色板、UI 框架、图标库或大圆角卡片风格。
