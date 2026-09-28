# Codex 任务书：模块1「个人能力成长」+ 移动端主页

你是本项目的模块1负责人，同时负责全局主页、用户个人概览和共享视觉基线。请先完整阅读仓库中的 `plans/00-shared-integration.md`、`plans/00-ui-style-contract.md` 和 `plans/04-integration-review.md`，严格遵守技术基线、数据边界、视觉合同和合并规范。

当前 `frontend` 已经存在可运行的首页与能力成长页。它们是其他模块的视觉基准，不要重建工程或推翻现有结构。先运行 `/` 和 `/ability`，确认现状后在原实现上迭代。

## 目标

开发一个移动端优先的“个人能力成长模块”，并搭建统一主页。模块的核心不是普通 AI 问答，而是：将用户输入的目标、技能、经验和测评结果结构化，建立可编辑的能力关系图，并生成可解释的成长路径。必须有 Spring Boot 后端持久化用户、目标、技能和关系数据。

主页需要展示所有模块入口、用户概览和最近状态。第三模块方向已确定为“实践验证与复盘”，业务依据 `03-practice-review-prompt.md`；功能实现前保持 pending，不将方向确认标为功能完成。

## 比赛背景

项目参加 2026 年 iCAN 大学生创新创业大赛 AI 应用创新挑战赛软件赛道候选方向。通知重点要求：AI 必须在具体场景中发挥核心作用，作品可在线运行，视频不超过 5 分钟，方案 PDF 不超过 20 页，评审关注创新性、技术实现、实用价值、用户体验和展示效果。截止日期为 2026 年 9 月 30 日。

## 目标用户与第一版场景

目标用户是需要长期提升专业能力的个人，第一版演示可采用大学生专业成长案例，但产品文案不要把系统限制为某个学校或专业。

示例：用户选择“网络工程方向”，输入自己掌握 Python、Linux、TCP/IP，希望 60 天达到“能够完成一个小型网络服务部署”的目标。系统展示能力节点、前置关系、当前掌握程度和建议路径。

## 必须实现的功能

### A. 全局主页

- 移动端首屏显示用户头像/名称、当前目标和最近活动；
- 显示三个模块入口：能力成长、决策沙盒、实践验证与复盘（按实际开发状态展示）；
- 能力成长和决策沙盒入口可以进入真实页面；
- 第三模块未实现前保持 pending，联调后按真实验收结果更新为 prototype/ready；
- 提供统一底部导航或清晰的返回路径；
- 主页的模块卡片展示标题、用途、状态和最近一次更新时间；
- 添加空状态、加载状态和错误状态。

### B. 能力数据录入

至少支持以下输入方式：

- 新增能力节点；
- 设置掌握程度（不了解、了解、能实践、熟练、可指导）；
- 添加目标能力；
- 添加前置关系；
- 输入个人目标和截止日期；
- 添加一次测评/实践证据。

开发时允许使用 mock AI，但正式页面的数据必须通过后端 API 读写。没有后端或 AI 密钥时，使用 API 层的 mock/fallback，而不是在 Vue 组件内写死结果。

### C. 能力图谱

使用图谱而不是普通列表展示：

- 已掌握节点、进行中节点、目标节点、缺口节点使用不同视觉状态；
- 关系线表示前置依赖；
- 点击节点查看掌握等级、证据和建议下一步；
- 支持用户编辑节点和关系；
- 移动端支持横向滚动、缩放或切换为分层列表；
- 节点数量较多时仍保持可读，至少为 mock 数据准备 8—12 个节点。

### D. 成长路径

根据目标能力和当前能力生成一条可解释路径：

- 路径中的每一步说明需要补充的能力；
- 显示推荐顺序和原因；
- 显示当前状态、目标状态和待验证项目；
- 不输出没有依据的精确时间承诺；
- AI 结果必须显示“依据/假设”，允许用户编辑或重新生成。

### E. AI 结构化处理

AI 只负责：

- 从自然语言中抽取能力、目标、掌握程度和前置关系；
- 生成候选能力节点和解释；
- 根据结构化数据生成成长路径说明。

页面不能直接依赖模型长文本。定义如下类型：

```ts
export interface SkillNode {
  id: string;
  name: string;
  description?: string;
  level: 0 | 1 | 2 | 3 | 4;
  status: 'mastered' | 'developing' | 'gap' | 'target';
  evidenceIds: string[];
}

export interface SkillRelation {
  from: string;
  to: string;
  type: 'prerequisite' | 'related';
  confidence: number;
}

export interface GrowthPathStep {
  skillId: string;
  reason: string;
  prerequisiteIds: string[];
  status: 'next' | 'blocked' | 'done';
}
```

## 后端与 API

在 `backend/src/main/java/com/ican/assistant/modules/abilitygrowth/` 实现：

