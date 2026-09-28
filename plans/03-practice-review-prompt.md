# 模块三开发文档：实践验证与复盘

> 版本：v1.0 · 2026-09-28
> 状态：本文保留开发需求与合同；P0 和决策来源只读关联已实施，实际验证范围见 [验收记录](05-practice-review-acceptance.md)。能力证据写入与反馈消费仍待联调。
> 适用人员：模块三开发者、前后端集成人和验收人员。
> 本文接替 `03-module3-pending-prompt.md` 中的占位与预研任务；现有技术栈、视觉合同和其他模块的数据所有权继续适用。

## 1. 产品目标与模块边界

模块名称：**实践验证与复盘**。用户完成一项真实实践后，提交成果材料和实际投入，逐项检查材料能否支持验收目标，补充缺失证据，并把确认后的结果用于后续能力更新和决策参考。

第一版服务大学生和个人开发者的技术项目实践，主案例是“独立完成可复现的网络服务部署”。支持用户自行建立项目，关联前两个模块是可选入口。

| 模块 | 核心对象 | 负责的问题 | 数据边界 |
|---|---|---|---|
| 能力成长 | 技能、掌握程度、前置关系、成长路径 | 当前能力与目标之间缺什么 | 模块一维护能力状态和证据索引 |
| 决策沙盒 | 场景、约束、方案、推演版本 | 不同选择的投入与风险如何变化 | 模块二维护条件、推演规则和原始版本 |
| 实践验证与复盘 | 实践项目、验收项、证据、复盘版本 | 已有材料支持哪些结果，实际投入偏离预期多少 | 模块三维护材料、判定依据、投入记录和确认后的反馈 |

模块三的独立价值是保存“验收标准—成果证据—检查结果—原方案—实际投入”的关联，使用户可以补交材料、检查前后变化、追溯判断依据。能力图谱仍由模块一展示，方案推演仍由模块二计算。

## 2. 开发前必须核对的仓库事实

先阅读 [统一技术合同](00-shared-integration.md)、[视觉合同](00-ui-style-contract.md) 和 [合并验收表](04-integration-review.md)。开发页面前，还需查看现有首页、能力页和对应 CSS 的实际效果。

以下是编写本文时的现状，开工时应再次检查：

- 前端为 Vue 3、TypeScript、Vite、Vue Router、Pinia；后端为 Java 21、Spring Boot 3.5、JPA、Flyway。
- 当前正式路由只有 `/`、`/ability`、`/decision`；`/module3` 尚未实现。
- `ModuleId` 已包含 `module3`；首页 fallback 和后端模块清单中的第三模块仍为 `pending`。
- 模块一已有 `SkillNode.evidenceIds` 和 `Evidence` 类型，但服务端能力持久化、新增证据接口尚未完成。不能将浏览器 fallback 当成服务端接口。
- 模块二已有 `/api/v1/decisions`、场景详情和 `/{id}/versions` 接口，可读取带方案结果的历史版本。
- 决策结果的 `timeRange` 单位是项目天数，`budgetRange` 为规则估算成本；个人投入小时与现金支出不能直接拿来比较。
- 当前只有 `V1__decision_sandbox.sql`。新迁移使用当时下一个空闲版本号，不修改已执行的迁移。
- 可复用 MySQL 配置和文件型 H2 演示 profile。模块三的业务数据存储在后端。

## 3. 首版范围与交付层次

### P0：独立可运行的最小版本

必须完成：

1. 创建、编辑、切换实践项目；从预置模板建立可修改的验收清单。
2. 输入目标后由 AI 提出候选验收项，用户确认后保存。
3. 粘贴文本说明、配置片段和日志作为证据；支持编辑、移除和关联验收项。
4. AI 按验收项返回结构化检查结果、证据引用和下一步补验建议。
5. 补充材料后重新检查，保留两次复盘的证据快照和判定变化。
6. 手工记录预期与实际投入，在同一口径下计算差异。
7. 用户确认反馈后，保存一份可复制的能力证据建议和决策参考记录。
8. 后端持久化、无 AI Key 的确定性 mock、加载/错误/空状态、桌面与手机布局。

P0 必须能独立完成“创建项目 → 确认验收项 → 提交材料 → 检查 → 补交 → 再检查 → 保存反馈”。其他模块不可用时，该流程仍然成立。

### P1：三个模块的联调

