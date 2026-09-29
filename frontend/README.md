# 模块一：个人能力成长

本目录是可运行的 Vue 3、TypeScript、Vite、Vue Router 和 Pinia 前端；四个模块均已有后端实现。

- [决策沙盒 `/decision`](src/modules/decision-sandbox/README.md)
- [实践验证与复盘 `/module3`](src/modules/practice-review/README.md)
- [职业规划 `/module4/match`](src/modules/career-planning/README.md)

职业规划内部有 `/module4/match`、`/module4/jobs`、`/module4/jd` 三个子页，通过 `careerService` 和共享 `core/api/apiClient.ts` 访问 `/api/v1/career`。用户意向、目标岗位和报告快照由后端保存；模块四不使用能力成长的离线演示 fallback，需要同时启动后端。

## 本阶段已完成

- 移动端优先的项目介绍主页，包含产品 Banner、四模块简介入口与亮点预览；首页和模块共用顶部导航。
- 四模块亮点介绍独立于业务加载，支持跨页面 hash 定位、刷新、重复点击与浏览器滚动恢复；职业规划三个子页保留模块内标签。
- 能力成长独立路由 `/ability`。
- 10 个种子节点、前置关系、四种节点状态及可缩放/可切换列表的能力图谱。
- 节点选择、详情、掌握等级、实践证据和建议下一步。
- 能力节点新增和编辑，保存后即时更新图谱与成长路径。
- 可扩展的知识方向视图，预置前端、后端和网络工程，并支持按任意文本生成结构化技术栈。
- 方向内容统一渲染为“方向 → 阶段 → 技术项”，并匹配个人技能树中的掌握状态。
- 未学习技术可直接带入新增能力表单；个人技术栈统一在下方个人技能树中展示。
- 自然语言结构化输入，以及无后端时可重复的 mock AI 结果。
- 同名候选能力合并，避免反复解析产生重复节点。
- 基于结构化节点和前置关系生成的可解释成长路径。
- 首次无法连接后端时在 service 层降级，并通过 localStorage 保存离线演示数据；后端已连接后的写入失败会报错。
- 加载、错误、空证据和保存成功状态。

## 启动

```bash
cd frontend
npm install
npm run dev
```

默认地址为 `http://localhost:5173`，`/api` 会代理到 `http://localhost:8000`。

生产构建：

```bash
npm run build
```

首页与导航验收（需运行现有前后端与 Chrome）：

```bash
npm run test:home
```

首页测试在浏览器中拦截非读取 API 请求，不修改后端记录。可用 `HOME_ORIGIN` 指定预览地址；默认地址为 `http://127.0.0.1:5173`。截图与结果保存在 `test-results/home-redesign/`；实际验证范围见 [首页验收记录](../plans/09-homepage-redesign-acceptance.md)。

## API 边界

页面只通过 `src/modules/ability-growth/services/abilityService.ts` 访问能力数据。后端提供以下接口；首次无法连接时可使用离线演示：

```text
GET  /api/v1/ability/graph
POST /api/v1/ability/skills
DELETE /api/v1/ability/skills/{id}
POST /api/v1/ability/relations
POST /api/v1/ability/skills/{id}/evidence
POST /api/v1/ability/parse
POST /api/v1/ability/path
GET  /api/v1/knowledge/catalog
POST /api/v1/knowledge/generate
```

用户资料、技能更新和关系接口已由后端提供。决策沙盒与实践复盘接口见各自模块文档。

知识方向生成请求会同时携带用户问题和当前技能摘要：

```json
{
  "query": "机器人控制工程师",
  "currentSkills": [
    { "name": "Python", "level": 3, "status": "mastered" }
  ]
}
```

AI 响应必须符合 `KnowledgeTrack`：包含方向基本信息、`stages` 阶段数组以及每个阶段的 `items` 技术项数组。前端在 service 层校验响应结构后才交给通用组件渲染；接口不可用时使用同结构的确定性 fallback。

## 本阶段未完成

- 登录认证和生产部署配置。
- 更完整的关系编辑与端到端自动化验收。
- 模块二的实现与运行说明请参阅 [决策沙盒 README](src/modules/decision-sandbox/README.md)。

localStorage 仅用于首次离线演示和后端数据的本地缓存；正式数据以后端为准。
