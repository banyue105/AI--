# 模块三实施与验收记录

日期：2026-09-28（Asia/Shanghai）。依据：[模块三开发文档](03-practice-review-prompt.md)、[统一技术合同](00-shared-integration.md)、[视觉合同](00-ui-style-contract.md)、[合并验收表](04-integration-review.md)。

当前结论：P0 独立流程已实现并通过本机验收；P1 的决策版本/方案只读关联已实现。能力证据写入、决策模块消费反馈仍待联调。首页和 `/api/v1/modules` 保持 `prototype`。真实远程 AI、MySQL 8.4、生产部署未验收。

## 1. 实现范围

- 项目创建、目标与过程说明、手工投入记录、标准模板和用户确认的候选清单。
- 说明/配置/日志的新增、编辑、移除、关联标准和版本管理。
- 覆盖全部验收项的材料检查，带证据版本与精确行引用；引用可定位原文。
- 完整输入与结果快照、历史版本、补验比较、过期判定与确认限制。
- 指标/单位/范围/口径一致时的程序比较，区间差异、零基线、缺失与事后基线提示。
- 用户纠正、原判断保留、确认记录、可重试且幂等的参考反馈、复制摘要；能力反馈记录拟关联技能名称，默认摘要包含待补验建议、比较口径与冻结的原方案版本和假设。
- 决策场景、历史版本和具体方案的只读选择；原方案估算与假设冻结保存。
- 无密钥 mock、错误结构或引用的整体回退、超时提示、请求体和材料总量限制。
- 桌面与手机、空状态、错误恢复、临时草稿、键盘焦点和保存期间输入保护。

业务数据以数据库为准；localStorage 仅保存未提交草稿，sessionStorage 仅保存请求重试信息。反馈实际为 `reference_only`，没有伪造接收方回执。

## 2. 验证环境与命令

| 项目 | 本次环境 |
|---|---|
| 系统 | Windows / PowerShell |
| 后端 | Java 21.0.10、Spring Boot 3.5.16、Maven 3.9.11 |
| 数据库 | H2 2.3.232，MySQL 模式；单元/接口测试使用内存库，浏览器验收使用文件库 |
| 前端 | Vue 3、TypeScript、Vite 7.3.6、Node.js 24.14.0 |
| 浏览器 | 本机 Chrome，由 playwright-core 启动 headless 模式 |
| AI | 无密钥确定性 mock；远程调用路径用本机 HTTP 模拟服务测试 |
| 本次地址 | 前端 `http://127.0.0.1:5174`，后端 `http://127.0.0.1:8001` |
| 文件库 | `backend/target/practice-demo-db.mv.db` |

原有 5173/8000 服务已在运行，本次使用独立端口和演示数据库。启动参数与可复用的环境变量见 [模块 README](../frontend/src/modules/practice-review/README.md)。本机未发现可用 Docker/MySQL 命令或 3306 监听，未安装额外数据库，因此不将 H2 结果记为 MySQL 已通过。

执行命令（从对应目录）：

```powershell
# backend；本机使用已存在的 Maven 3.9.11 mvn.cmd 执行
mvn test

# frontend
npm run build
$env:PRACTICE_ORIGIN='http://127.0.0.1:5174'
npm run test:practice
$env:DECISION_ORIGIN='http://127.0.0.1:5174'
npm run test:e2e

# 在后端重启前后分别执行，保持同一文件数据库
node tests/practice-persistence.mjs capture
node tests/practice-persistence.mjs verify
```

## 3. 实测结果

| 验证 | 结果 |
|---|---|
| 前端类型检查和生产构建 | 通过；模块三使用独立的按需加载 JS/CSS |
| 后端完整测试 | 28 项通过，0 失败、0 错误；其中模块三 20 项、已有决策沙盒 8 项 |
| 模块三接口测试 | 6 项通过，包括 Unicode 码点边界和浏览器来源的 DELETE 请求 |
| 模块三浏览器主流程 | 13 组检查通过；创建项目、4 份材料、3 次复盘、人工确认、2 条参考反馈；反馈包含原方案版本与参考边界 |
| 决策沙盒浏览器回归 | 通过；5 项候选、4 个版本、恢复条件和失败状态均可操作 |
| 桌面 1280×720 | 页面横向溢出 0px；主要检查操作在清单顶部可见 |
| 手机 360×800 | 页面横向溢出 0px；弹窗、历史、证据、反馈均可操作 |
| 长中文/长日志 | 两个视口均无页面横向溢出；原文只在自身容器滚动 |
| 页面运行与控制台 | 无新增 pageerror、无未预期 console error；故障用例主动注入的资源失败单独排除 |
| 文件库重启恢复 | 项目、材料、复盘、确认与反馈逐字段相等 |
| V2 → V3 升级保留数据 | 已执行 V3 升级；原项目及历史、确认、反馈与升级前快照逐字段相等 |

后端测试文件位于 `backend/src/test/java/com/ican/assistant/modules/practicereview/`：

- `PracticeRulesTest`：7 项，区间/零基线/小数金额、异口径、Unicode 与行尾、精确引用、证据与验收项关联、否定日志和未知标准。
- `PracticeApiTest`：6 项，完整持久化、过期确认、幂等批次、跨项目归属、移除后的历史引用、错误人工引用、请求体上限、人工修正、来源冻结和字符存储边界。
- `PracticeConcurrencyTest`：3 项，模型等待期间可编辑、旧结果不可确认、失败/中断复用原快照、并发反馈只保存一批。
- `PracticeAiGatewayTest`：4 项，模拟服务的合规结果、伪造引用、超时、错误候选结构；回退来源和提示均明确。