- 从模块二选择场景、历史版本和具体方案，保存不可变来源快照。
- 从模块一读取能力节点，将确认后的实践证据通过其公开接口关联到指定能力。
- 能力等级调整单独展示旧值、建议值、理由，由用户确认；接收方接口须支持校验和幂等。
- 在后续决策页面展示已关联的历史实践记录，供用户修改新方案条件时参考。

P1 依赖模块一补齐后端接口，以及模块二负责人提供历史实践参考入口。依赖未完成时，明确展示“反馈已保存，尚未写入能力模块”或“已保存为决策参考”。不得标记为“已同步”。

### 后续再考虑

文件批量解析、图片 OCR、在线执行用户代码、自动访问项目地址、Git 仓库持续同步、多人审阅、任务排期、证书签发、跨项目自动校准模型均不属于首版。首版只需一个部署模板；接口项目模板可在主流程验收后补充。

## 4. 典型用户流程

### 4.1 创建项目与确定基线

用户填写项目名称、目标和选用模板。可手填预期投入，也可在 P1 选择决策来源。来源可为空，不阻塞创建。

进入实践前确认验收项与预期投入，记录 `baselineRecordedAt`。若项目已经完成后才补填预期，标记为“事后回忆”，不可展示成事前预测。

验收项包含名称、可观察的标准、建议提交的材料、是否必需。AI 输出先出现在候选区，用户可编辑、取消勾选和确认。

### 4.2 提交证据

证据类型限定为 `note`、`config`、`log`。每份证据有标题、正文、记录时间和用户选择的关联验收项；AI 可以建议关联，但不得覆盖用户选择。

外部链接可作为备注保存，但首版不读取链接内容。仅有链接时，AI 不得声称已经检查链接中的成果。

### 4.3 检查与补充

用户点击“检查材料”。后端冻结当前输入快照，AI 对每一项返回结论和引用。点击引用可定位到对应证据版本的行号。

用户可补交材料或纠正判断。修改目标、验收标准、证据、基线或实际投入后，旧复盘标记“输入已变化，需重新检查”，保留原结果供历史查看。

### 4.4 确认复盘与保存反馈

用户逐项接受或修正结论后点击“确认复盘”。修正必须记录理由，保留原 AI 建议。

随后选择需要保存的反馈：能力证据建议、待补验事项、投入差异及用户说明。保存成功后展示真实投递状态。P0 保存于模块三，可复制结构化摘要；P1 只有收到接收模块成功回执后才展示“已写入”。

## 5. 验收语义与业务规则

### 5.1 判定状态

| 状态值 | 页面文案 | 含义 |
|---|---|---|
| `supported` | 材料支持 | 当前材料对该验收项提供了具体支持证据 |
| `insufficient` | 证据不足 | 缺少材料、材料不完整，或无法确定 |
| `conflicting` | 存在矛盾 | 材料中有相互冲突或与验收要求冲突的明确内容 |

未运行检查时，验收项显示“待检查”，不制造默认判定。三个状态描述材料支持程度，不等同于独立验证真实环境或认证个人能力。

- `supported` 和 `conflicting` 必须至少引用一段有效证据。
- `insufficient` 可无引用，但必须解释缺少什么，并给出可执行的补验步骤。
- 没有 HTTPS 材料意味着“证据不足”；明确的 TLS 失败日志才可能支持“存在矛盾”。
- 出现关键词不能直接通过。例如日志包含 `HTTPS failed`，不得因为出现 `HTTPS` 就判为支持。
- 当前材料不能证明是谁独立完成的，因此不自动提升掌握等级。
- 汇总只显示“必需项中有多少项材料支持”和具体缺项，不生成综合能力分、成功概率或排名。
- 人工将结论改为 `supported` 或 `conflicting` 时，同样需要选择有效证据引用；修正理由与引用一起保存，并标注为人工判断。

### 5.2 输入版本与复盘历史

- `inputRevision` 是项目输入版本号。目标、验收项、证据、基线及投入记录的有效修改在同一事务内使其加一。
- 检查前要求客户端提交当前版本号。后端冻结所有输入，保存为不可变 `ReviewSnapshot`。
- AI 调用在数据库长事务外执行；结果保存时再次检查版本。运行期间输入已变化时，可保留旧快照结果，但标记为过期，不能作为当前可确认复盘。
- 复盘创建、确认和反馈投递不改变输入版本，各自记录独立时间和状态。
- 只有当前输入版本下的复盘可以首次确认。已确认历史保留；后续输入变化不会重写其内容。
- 编辑证据后，旧复盘仍展示旧证据正文和旧行号；移除证据仅影响当前输入。
- 两次复盘对比按验收项稳定 ID 匹配；标准文本变更时标注“标准已修改”，不能把它直接当作结果改善。

