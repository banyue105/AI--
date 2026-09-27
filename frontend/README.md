# 模块一：个人能力成长

本目录是模块一第一阶段的可运行前端原型，使用 Vue 3、TypeScript、Vite、Vue Router 和 Pinia。

## 本阶段已完成

- 移动端优先的全局主页，包含用户概览、当前目标、最近活动和三个模块入口。
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
- API 不可用时在 service 层降级，并通过 localStorage 保存演示数据。
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

## API 边界

页面只通过 `src/modules/ability-growth/services/abilityService.ts` 访问数据。当前会尝试以下接口，接口不可用时自动使用本地 fallback：

```text
GET  /api/v1/ability/graph
POST /api/v1/ability/skills
POST /api/v1/ability/parse
POST /api/v1/ability/path
GET  /api/v1/knowledge/catalog
POST /api/v1/knowledge/generate
```

后端接入时应继续补齐任务书要求的 profile、skill update 和 relation 接口，保持现有 TypeScript 类型语义不变。

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

- 模块一的 Spring Boot、JPA、MySQL 与 Flyway 持久化（模块二已单独实现其后端）。
- 新增/编辑前置关系的页面表单。
- 新增实践证据的页面表单。
- 前端自动化测试；本阶段完成了生产构建和桌面/360px 浏览器手动验收。
- 模块二的实现与运行说明请参阅 [决策沙盒 README](src/modules/decision-sandbox/README.md)。

localStorage 仅用于后端尚未合并时的演示降级。接入 Spring Boot 后，用户和能力数据应以后端为唯一事实来源。