- 用户能力、目标、技能关系和成长路径的 MyBatis 数据模型/Mapper；
- Java DTO/record 请求/响应模型；
- 至少实现以下接口：

```text
GET  /api/v1/profile
PUT  /api/v1/profile
GET  /api/v1/ability/graph
POST /api/v1/ability/skills
PUT  /api/v1/ability/skills/{skill_id}
POST /api/v1/ability/relations
POST /api/v1/ability/path
POST /api/v1/ability/parse
```

- `parse` 接受用户自然语言，调用统一 AI 适配层；无密钥时返回固定 mock；
- `graph` 返回节点、关系、来源和更新时间；
- `path` 只基于结构化能力数据生成，不允许直接返回未经校验的长文本作为路径；
- 所有写入接口使用 Bean Validation 校验字段，错误按统一格式返回；
- 提供至少一组 seed/mock 数据，便于评审启动后直接看到完整图谱。

在 `frontend/src/modules/ability-growth/` 实现 Vue 页面、组件、Pinia 状态和 API service；页面通过 API service 调用后端。

## 技术实现要求

- 前端业务代码放在 `frontend/src/modules/ability-growth/`；
- 主页和全局布局放在 `frontend/src/app/` 或 `frontend/src/pages/`，并记录新增路由；
- 前端共享类型放在 `frontend/src/core/types.ts`，后端 DTO/record 放在 `backend/src/main/java/com/ican/assistant/modules/abilitygrowth/`；
- 前端用 service 层隔离 API，例如 `abilityService.getGraph()`、`abilityService.generatePath()`；
- AI 调用只允许出现在 `backend/src/main/java/com/ican/assistant/core/ai/` 适配层；
- localStorage 只保存临时界面偏好，用户和能力数据必须进入后端；
- 不引入登录、支付、实时协作等非 MVP 功能。

## 视觉和手机端要求

- 视觉实现以 `plans/00-ui-style-contract.md` 为唯一标准，不创建第二套 token；
- 保持白色工作面、浅灰背景、蓝色操作、绿色成功和琥珀色进行中状态；
- 首页使用 1120px 内容宽度，模块页使用 1320px 内容宽度和统一吸顶页头；
- 能力页信息顺序保持“目标概览 -> AI 输入 -> 个人技能树和详情 -> 知识方向 -> 可解释路径”；
- 个人技能树属于用户当前状态，不能再作为知识方向 tab；
- 知识方向使用“方向 -> 阶段 -> 技术项”的结构化渲染，数据可由 API 或 AI 生成；
- 图谱区域使用稳定的高度和溢出策略；
- 颜色同时表达状态和文字标签，不能只依赖颜色；
- 关键按钮使用图标加文字或熟悉图标，并提供可访问标签；
- 首页优先展示用户状态和模块入口，不制作营销型 hero；
- 至少完成空状态、加载状态、AI 失败状态和保存成功反馈；
- 新增节点必须经布局分配器寻找空位，加载已有数据时也要处理重叠；
- 在 `1280 x 720` 和 `360 x 800` 实测，并确认页面级无横向溢出。

## 验收案例

1. 打开主页，看到用户、目标和三个模块入口。
2. 进入能力成长，输入“我会 Python、Linux，目标是完成网络服务部署”。
3. AI/mock 服务返回结构化能力节点和关系。
4. 页面显示能力图谱，用户可以点击和修改节点。
5. 选择目标能力，页面显示成长路径和缺口。
6. 刷新页面后，演示数据仍然存在。
7. 关闭 AI 配置后，页面仍能通过后端 mock 数据完整演示；
8. 重启后端后，已经保存的用户和能力数据仍然存在。

## 交付物

- Vue 3 + TypeScript 的模块1页面和组件；
- 移动端主页；
- 路由与模块注册说明；
- Spring Boot 模块1 API、MyBatis 数据模型、Java DTO/record 和 seed 数据；
- 前端 API service 和 mock/fallback 说明；
- 至少一组前端或后端测试；
- `README` 中的前端/后端启动方式、环境变量和已知问题；
- 提交前确认 `npm run build`、后端启动和测试成功，并在提交说明中写出与模块2的集成点。
- 按 `plans/04-integration-review.md` 提交桌面与手机验收结果，说明共享视觉类是否被修改。

## 不要做的事情

- 不要把能力成长做成普通聊天窗口；
- 不要把“AI 判断用户是否真正掌握”写成无依据的绝对结论；
- 不要为第三模块自行设计业务；
- 不要修改模块2内部代码；
- 不要为了视觉效果引入三维场景或复杂动画；
- 不要在没有数据依据时显示精确的学习时长、成功率或排名。
- 不要改变共享 token 来给单一页面配色，不要增加渐变、大圆角、装饰性玻璃、卡片嵌套或营销 hero。