### 5.3 投入比较规则

每项记录必须有 `metric`、`unit`、`scope` 和 `basis`。只有四者完全相同，且属于同一实践范围时才计算差异。

| 指标 | 单位 | 口径示例 |
|---|---|---|
| `effort` | `hour` | 本人的有效操作时间；不含等待 |
| `duration` | `day` | 项目日历持续时间；具体起止口径写入 basis |
| `cost` | `CNY` | 现金支出，或含人工的完整成本；二者分别记录 |

规则如下：

- 预期可为单值或区间 `[min,max]`，实际为一个非负数；单值转换为 `min=max`。
- 程序计算 `actual-min`、`actual-max`，并返回 `below`、`within`、`above`，边界相等属于区间内。
- 单值预期大于零时可显示百分比差异；区间不显示单一“误差百分比”。
- 缺失值与零不同。预期为零时，百分比为 `null`，展示“无可用百分比基线”。
- 金额用后端 `BigDecimal`，保留两位小数；不由模型完成算术。
- 个人小时不转换成项目天数；服务器账单不直接比较包含人日成本的沙盒预算。
- 来源不兼容时展示两组原始记录和不可比原因，差值为 `null`。
- AI 可根据用户记录提出可能原因，标记为“待确认原因”；用户说明与 AI 推测分别保存。
- 一次实践不自动修改全局推演参数。历史记录用于人工参考，多次数据积累后的校准另行设计。

## 6. 页面与交互设计

界面类型为 **Operate**，复盘历史局部使用 **Compare**。遵守现有 token、Lucide 图标、页头和按钮样式。新样式统一以 `.practice-*` 开头。

建议主路由 `/module3`，详情路由 `/module3/:projectId`。保持 manifest 的 `id: 'module3'`，标题为“实践验证与复盘”。实现前状态保持 `pending`；核心流程可运行后为 `prototype`，验收完成后才改 `ready`。

页面顺序：

```text
统一吸顶页头：返回 / 模块名 / 保存状态 / 新建实践
项目切换与概览：目标 / 来源 / 当前复盘状态
验收标准：模板选择 / 候选确认 / 编辑入口
证据与检查工作区
  桌面：验收列表与结果为主区；证据原文和引用为侧栏
  手机：验收列表 → 选中项详情 → 证据原文，按需展开
投入对比：预期 / 实际 / 口径 / 差值或不可比原因
复盘历史：版本选择 / 变更对比
反馈确认：待补验事项 / 能力证据建议 / 决策参考 / 保存状态
```

- 每个局部工作区一个主要操作；工作流依次为“确认标准”“检查材料”“确认复盘”“保存反馈”。
- 支持项目、验收项和证据的空状态；AI 检查时保留已保存输入和上次结果，显示加载状态。
- 过期结果显示明确提示，首次确认按钮禁用；用户可阅读旧版本和重新检查。
- 结果列表用状态文字、图标和依据表达信息；引用点击后高亮原文行。
- 历史视图显示检查来源、输入版本、确认时间和人工修正理由。
- 无需引入图谱或图表库；列表、证据详情、投入表和版本时间线足够。
- 在 `1280×720` 和 `360×800` 验收；代码片段仅在容器内滚动，页面不横向溢出。

## 7. 数据结构

业务类型放在 `frontend/src/modules/practice-review/types.ts`，Java 使用同语义 DTO/record。只有跨模块传递的摘要类型进入 `frontend/src/core/types.ts`。

下面为核心合同，列表摘要和表单 DTO 可按字段子集定义：

