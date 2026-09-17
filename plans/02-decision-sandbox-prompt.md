# Codex 任务书：模块2「AI 决策沙盒」

你是本项目的模块2负责人。请先完整阅读 `plans/00-shared-integration.md`、`plans/00-ui-style-contract.md` 和 `plans/04-integration-review.md`，再运行并查看 `/` 与 `/ability`。严格遵守技术基线、目录边界、共享类型、视觉合同和合并规范。你只负责决策沙盒模块，不修改主页、能力成长模块、共享 token 或第三模块业务。

## 目标

开发一个移动端优先的 AI 决策沙盒。用户输入现实中的目标、人员、资金、设备、技术、时间和市场等条件，系统把这些信息结构化，允许用户修改关键条件，比较多个方案的时间、资源、成本和风险后果，并支持回溯和对比。

它不是普通的“哪个方案好”的问答工具，而是：

```text
输入现实条件 → 建立关系 → 修改条件 → 计算结果 → 生成分支 → 比较与回溯
```

## 比赛背景

项目参加 2026 年 iCAN 大学生创新创业大赛 AI 应用创新挑战赛软件赛道候选方向。通知明确要求展示 Agent 如何接收请求、分析决策、反馈结果和协同模块；评分重点为创新性 30 分、技术实现 30 分、实用价值 20 分、用户体验 10 分、展示效果 10 分。应用方案 PDF 不超过 20 页，演示视频不超过 5 分钟，作品提交截止 2026 年 9 月 30 日。

## 目标用户与第一版场景

产品面向需要在资源约束下做选择的个人和小团队。第一版可以用大学生创新项目案例作为演示，但模型不要写死为学生项目。

示例：3 名成员、10 万元预算、60 天周期、已有服务器，用户提出“增加视觉识别功能”，系统比较增加功能与保持原方案的影响。

## 必须实现的功能

### A. 条件录入

支持创建一个决策场景，并录入：

- 目标；
- 时间约束；
- 资金预算；
- 人员数量和角色；
- 设备/技术资源；
- 当前已有条件；
- 可选方案或用户提出的变更。

至少支持自然语言快速录入和结构化表单编辑两种方式。自然语言解析失败时必须允许用户手动修正。数据通过 Spring Boot 后端保存。

### B. 现实状态模型

将词条转为可编辑节点和关系，不要只保留一段摘要。至少支持：

- 资源节点；
- 目标节点；
- 任务/功能节点；
- 约束节点；
- 风险节点；
- 影响关系。

用户可以查看每个节点的来源、假设和置信度。

### C. 方案推演

至少生成两个方案：基准方案和变更方案。每个方案显示：

- 预计工期区间；
- 资金成本区间；
- 人力投入；
- 资源占用；
- 风险等级；
- 关键假设；
- 可能的后续影响。

必须使用可解释的规则/计算逻辑完成数值变化。AI 可以抽取关系和生成解释，但不能无依据地编造精确数字。

### D. 可视化

移动端优先展示：

- 当前条件卡片或关系图；
- 方案分支树；
- 方案对比表；
- 时间线；
- 成本/风险指标；
- 假设条件和警告。

分支图在手机屏幕上需要支持横向滚动、折叠或切换为步骤列表。不要让宽图被强行缩小到不可读。

### E. 回溯与比较

- 保存每次条件修改和推演结果；
- 支持回到上一个状态；
- 支持选择两个方案比较；
- 显示“改变了什么条件，导致了什么结果变化”。

## 后端与 API

在 `backend/src/main/java/com/ican/assistant/modules/decisionsandbox/` 实现：

- 场景、约束、资源、节点、推演结果和版本的 MyBatis 数据模型/Mapper；
- Java DTO/record 请求/响应模型；
- 至少实现以下接口：

