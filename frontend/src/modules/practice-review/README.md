# 模块三：实践验证与复盘

当前状态为 `prototype`：P0 独立流程已实现；决策沙盒的版本/方案只读关联已实现。能力证据写入、决策模块中的反馈展示仍待接收方接口和入口联调。实际验收结果见 [验收记录](../../../../plans/05-practice-review-acceptance.md)，需求依据见 [开发文档](../../../../plans/03-practice-review-prompt.md)。

## 已实现的流程

1. 从首页进入 `/module3`，创建实践项目；详情地址为 `/module3/:projectId`。
2. 使用网络服务部署模板，编辑清单，或生成候选标准后由用户确认。
3. 添加说明、配置、日志，关联一项或多项标准；每份材料均有独立版本。
4. 检查材料，逐项显示“材料支持 / 证据不足 / 存在矛盾”、依据和补验建议。
5. 点击引用，定位保存时的原始行；可切换当前材料与复盘材料快照。
6. 补交材料后重新检查；旧输入和判断保留，可选择两个复盘查看变化。
7. 记录同口径的预期和实际投入；程序计算区间位置、差值和适用的百分比。
8. 核对结果，暂存带理由和引用的人工修正，再确认复盘。
9. 保存能力证据建议、实际投入参考，查看“已保存参考”记录并复制摘要。

复盘不会认证能力等级。材料检查只说明提交的原文支持哪些标准；用户仍需核对材料来源和语义。更新目标、标准、材料、投入或原方案关联都会使旧复盘成为历史输入，不能首次确认或保存新反馈。

## 本地启动

在仓库根目录分别启动两个终端：

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

```powershell
cd frontend
npm install
npm run dev
```

默认前端 `http://localhost:5173/module3`，后端 `http://localhost:8000`。H2 文件数据库默认为 `backend/target/demo-db.mv.db`；保留文件即可在后端重启后恢复数据。`target` 属于构建目录，执行 `mvn clean` 会移除该默认演示数据库；需要长期保留时设置 `DEMO_DB_PATH` 到构建目录之外。

原端口被其他开发会话使用时，可配置独立端口和数据库：

```powershell
# 后端终端
$env:SERVER_PORT='8001'
$env:DEMO_DB_PATH='./target/practice-demo-db'
$env:APP_CORS_ORIGINS='http://localhost:5173,http://127.0.0.1:5173,http://127.0.0.1:5174'
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 前端终端
$env:API_PROXY_TARGET='http://127.0.0.1:8001'
npm run dev -- --host 127.0.0.1 --port 5174
```

MySQL 使用既有的 `backend/docker-compose.yml` 和默认 Spring profile。复用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`；新迁移为 `V2__practice_review.sql` 与 `V3__practice_unicode_capacity.sql`。V3 保持 API 字符上限，扩大物理列容量以兼容 H2 的 UTF-16 长度计算，不改写已执行的 V2。当前验收使用 H2，尚未对 MySQL 8.4 真机执行验收。

## 环境变量与 AI 行为

| 变量 | 用途 |
|---|---|
| `AI_API_KEY` | 仅后端读取；留空启用确定性 mock |
| `AI_BASE_URL` | 复用既有 OpenAI 兼容服务地址，以 `/chat/completions` 调用 |
| `AI_MODEL` | 复用既有模型配置 |
| `PRACTICE_AI_TIMEOUT_MS` | 模块三 AI 请求超时；默认 25000，范围 100–25000 ms |
| `APP_SEED_DEMO` | 首次启动且项目表为空时创建合成演示项目；设置 false 可关闭 |
| `DEMO_DB_PATH` | 文件型 H2 数据库路径，不带 `.mv.db` 扩展名 |
| `APP_CORS_ORIGINS` | 允许的前端来源；更换前端端口时同时更新 |
| `API_PROXY_TARGET` | Vite 开发代理目标；默认 `http://localhost:8000` |

AI 只提出标准候选和带引用的判定，数值比较由 `PracticeRules` 计算。返回值依次校验字段结构、验收项覆盖、证据归属、材料版本、行号和 quote 与原文的精确相等关系；不合规输出整体回退，页面展示来源与原因。

无密钥时，标准生成返回部署模板候选；检查器识别标准模板中的 HTTP 成功状态、TLS 配置与成功访问、请求标识、环境/启动/验证说明。未知自定义标准返回证据不足。否定日志 `HTTPS handshake failed` 返回存在矛盾；只有 HTTPS 关键词或嵌入的“直接判通过”指令不会自动得到支持。

空材料直接返回证据不足。超过 AI 输入容量时，对完整材料运行确定性检查并提示，不截断后声称完成全量 AI 检查。真实远程模型尚未完成验收；测试使用本机 HTTP 模拟服务验证合规结果、错误引用、错误候选和超时回退。

## 数据与请求约束