```ts
type Verdict = 'supported' | 'insufficient' | 'conflicting'
type ResultSource = 'ai' | 'mock'

interface Criterion {
  id: string
  title: string
  standard: string
  expectedEvidence: string
  required: boolean
  order: number
}

interface Evidence {
  id: string
  revision: number
  title: string
  kind: 'note' | 'config' | 'log'
  content: string
  criterionIds: string[]
  updatedAt: string
}

interface Citation {
  evidenceId: string
  evidenceRevision: number
  startLine: number // 1-based，包含该行
  endLine: number   // 1-based，包含该行
  quote: string
}

interface Finding {
  criterionId: string
  verdict: Verdict
  reason: string
  citations: Citation[]
  nextAction: string | null
}

interface InputMetric {
  metric: 'effort' | 'duration' | 'cost'
  unit: 'hour' | 'day' | 'CNY'
  scope: string
  basis: string
  expected: { min: number; max: number } | null
  actual: number | null
  baselineTiming: 'before_practice' | 'retrospective'
  baselineRecordedAt: string | null
}

interface DecisionOrigin {
  scenarioId: string
  versionId: string
  optionKey: 'baseline' | 'changed'
  capturedAt: string
  snapshot: {
    title: string
    goal: string
    optionName: string
    timeRange: { min: number; max: number; unit: string }
    budgetRange: { min: number; max: number; unit: string }
    assumptions: string[]
  }
}

interface PracticeProject {
  id: string
  title: string
  goal: string
  templateId: string
  inputRevision: number
  criteria: Criterion[]
  evidence: Evidence[]
  metrics: InputMetric[]
  processNote: string
  decisionOrigin: DecisionOrigin | null
  createdAt: string
  updatedAt: string
}

interface ReviewSnapshot {
  id: string
  projectId: string
  number: number
  inputRevision: number
  input: PracticeProject // 不可变输入副本
  findings: Finding[]   // 原始 AI/mock 结论，不被人工修正覆盖
  source: ResultSource
  providerNotice: string | null
  createdAt: string
  confirmation: {
    confirmedAt: string
    overrides: Array<{
      criterionId: string
      verdict: Verdict
      reason: string
      citations: Citation[]
    }>
  } | null
}

interface FeedbackRecord {
  id: string
  projectId: string
  reviewId: string
  target: 'ability' | 'decision'
  targetId: string | null
  targetName: string | null // 能力反馈的拟关联技能名称；决策反馈可为场景名称
  title: string
  note: string
  criterionIds: string[]
  evidenceIds: string[]
  delivery: 'reference_only' | 'pending' | 'applied' | 'failed'
  externalRecordId: string | null
  confirmedAt: string
}
```

补充约束：

- ID 由后端生成。首版模板 ID 固定为 `network-service-v1`。
- `scope` 和 `basis` 使用预设选项或已确认的相同标识比较，不让 AI 以语义相似为由自动认定口径一致。
- `isStale = review.inputRevision !== project.inputRevision`，为返回时派生值；不信任客户端传入值。
- `confirmation.overrides` 只记录用户修改，其余项保留原判定。确认后锁定该复盘；进一步修改通过新复盘体现。
- 投入比较结果由服务端根据 `metrics` 派生，响应包含可比性、原因、区间位置和差值。
- `delivery` 在 P0 只能为 `reference_only`，页面显示“已保存参考”；P1 才启用投递状态。
- 时间戳采用 ISO 8601；后端存储 `Instant`，前端按本地时区显示。

## 8. 后端持久化与目录

建议新增以下五张表，使用模块内外键，不建立指向其他模块业务表的强外键：

| 表 | 核心内容 |
|---|---|
| `practice_projects` | 名称、目标、模板、输入版本、投入 JSON、过程说明、来源快照 JSON、时间戳 |
| `practice_criteria` | 项目 ID、验收标准、必需标记、顺序 |
| `practice_evidence` | 项目 ID、版本、类型、正文、关联验收项 JSON、更新时间 |
| `practice_reviews` | 项目 ID、版本号、输入版本、执行状态、完整输入/结果 JSON、来源、确认记录 JSON、请求 ID |
| `practice_feedback` | 复盘 ID、目标模块/对象、确认内容 JSON、投递状态、外部回执、请求 ID |

证据当前版本保存在证据表，历史正文随复盘快照保存，首版不单独建设通用文件版本系统。项目编辑和输入版本增加在事务内完成；AI 调用不持有数据库锁。

为 `(project_id, review_number)`、复盘请求 ID 和反馈请求 ID 建唯一约束；反馈还应避免同一复盘同一目标被重复创建。使用 `LONGTEXT` 或在 MySQL/H2 均验证可容纳最大输入的等价类型存正文与快照，不能沿用容量不足的普通 `TEXT`。

