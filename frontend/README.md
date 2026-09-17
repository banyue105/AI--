# 前端：首页与个人能力成长

使用 Vue 3、TypeScript、Vite、Vue Router 和 Pinia。当前已对接 Java 21 + Spring Boot + MyBatis + MySQL 后端，保留明确区分的离线演示模式。

## 已实现

- 首页 `/`：用户概览、当前目标、最近活动和模块入口。
- 能力成长 `/ability`：10 个种子节点、四种能力状态、缩放与列表模式。
- 能力节点新增、编辑、候选能力确认、同名合并；服务端保存成功后刷新图谱和路径。
- 等级、已有实践证据、前置关系和可解释成长路径展示。
- 预置前端、后端、网络工程三个知识方向，统一渲染“方向 → 阶段 → 技术项”。
- 任意方向生成、个人技能匹配、从知识项带入新增能力表单。
- 服务端返回的新增知识方向保存到数据库，刷新后可继续读取；生成来源保持 `mock`。
- 统一 API client、加载/错误/保存反馈，以及受控的本地演示 fallback。

决策沙盒 `/decision` 为占位页，第三模块尚未确定。

## 启动与验证

需要 Node.js 20.19+ 或 22.12+。

```bash
cd frontend
npm install
npm run dev
```

访问 [首页](http://localhost:5173/) 或 [能力成长](http://localhost:5173/ability)。Vite 将 `/api` 代理到 `http://localhost:8000`；后端启动与数据库配置见 [backend/README.md](../backend/README.md)。

```bash
npm run build
node --test tests/api-services.test.cjs
```

本轮 11 项 API service 测试已通过，覆盖服务端结果、错误处理、离线演示和来源标识等行为。生产构建已通过 `npm run build -- --configLoader native` 验证；此参数用于受限 Windows 环境中默认 Vite 配置打包遇到的目录权限问题。已在 1280×720、360×800 浏览器中检查页面无横向溢出，并走通新增、编辑、刷新读取、知识方向生成、保存失败反馈与首次离线演示；没有页面脚本异常。

## API 接入

业务组件通过各自 service 调用 `src/core/api/apiClient.ts`，不直接请求数据库或 AI 服务。

| service | 当前使用的接口 |
|---|---|
| `core/api/homeService.ts` | `GET /api/v1/home` |
| `modules/ability-growth/services/abilityService.ts` | `GET /api/v1/ability/graph`、`POST /api/v1/ability/skills`、`POST /api/v1/ability/relations`、`POST /api/v1/ability/parse`、`POST /api/v1/ability/path` |
| `modules/ability-growth/services/knowledgeCatalogService.ts` | `GET /api/v1/knowledge/catalog`、`POST /api/v1/knowledge/generate` |

后端同时提供用户资料读写、指定技能更新和新增实践证据接口，完整清单见后端 README；部分能力尚未提供专用前端表单。成功响应直接返回 DTO 或数组，无 `data` 包装层。错误信息读取统一的 `error.message`。

知识方向请求示例：

```json
{
  "query": "机器人控制工程师",
  "currentSkills": [
    { "name": "Python", "level": 3, "status": "mastered" }
  ]
}
```

响应符合 `KnowledgeTrack`，包含方向信息、`source`、`stages` 和阶段内的 `items`。当前后端使用固定本地模板，返回 `source: "mock"` 并说明真实模型尚未接入；当前技能摘要不会触发个性化模型推理。前端校验响应结构并保留实际来源。

## 数据来源与离线演示

首次尚未连接后端且请求因网络或开发代理不可达而失败时，service 可以使用本地演示。API 返回的 4xx/5xx 业务错误会作为失败显示，不会被改写为本地保存成功。

本次页面会话一旦成功连接后端，后续断连会提示错误。能力图谱若已缓存服务端数据，也不会静默改为可写离线演示。localStorage 保存的是演示数据或已确认的服务端缓存；服务端连接后的数据以数据库为准，本地演示不会自动导入数据库。

新增知识方向在后端模式下持久化；纯离线模板生成仍为演示行为。自然语言候选必须经用户确认才保存。

## 待完成

- 前置关系与实践证据的专用新增/编辑表单。
- 目标截止日期编辑、删除和历史版本管理。
- 真实模型接入与相应的输入输出验证。
- 决策沙盒业务、第三模块方向及生产部署。
- 登录、鉴权和多用户数据隔离；当前后端使用固定 `demo-user`。