```text
GET  /api/v1/decisions
POST /api/v1/decisions
GET  /api/v1/decisions/{scenario_id}
PUT  /api/v1/decisions/{scenario_id}
POST /api/v1/decisions/{scenario_id}/parse
POST /api/v1/decisions/{scenario_id}/simulate
GET  /api/v1/decisions/{scenario_id}/versions
POST /api/v1/decisions/{scenario_id}/compare
```

- `simulate` 使用后端规则/计算 service 生成结果，再由 AI 生成解释；
- 所有结果保存假设、来源和 `source: "ai" | "rule" | "mock"`；
- 没有 AI 密钥时，`parse` 和解释接口使用确定性的 mock，规则计算仍然真实执行；
- Controller 不直接写 SQL，通过 MyBatis Mapper/Service 访问数据库。

在 `frontend/src/modules/decision-sandbox/` 实现 Vue 页面、组件、Pinia 状态和 API service；页面不得直接访问数据库或 AI 服务。

## 数据类型

前端业务代码放在 `frontend/src/modules/decision-sandbox/`，后端业务代码放在 `backend/src/main/java/com/ican/assistant/modules/decisionsandbox/`。可以使用以下前端类型，必要时扩展但不要破坏字段含义：

```ts
export interface DecisionScenario {
  id: string;
  title: string;
  goal: string;
  constraints: Constraint[];
  resources: Resource[];
  nodes: DecisionNode[];
  createdAt: string;
}

export interface Constraint {
  id: string;
  type: 'time' | 'budget' | 'people' | 'technology' | 'market' | 'other';
  label: string;
  value: string;
  source: 'user' | 'ai' | 'system';
}

export interface Resource {
  id: string;
  type: 'person' | 'money' | 'equipment' | 'skill' | 'other';
  label: string;
  quantity?: number;
  unit?: string;
}

export interface DecisionNode {
  id: string;
  label: string;
  type: 'goal' | 'resource' | 'task' | 'constraint' | 'risk';
  source?: string;
}

export interface SimulationResult {
  id: string;
  scenarioId: string;
  optionName: string;
  timeRange: { min: number; max: number; unit: 'day' | 'week' };
  budgetRange?: { min: number; max: number; unit: string };
  riskLevel: 'low' | 'medium' | 'high';
  assumptions: string[];
  impacts: string[];
}
```

## 技术实现要求

- 前端使用 `decisionService.parseInput()`、`decisionService.simulate()`、`decisionService.compare()` 等 service 函数调用 API；
- 后端使用同名或对应的 Spring service 隔离 AI 和计算逻辑；
- 计算引擎先实现可读的规则：例如功能数量增加会影响工期和测试资源，预算不足会提高风险；
- 每个结果保留假设条件和区间，不输出伪精确结论；
- 没有 API Key 时使用确定性的 mock 场景和规则计算；
- 后端 MySQL 保存演示场景和版本，localStorage 只保存界面偏好；
- 使用 Vue Flow 或等价方案绘制节点关系和分支，使用 ECharts 或普通 CSS 展示指标；
- 不添加登录、支付或多人实时协作等非 MVP 功能。

## 页面结构与视觉映射

本模块是 `Operate + Compare` 工作台。不要生成营销页、聊天页、深色数据大屏或满屏统计卡。桌面端按以下顺序实现：

```text
统一吸顶页头
  返回 | 决策沙盒 + 条件推演工作台 | 已保存/版本状态 | 新建场景

场景概览面板
  左：场景标题、目标、最后更新时间
  右：方案数、当前约束数或总体风险，不超过两个指标

AI 条件输入条
  一句话描述变更 -> 结构化 -> 用户确认候选条件

主工作区
  左：现实状态模型/条件关系图
  右：选中节点、来源、置信度、假设和编辑入口

方案切换与比较区
  基准方案 | 变更方案 | 自定义方案
  先显示差异摘要，再显示时间/预算/人力/资源/风险对比

影响时间线与版本历史
  展示改变了什么、为何产生变化、可回溯入口

底部假设说明
```

与能力页的组件对应关系：