发起检查时先以短事务预留 requestId、输入快照与 `running` 状态；保存结果后改为 `succeeded`，不可恢复的失败记为 `failed`。只把 succeeded 记录作为可确认的 ReviewSnapshot；版本号允许有间隔。持久化开始时间，超过检查超时和保存余量仍停留 running 的记录视为中断，通过原 requestId 重试时可原子重占并继续，不能永久阻塞。失败或中断重试继续使用原输入快照；输入已变化时生成的结果仍为过期结果。

```text
frontend/src/modules/practice-review/
  PracticeReviewPage.vue
  types.ts
  services/practiceService.ts
  stores/practiceStore.ts
  components/ProjectEditor.vue
  components/CriteriaEditor.vue
  components/EvidenceEditor.vue
  components/ReviewFindings.vue
  components/MetricComparison.vue
  components/ReviewHistory.vue
  components/FeedbackPanel.vue
  README.md

backend/src/main/java/com/ican/assistant/modules/practicereview/
  PracticeController.java
  PracticeService.java
  PracticeDtos.java
  PracticeRules.java
  PracticeDemoSeed.java
  实体与 Repository

backend/src/main/java/com/ican/assistant/core/ai/
  PracticeAiGateway.java
  MockPracticeAiProvider.java
```

复用现有应用入口、数据库配置和错误响应形式。不要重新创建 Spring Boot 工程或通用模块框架。仅对新增错误类型补充现有异常处理，保留模块二行为。

## 9. API 合同

统一前缀 `/api/v1/practice`，成功响应直接返回 DTO，与现有模块风格一致。以下均为拟新增接口。

| 方法与路径 | 输入 | 输出与行为 |
|---|---|---|
| `GET /templates` | 无 | 预设模板及验收项；无需调用 AI |
| `GET /projects` | 无 | 项目摘要列表，按更新时间降序 |
| `POST /projects` | title、goal、templateId | `201` + 项目；初始验收项复制模板 |
| `GET /projects/{id}` | 路径 ID | 完整项目、派生投入比较、最近复盘状态 |
| `PUT /projects/{id}` | inputRevision、title、goal、metrics、processNote | 更新上述字段；不接受客户端覆写来源快照 |
| `POST /projects/{id}/criteria-suggestions` | inputRevision | 候选验收项、来源、假设；不写入清单 |
| `PUT /projects/{id}/criteria` | inputRevision、完整已确认验收项数组 | 保存清单；保留未变化项的 ID |
| `POST /projects/{id}/evidence` | inputRevision、title、kind、content、criterionIds | `201` + 更新后的项目 |
| `PUT /projects/{id}/evidence/{evidenceId}` | inputRevision、证据可编辑字段 | 更新证据版本，返回项目 |
| `DELETE /projects/{id}/evidence/{evidenceId}?inputRevision=N` | 当前输入版本 | 从当前项目移除，返回项目；历史快照保留 |
| `POST /projects/{id}/reviews` | inputRevision、requestId | `201` + 不可变复盘，明确返回是否过期 |
| `GET /projects/{id}/reviews` | 无 | 复盘摘要及确认状态 |
| `GET /projects/{id}/reviews/{reviewId}` | 无 | 完整快照、引用、派生过期状态和投入比较 |
| `POST /projects/{id}/reviews/{reviewId}/confirm` | inputRevision、overrides | 确认当前复盘；相同内容重试返回原确认 |
| `POST /projects/{id}/feedback` | reviewId、requestId、用户确认的反馈项；能力反馈含拟关联技能名称 targetName | `201` + 反馈记录；P0 为 reference_only |
| `GET /projects/{id}/feedback` | 无 | 已确认反馈及真实投递状态 |
| `POST /projects/{id}/decision-origin`（P1） | inputRevision、scenarioId、versionId、optionKey | 后端读取并冻结方案，返回项目 |

所有修改当前输入的接口都要求 `inputRevision`，成功返回新版本。P0 用同步检查接口即可，AI 总等待上限建议 30 秒，客户端超时留出保存结果的余量；无需引入消息队列。

`requestId` 由前端每次主动操作生成 UUID；网络失败重试复用原 ID。同一个 ID 和相同请求返回原结果，ID 相同但载荷不同返回 `409`。运行中的同 ID 请求返回 `409 REQUEST_IN_PROGRESS`，避免并行生成重复版本。首次确认内容不同的二次确认返回 `409 REVIEW_ALREADY_CONFIRMED`。

### 校验与错误