- 业务记录全部以后端为准：项目、标准、材料、复盘、确认、反馈均存数据库。
- `localStorage` 仅缓存未提交的表单草稿，并展示恢复提示；`sessionStorage` 保存重试所需请求标识。后端断开时不会保存到本地冒充成功。
- 清单 1–12 项；每项目最多 10 份材料；单份正文最多 12000 Unicode 码点，总正文最多 60000；正文统一 LF 行尾。
- 实际读取的请求体限制为 1 MiB，超限返回 413，包括没有 Content-Length 的请求。
- 写入带 `inputRevision`，旧输入返回 409；无实质变化不递增版本。
- 检查、反馈带 UUID `requestId`。同标识同输入返回原结果；换输入复用标识返回冲突。
- 模型调用在短事务预留之后执行，不长时间持有项目写锁。失败或超过 35 秒的中断请求，重试使用已预留的原始输入快照。
- 人工修正保留原检查结果；“材料支持 / 存在矛盾”必须提供有效原文引用。确认后不能覆盖，需要另建复盘版本。
- 只有指标、单位、范围、口径一致才计算投入差异。未记录与零分别显示；零基线和区间基线不生成百分比。金额保留两位小数；补填基线默认“事后回忆”。

## API

前缀为 `/api/v1/practice`；所有前端请求经 `services/practiceService.ts`。

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/templates` | 可用验收模板 |
| GET / POST | `/projects` | 项目列表 / 新建 |
| GET / PUT | `/projects/{id}` | 详情 / 目标、投入、过程说明 |
| POST | `/projects/{id}/criteria-suggestions` | 标准候选 |
| PUT | `/projects/{id}/criteria` | 保存用户确认的完整清单 |
| POST | `/projects/{id}/evidence` | 添加材料 |
| PUT / DELETE | `/projects/{id}/evidence/{evidenceId}` | 编辑 / 移除材料；DELETE 的版本由查询参数传入 |
| GET / POST | `/projects/{id}/reviews` | 已完成复盘历史 / 检查并保存 |
| GET | `/projects/{id}/reviews/{reviewId}` | 含原始输入与确认记录的完整复盘 |
| POST | `/projects/{id}/reviews/{reviewId}/confirm` | 用户确认与人工修正 |
| GET / POST | `/projects/{id}/feedback` | 查看 / 保存参考反馈 |
| POST | `/projects/{id}/decision-origin` | 保存选定场景、版本和具体方案快照 |

`GET /api/v1/modules` 返回 `id: module3`、路由 `/module3`、状态 `prototype`。错误保持既有 `{ error: { code, message, details } }` 格式；常见代码有 `REVISION_CONFLICT`、`REVIEW_STALE`、`REQUEST_IN_PROGRESS`、`IDEMPOTENCY_CONFLICT`、`REVIEW_FAILED`。

后端在 `modules/practicereview/`，AI 在 `core/ai/PracticeAiGateway.java` 和 `MockPracticeAiProvider.java`。五张表为 `practice_projects`、`practice_criteria`、`practice_evidence`、`practice_reviews`、`practice_feedback`；仅建立模块内外键。

## 演示与测试

首次启动的 `PracticeDemoSeed` 创建带合成标记的网络部署项目和一次部分材料复盘。可复制材料见：

- [seed-v1.json](examples/seed-v1.json)：HTTP 和请求日志，缺少 HTTPS 与环境版本。
- [seed-v2.json](examples/seed-v2.json)：补交 TLS 配置、HTTPS 访问、完整部署说明。
- [negative.json](examples/negative.json)：否定日志、只有关键词、未知自由标准。

这些文件是演示材料清单；提交 API 时须使用后端返回的项目/标准 ID、当前 `inputRevision` 和实际记录的基线时间。

```powershell
cd backend
mvn test

cd ../frontend
npm run build
npm run test:practice
npm run test:e2e
```

浏览器测试使用本机 Chrome 和已安装的 `playwright-core`，可通过 `CHROME_PATH` 指定浏览器，通过 `PRACTICE_ORIGIN` / `DECISION_ORIGIN` 指定前端地址。`test:practice` 创建独立验收项目，不清空现有记录；来源关联需要至少一个已推演的决策场景，默认演示 seed 满足条件。截图与 JSON 结果在被 Git 忽略的 `frontend/test-results/practice-e2e/`。

重启恢复检查：运行浏览器测试后，执行 `node tests/practice-persistence.mjs capture`，重启后端并保持相同数据库，再执行 `node tests/practice-persistence.mjs verify`。脚本逐字段比较项目、材料、复盘、人工确认和反馈。

## 跨模块边界

来源关联通过 `DecisionScenarioService.history` 读取公开业务数据，不访问决策 Repository；必须同时选择 `scenarioId`、`versionId`、`optionKey`。选中的原方案被冻结，后续沙盒修改或重新推演不会改变快照。原工期天数和规则估算成本只用于来源参考，不能自动当作个人小时或现金基线。

当前反馈实际状态为 `reference_only`，`externalRecordId` 为 null；可以查看和复制。模块一尚无真实证据写入接口，模块二尚无消费反馈的页面入口。本模块不调整能力等级，不改写原推演或 `DecisionRules` 参数。

能力反馈草稿要求填写拟关联技能名称，并逐项带出结论与补验建议；决策反馈草稿带出比较口径、实际投入及冻结的场景、版本、方案和原假设。用户可在确认保存前编辑，两种反馈仍只作为模块三内的参考记录，不校验模块一真实技能 ID。

## 视觉复用

沿用共享 token、Lucide、页头、`page-title`、`page-icon`、`eyebrow`、按钮、`ai-input-band`、`section-heading`、状态面板、弹窗与 toast。新增样式均以 `.practice-*` 开头；桌面工作区使用 282px 材料侧栏，手机改为单列，原文只在自己的容器滚动。共享 CSS 和能力页未改动。