## 4. 需求用例对应

| 编号 | 验收情况 | 依据 |
|---|---|---|
| A01 完整主流程 | 通过 | 真实后端浏览器流程：创建、候选确认、补交、复盘、确认、反馈 |
| A02 刷新/重启恢复 | 通过（H2） | 浏览器刷新及文件库重启后的完整快照比较 |
| A03 空材料/未知标准 | 通过 | 空材料浏览器流程、自由标准的保守规则测试 |
| A04 错误引用/未知 ID | 通过 | 网关阻止伪造 quote，版本、项目归属、验收项关联与行号规则测试 |
| A05 编辑/移除材料 | 通过 | 当前输入递增、旧复盘过期、删除后仍可读取历史原文 |
| A06 检查期间修改 | 通过 | 模型等待时编辑在 3 秒内完成；旧快照结果被标为过期并阻止确认 |
| A07 网络重试/重复请求 | 通过 | 浏览器模拟已完成但响应丢失；同请求返回原复盘。反馈刷新后复用 UUID，并发保存无重复 |
| A08 数值与字符边界 | 通过 | 区间边界、零、小数、负值；最大 emoji 标题/目标/标准/过程说明/正文实际入库 |
| A09 不同口径 | 通过 | 个人小时/项目天数、现金/完整成本不产生差值 |
| A10 无密钥/AI 超时 | 通过（mock 与模拟服务） | 无密钥完整流程、本机服务超时回退；真实远程模型未验收 |
| A11 否定/嵌入指令 | 通过 | HTTPS 失败日志不支持；嵌入“直接通过”不改变规则；HTML 作为普通文本渲染 |
| A12 人工纠正/确认 | 通过 | 原判定保留，修正引用来自快照；待确认改为不足仍有补验提示；确认不可覆盖 |
| A13 外部模块缺接口 | 通过 P0 边界 | 反馈明确显示“已保存参考”，外部记录 ID 为空 |
| A14 两视口与故障 | 通过 | 无页面溢出、长文本、键盘、草稿恢复、失败重试、运行错误检查 |
| A15 P1 全量联调 | 部分通过 | 选定版本/方案与冻结来源已通过；能力写入回执、反馈消费入口尚未提供 |

浏览器长文案与 HTML 用例使用只读响应夹具检查渲染；主流程、复盘和反馈使用实际后端写入。所有演示日志、配置、投入均为合成数据，不能作为真实能力或部署认证。

## 5. 产物与查阅位置

| 产物 | 位置 |
|---|---|
| 前端模块、类型、service、store、组件 | `frontend/src/modules/practice-review/` |
| 业务 API、规则、实体、持久化 | `backend/src/main/java/com/ican/assistant/modules/practicereview/` |
| AI 与确定性检查器 | `backend/src/main/java/com/ican/assistant/core/ai/PracticeAiGateway.java`、`MockPracticeAiProvider.java` |
| 迁移 | `V2__practice_review.sql`、`V3__practice_unicode_capacity.sql` |
| 浏览器验收/重启检查脚本 | `frontend/tests/practice-e2e.mjs`、`practice-persistence.mjs` |
| 主例、补交材料与负例 | `frontend/src/modules/practice-review/examples/` |
| 操作、API、环境变量与未完成联调 | [模块 README](../frontend/src/modules/practice-review/README.md) |

测试生成结果被 Git 忽略，可在本机 `frontend/test-results/practice-e2e/` 查看：`acceptance.json`、`persistence-result.json`、`migration-preservation.json`、桌面部分/完整材料截图、手机主页面/完整页面/编辑弹窗/空状态/长文案截图。运行脚本可重建结果；构建产物、数据库和截图未加入源码。

## 6. 共享变更与视觉复用

最小修改路由、首页入口、模块清单，保留 `ModuleId: module3`。Vite 仅新增可配置代理目标，默认仍为 8000；后端 CORS 增加材料移除所需的 DELETE；配置增加模块三 AI 超时。未改模块一 store、localStorage 数据、能力图谱、决策规则或已执行的 V1/V2 迁移。

复用 `page-title`、`page-icon`、`eyebrow`、`primary-button`、`secondary-button`、`text-button`、`icon-button`、`ai-input-band`、`section-heading`、`editor-dialog`、`modal-backdrop`、`state-panel`、`toast-message` 和共享语义 token。新增 CSS 使用 `.practice-*`，共享 `main.css` 未改。桌面材料侧栏 282px，手机单列。

V3 仅增加物理字符串容量以兼容 H2 的 UTF-16 长度计算，接口上限仍按 Unicode 码点校验；保留已执行的 V2，验证升级过程没有修改业务数据。

## 7. 剩余工作

1. 模块一提供真实能力证据写入接口、技能校验、去重和回执；模块三据此记录 applied / failed 并支持外部投递重试。
2. 模块二接入参考反馈查询与展示；继续保留原推演和参数，由用户显式采用到新方案。
3. 配置实际 AI 服务，在真实材料和自定义标准上验证模型兼容性、引用与语义；当前 HTTP 模拟测试不代表真实模型已通过。
4. MySQL 8.4 执行迁移、完整接口与重启验收，再完成认证和生产部署。

这些工作未记为完成，入口暂保持 prototype。