- title：1–120 字符；goal：1–1000 字符；过程说明最多 5000 字符。
- 每项目最多 12 项验收标准；每项名称最多 120 字符，标准和材料要求各最多 1000 字符。
- 每项目最多 10 份证据；单份正文最多 12000 字符，总正文最多 60000 字符。统一按 Unicode 码点计数，前后端规则一致；HTTP body 上限另按 UTF-8 字节配置到足够容纳合法请求。
- 正文换行在保存时统一为 LF，再计算引用行号；证据 revision 随正文或元数据的有效修改增加。
- 引用和关联 ID 必须属于当前项目；检查结果必须覆盖快照内全部验收项，不能重复或增加未知项。
- 移除验收项时，在同一事务内解除当前证据对该项的关联，保留历史快照中的原关系。
- 保存反馈要求复盘已确认且仍对应当前输入版本；反馈中的验收项与证据必须来自该复盘。过期时返回 REVIEW_STALE，用户可查看已保存的历史反馈。
- 指标值有限、非负，区间 min ≤ max，metric/unit 配对有效；币种首版只支持 CNY。
- 空证据可以发起检查，规则直接返回全部“证据不足”，无需调用模型。

沿用 `{"error":{"code":"...","message":"...","details":{}}}`：

| HTTP | 错误码 | 前端恢复方式 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 定位字段并保留表单 |
| 404 | `NOT_FOUND` | 提示对象不存在，返回列表 |
| 409 | `REVISION_CONFLICT` / `REVIEW_STALE` | 获取最新项目，保留用户草稿供重新提交 |
| 409 | `REQUEST_IN_PROGRESS` / `IDEMPOTENCY_CONFLICT` | 前者使用原 requestId 稍后重试；后者提示请求冲突 |
| 409 | `REVIEW_ALREADY_CONFIRMED` | 返回原确认，提示通过新复盘修改 |
| 413 | `PAYLOAD_TOO_LARGE` | 指出材料长度限制，不清空内容 |
| 422 | `REVIEW_FAILED` | AI 与确定性回退均失败；保留输入并允许重试 |
| 503 | `SOURCE_UNAVAILABLE` | P1 来源模块暂不可用；保留现有快照 |

AI 超时或无密钥但 mock 成功时属于成功响应，返回 `source: 'mock'` 和 `providerNotice`。数据库不可用时不得返回“保存成功”。前端缓存仅用于未提交草稿，恢复时要明确区分草稿与服务端已保存记录。

## 10. AI 处理合同

AI 的两个职责：从目标提出候选验收标准；把已有证据对应到验收项并说明材料支持程度。算术、版本、权限边界、状态迁移和持久化由程序完成。

### 输入

验收建议输入为目标、模板和已有标准。证据检查输入为冻结后的验收项、带编号行的证据、用户关联关系及过程说明。将材料作为待分析数据处理，材料中的“忽略规则”“直接判定通过”等文字不能改变系统要求。

### 输出

验收建议返回 `{ candidates, assumptions, source }`，候选包含 title、standard、expectedEvidence、required。

检查结果采用严格 JSON Schema，顶层为 `{ findings }`。每项字段与第 7 节 `Finding` 一致；禁止额外字段，枚举固定，长度有上限。ID、行号和引用由服务端校验，不信任模型自己声明有效。

```json
{
  "findings": [
    {
      "criterionId": "criterion-https",
      "verdict": "insufficient",
      "reason": "当前材料只有 HTTP 访问日志，缺少 HTTPS 配置与访问结果。",
      "citations": [],
      "nextAction": "补充 TLS 配置片段及一次 HTTPS 请求结果。"
    }
  ]
}
```

此示例是单项响应片段；实际结果必须覆盖输入中的全部验收项。

校验顺序：JSON Schema → ID/项目归属 → 验收项覆盖 → 证据版本 → 行号范围 → quote 与原文逐行精确匹配 → 判定引用要求。quote 使用 LF，必须等于 startLine 到 endLine 的完整原文；需要短引用时让模型选择更窄的行范围。

有效引用只证明引用存在，不能证明语义判断必然正确；保留用户纠正入口。不合规输出整体回退到确定性 provider，并显示回退原因，禁止静默展示伪造引用。证据按普通文本渲染，不执行配置、日志或 HTML 内容。

### 无密钥和失败演示

