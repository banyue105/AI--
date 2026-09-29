# 模块四定向同步记录（2026-09-29）

## 来源与边界

- 来源：GitHub `banyue105/AI--`，已 fetch 的 `origin/main` 提交 `2cc6c5b80003dd519a4d34bfc03a430607b46392`。
- 同步时本地 `main` HEAD 保持 `97dabe06f93e71bdbc2b9045fb8281bd2e00bce1`；这是模块四定向同步，不是全量 main 合并。同步阶段未提交或推送；后续提交范围见末尾说明。
- 同步前记录了 39 个已有改动文件的 SHA-256。模块二/三代码、测试与文档，以及用户的 `06-homepage-redesign-prompt.md` 保持原内容；根 README 和本计划索引仅增量补充模块四状态。
- 不同步远端模块一的其他改动，也不整体替换已有共享协作文档或后端配置。

## 同步内容

- 前端 `modules/career-planning/`：职业规划外壳、岗位匹配、岗位筛选、JD 分析、类型、样式、service 与 README。
- 后端 `modules/careerplanning/`：Controller、DTO、JdbcTemplate Service；配套 `CareerApiTest`。
- 数据迁移 `V8__career_planning.sql`：`career_job`、`career_preference`、`career_report`，12 个演示岗位；已有 V1–V7 不变。
- 共享入口：路由、ModuleId、首页图标、后端 ModuleRegistry 与入口测试。
- 必要依赖：共享 `core/api/apiClient.ts`、Vite 的 `API_PROXY_TARGET` 配置；首页 fallback 增加第四入口，公共首页网格调整为两列。不改变模块一 fallback 行为或共享视觉 token。
- 完整同步的 21 个文件通过 Git blob 校验，与来源提交逐一一致；其余是保留本地内容的集成与文档增量修改。

## 已执行验证

- `frontend` 中 `npm run build`：TypeScript 检查与生产构建通过。
- 后端定向测试：37 项，0 失败、0 错误、0 跳过。包含 `CareerApiTest`、`WorkspaceControllerTest`、`DecisionApiTest`、`DecisionFailureApiTest`、`DecisionRulesTest`、`MockDecisionAiProviderTest`、`PracticeApiTest`、`PracticeRulesTest`、`PracticeConcurrencyTest`、`PracticeAiGatewayTest`。测试使用 H2 MySQL 模式，Flyway V1–V8 执行成功。
- `mvn -DskipTests package` 打包通过；此命令本身跳过测试，测试结论来自上述独立定向测试。
- `npm run test:module-ui`：模块二/三 13 项 UI 回归检查通过，两个视口无横向溢出。
- 模块四浏览器只读冒烟：首页四入口、三个子页面、12 岗位显示、后端开发/上海筛选、岗位详情、JD 带入、键盘切换和无匹配空状态通过；未提交偏好、目标或报告，未产生后端写请求。
- 首页与三个子页面均在 1280×720、360×800 检查：根页面 scrollWidth 分别为 1280、360，未横向溢出；无未预期页面/控制台错误。已有缺失 favicon 的 404 单独记录，不当作模块错误。
- 偏好/目标保存、报告生成与快照读取、用户隔离及错误参数由 `CareerApiTest` 验证；本次浏览器未进行这些写操作。
- 预览服务健康检查为 UP，首页返回四模块，职业规划 jobs/matches 接口各返回 12 项。

浏览器截图在忽略目录 `frontend/test-results/career-sync/`；模块二/三回归产物在 `frontend/test-results/module-ui-e2e/`。

## 本地预览与数据保护

- 前端：`http://127.0.0.1:5173/`。
- 职业规划：`http://127.0.0.1:5173/module4/match`，子页 `jobs`、`jd`。
- API：`http://127.0.0.1:8000/api/v1/career`。
- 本次预览使用 demo profile，`DEMO_DB_PATH=./target/merged-preview-db`，沿用原 H2 文件库。
- 后端停机后、V8 执行前，数据库已备份到 `backend/target/merged-preview-db.before-career-20260929-081952.mv.db.bak`。
- 迁移并重启后核对：原 4 个决策场景和 4 个复盘项目的 ID 列表完全相同，未丢失或新增测试记录。
- 重启此预览库：在 `backend` 中设置上述 `DEMO_DB_PATH`，运行 `java -jar target/assistant-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo`；在 `frontend` 运行 `npm run dev`。不要用 `mvn clean` 清除需要保留的库和备份。
- 使用默认 H2 库或 MySQL 的常规启动步骤见 `backend/README.md`。后端换端口时同时设置 `SERVER_PORT` 与前端 `API_PROXY_TARGET`，不要改业务组件地址。

## 未验证与产品限制

- 本次没有执行 MySQL 8.4 实库验收，也没有重跑完整后端测试套件；不能将定向测试通过写成全量通过。当时本地原始工作区的后端 README 保留模块一既有测试失败记录，相关远端修复未混入定向同步；这不是对远端最新 main 测试套件的结论。
- 岗位、公司、薪资是演示数据；匹配与 JD 分析采用明确技能词规则，分数不是录用概率，不是实际招聘推荐。
- 当前未接入真实招聘平台、简历生成或正式登录认证；职业规划偏好和报告使用现有临时用户边界。

## 后续提交范围

用户随后要求将本次修改推送到 main。模块四的功能代码和必要入口已经存在于来源提交中，因此不重复上传代码。发布提交基于最新 `origin/main`，仅增量补充根 README、前后端 README、计划索引和本同步记录，不覆盖远端其他模块实现，也不提交原工作区的模块二/三 UI 改动或用户的 06 文档。

上述测试和浏览器结论来自定向同步后的本地工作区；发布提交只修改文档，不将这些结果冒称为远端整个工程的全量验收。原工作区、运行中的预览及未提交改动保留。