| 决策沙盒 | 复用/对齐能力页 |
|---|---|
| 场景概览 | 对齐 `.ability-overview` 的尺寸、边框、8px 圆角和指标布局 |
| AI 条件输入 | 复用 `.ai-input-band`、`.quick-form` 和结构化候选确认流程 |
| 条件图工具栏 | 对齐 `.graph-panel`、`.graph-toolbar`、`.legend`、`.graph-actions` |
| 节点详情 | 对齐 `.node-inspector` 的 282px 侧栏结构 |
| 方案 tabs | 对齐 `.track-switcher` 的分段切换视觉 |
| 比较面板 | 对齐 `.track-content` 的整块白色工作面，内部使用行分隔线 |
| 版本路径 | 对齐 `.path-list` 的局部横向滚动和步骤标记 |
| 编辑弹窗 | 复用 `.modal-backdrop`、`.editor-dialog`、`.segmented-control` |
| 反馈状态 | 复用 `.state-panel`、`.spinner`、`.toast-message` |

新增类统一使用 `.decision-*` 前缀。允许复用公共类，但不要把 `.ability-*`、`.graph-*` 等模块私有类直接写进决策组件；应使用 `.decision-*` 实现相同的视觉参数。禁止复制或修改 `:root` token。主要按钮蓝色；低风险/成功绿色；中风险/注意琥珀色；未计算灰色；高风险用警告图标、清晰文案和克制边框表示，不使用大面积高饱和红底。

桌面主工作区可采用 `minmax(0, 1fr) 282px`。在 900px 以下变成单列，详情面板移到图后；在 620px 以下，页头主操作只显示图标，输入和按钮纵向堆叠，方案 tab、比较表和时间线仅在自身容器横向滚动。必须保证 360px 页面本身不横向溢出。

## 验收案例

1. 创建“60 天完成 AI 网页项目”的场景。
2. 录入 3 名成员、10 万元预算、已有服务器等条件。
3. 输入“增加视觉识别功能”。
4. 页面生成基准方案和变更方案。
5. 变更方案显示工期、资源、预算和风险的区间变化，并展示假设。
6. 用户修改预算或剩余时间，结果重新计算。
7. 用户打开历史版本，比较两次推演的差异。
8. 关闭 AI 配置后，后端 mock 场景仍然可完整演示；
9. 重启后端后，场景和历史版本仍然存在。

## 与其他模块的集成

只通过共享的 `Goal`、`Resource`、`ModuleManifest` 或明确的 service/event 接口集成。可以提供“导入能力缺口”按钮，但不能直接访问模块1的内部 store。第三模块待定，不要为其预留具体业务依赖。

## 交付物

- Vue 3 + TypeScript 决策沙盒页面和移动端布局；
- 条件录入、关系模型、规则计算、分支图和方案比较；
- Spring Boot API、MyBatis 数据模型、规则计算 service、Java DTO/record 和 mock seed 数据；
- 前端 API service 和后端接口测试或手动验收记录；
- 至少覆盖条件变更、空数据、计算失败和数据库重启恢复；
- 模块 README，包括前后端运行方式、数据结构、规则假设、环境变量和集成点；
- 提交前确认 `npm run build`、后端启动和测试成功，并列出自己新增的依赖；
- 按 `plans/04-integration-review.md` 记录 `1280 x 720` 和 `360 x 800` 视觉验收、页面溢出检查以及复用/新增的 CSS 类。

## 不要做的事情

- 不要做普通聊天机器人；
- 不要让大模型直接决定所有数值；
- 不要声称预测结果是事实或保证；
- 不要把模块做成企业 ERP 或完整项目管理软件；
- 不要修改主页和能力成长模块内部代码；
- 不要使用未经授权的第三方数据或素材；
- 不要修改共享颜色和公共类来适配本模块；不要添加渐变、光球、大圆角、卡片套卡片、装饰性玻璃或自绘 SVG 图标。