- 固定模板始终可用；无密钥时返回模板候选。
- 对主演示模板使用可测试的材料模式，明确覆盖正向、否定和缺失案例。
- 同一输入在 mock 下产生相同 findings，版本 ID 和创建时间除外。
- 对无法识别的自由标准、模糊日志和任意材料返回“证据不足”，不默认通过。
- mock 返回的引用必须由实际匹配位置生成；材料变化后重新匹配，不按演示步骤硬编码状态。
- 真实 AI 与 mock 使用相同 DTO，但页面始终展示结果来源。
- 超出所配置模型的输入容量时明确回退并提示，不静默截断已接收材料后声称完成全量检查。

## 11. 跨模块集成合同

### 11.1 模块二 → 模块三：原方案快照

P1 通过既有公开 service 或明确的只读适配接口获取场景及版本，不直接查询模块二 Repository。当前没有单版本读取接口，可从 `GET /api/v1/decisions/{id}/versions` 中选择并验证 versionId。

必须同时选择 `optionKey`；版本含基准与变更两个方案，不能只选版本就默认采用其中一个。快照保存方案名称、工期区间、预算区间、目标和原假设。模块二之后编辑、回溯或重新推演，不改变这份快照。

原范围与实践目标不同或成本口径未确定时，只展示来源供参考；用户在模块三建立兼容的指标基线后才能计算差异。原来源快照不得被手填基线覆盖。

### 11.2 模块三 → 模块一：能力证据

P0 存 `FeedbackRecord`，记录目标技能名称/可选 ID、关联验收项、证据 ID、复盘 ID及用户确认说明。提供复制摘要入口。

P1 由模块一负责人提供证据写入接口，例如 `POST /api/v1/ability/evidence-imports`（拟议，当前不存在），输入包含 `sourceModule: 'module3'`、feedbackId、reviewId、skillId、title、note、证据引用和可选的用户确认等级。

接收方必须验证技能存在，在事务内创建证据并关联技能；按 feedbackId 去重，返回 evidenceId、skillId 和实际更新结果。等级更新需要独立确认和接收方版本校验。模块三收到真实回执后记录 `externalRecordId`；失败保留记录并允许重试。

模块三开发者不直接修改能力模块 store、localStorage、图谱布局或其数据库。接口未提供时，交付状态写明“待联调”。

### 11.3 模块三 → 模块二：实际投入参考

首版反馈保存关联 scenarioId/versionId、比较口径、实际投入、差异和用户说明。模块二可通过模块三公开查询接口展示相关记录；具体入口由模块二负责人集成。

反馈不会改写历史推演，也不会自动改 `DecisionRules` 的工作量、产能或人日成本参数。用户采用反馈时，另建或修改新方案，保留原版本。

### 11.4 共享注册点

需要集成人完成或最小修改的文件：

- `frontend/src/app/router.ts`：新增模块三路由及按需加载。
- `frontend/src/core/api/homeService.ts`：更新入口标题、描述、路由和真实开发状态。
- `backend/src/main/java/com/ican/assistant/api/PlatformController.java`：更新 `/modules` 返回值。
- `frontend/src/core/types.ts`：仅添加确有跨模块用途的摘要类型，保留现有 ModuleId。

以上属于实施时的集成清单；本文发布不改变运行中页面的完成状态。

## 12. 演示数据与验收脚本

### 主案例数据

项目：“独立完成可复现的网络服务部署”。模板含四项必需标准：

1. 基础 HTTP 服务有访问结果。
2. 有 HTTPS 配置和对应访问结果。
3. 有可定位请求或故障的日志记录。
4. 部署说明包含环境版本、启动步骤和验证步骤。

准备 `seed-v1` 材料：HTTP 200 访问记录、普通访问日志、缺少环境版本的部署说明。预期基础访问和日志项为 `supported`，HTTPS 与可复现项为 `insufficient`。材料注明为合成演示样例。

准备 `seed-v2` 补充材料：TLS 配置和 HTTPS 访问结果、完整环境/启动/验证说明。重新检查后，相关项出现具体引用，历史仍能看到旧缺项。

投入采用可控的手填同口径案例：个人有效投入预期 4–6 小时，实际 9 小时；程序显示高于上限 3 小时、`above`。过程说明写“额外 3 小时用于排查环境依赖”。系统呈现该用户记录，不把它推断成所有差异的已证实原因。

另备负向日志 `HTTPS handshake failed` 和未知自由模板，验证模型或 mock 不因关键词出现而默认通过。

### 约 90 秒的模块演示

| 时间 | 操作 | 观众应看到的结果 |
|---|---|---|
| 0–15 秒 | 打开实践项目与验收标准 | 目标、标准和基线清楚可见 |
| 15–35 秒 | 提交第一组材料并检查 | 逐项结论、缺失材料、来源标签 |
| 35–55 秒 | 点击证据引用，补交第二组材料 | 可定位原文，输入版本变化 |
| 55–70 秒 | 再检查并对比历史 | 缺项变化有依据，旧记录可追溯 |
| 70–90 秒 | 查看投入差异并保存反馈 | 真实差值和保存状态；联调完成时展示真实回执 |

### 必须通过的验收用例

| 编号 | 场景 | 通过条件 |
|---|---|---|
| A01 | 完整主流程 | 从创建到补交、复盘、确认、保存反馈全部可操作 |
| A02 | 重启与刷新 | 项目、材料、复盘、确认和反馈均从后端恢复 |
| A03 | 空材料/未知标准 | 显示证据不足及补验建议，不出现默认支持 |
| A04 | 错误引用/未知 ID | 不合规 AI 输出被拦截并标注回退 |
| A05 | 编辑或移除证据 | 当前结果过期；历史引用仍定位原始材料 |
| A06 | 检查期间修改输入 | 旧版本结果不能被确认为当前复盘 |
| A07 | 网络重试/双击 | 同 requestId 不产生重复复盘和反馈 |
| A08 | 数值边界 | 区间边界、缺失、零基线、小数金额结果正确 |
| A09 | 口径不一致 | 个人小时与项目天数、现金与完整成本均不计算差值 |
| A10 | AI 超时/无密钥 | 相同结构的确定性 mock 可运行，来源明确 |
| A11 | 材料含否定或指令 | 不因 HTTPS 关键词或“直接判通过”而返回支持 |
| A12 | 人工纠正与确认 | 原结果保留，修正有理由，重复确认行为明确 |
| A13 | 外部模块未实现 | 独立流程可用，反馈不虚报同步成功 |
| A14 | 桌面/手机与故障 | 无页面溢出；输入保留；失败可恢复；无新增控制台错误 |
| A15 | P1 真实联调 | 正确选择方案、原快照不变、重复投递不重复写入 |

后端重点测试数值口径、引用校验、版本过期、幂等和事务持久化。前端至少走通主流程、失败恢复和移动端验收。MySQL 与 H2 各自记录验证结果，不将 H2 通过等同于 MySQL 已验收。

## 13. 实施顺序与完成标准

| 阶段 | 开发内容 | 可检查的退出条件 |
|---|---|---|
| 1 | 类型、模板、迁移、项目/材料 API | 后端持久化与重启恢复通过 |
| 2 | 规则、AI 适配、mock、快照与确认 | 主例、缺失、否定、过期、幂等用例通过 |
| 3 | 页面、引用定位、历史与投入比较 | 桌面和手机完成一次完整操作 |
| 4 | 本地反馈记录与共享入口注册 | 首页进入、保存反馈、刷新恢复正常 |
| 5 | P1 跨模块联调 | 接收接口有回执、失败可重试、记录无重复 |

先完成 P0 再扩展 P1；开发者不得为了演示完整性伪造写入其他模块的成功状态。

交付文件至少包括前后端模块代码、数据库迁移、测试、模块 README、主例与负例 seed，以及实际的验收记录。README 写明启动方式、环境变量复用项、AI/mock 行为、数据来源、尚未完成的集成和各环境测试结果。

提交前执行前端 `npm run build` 与后端 `mvn test`，按共享验收表记录 `1280×720`、`360×800` 的交互、布局和页面溢出检查。AI 密钥路径、MySQL 或 P1 未验证时如实记录，不标记整体 ready。

可直接交给开发者的起始指令：

```text
请实现模块三“实践验证与复盘”。先阅读 plans/00-shared-integration.md、plans/00-ui-style-contract.md、本任务书和 plans/04-integration-review.md，核对仓库现状并查看首页和能力页。按照本文 P0 范围先做出可独立运行的流程，复用现有 Vue/Spring Boot 工程。业务数据由后端持久化，AI 只生成结构化候选和带引用的判断。保存输入快照，处理证据不足、版本过期、同口径投入对比和重复请求。先验收 P0，再根据真实接口完成 P1；逐项报告实现、测试和待联调内容。保持共享视觉规范与各模块的数据所有权。
```
