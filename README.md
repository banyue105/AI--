# 拓界：AI 个人成长与决策工作台

“拓界”是一个面向个人成长与现实决策的响应式工作台。项目将自然语言转为结构化数据，再通过图谱、路径和比较视图让用户检查、修改并继续操作，而不是停留在一次性 AI 问答。

当前仓库处于初步架构阶段：模块一前端原型可运行，模块二保留稳定入口，Spring Boot/MySQL 后端由协作计划继续接入。

## 当前实现

- Vue 3 + TypeScript + Vite 前端基础工程；
- 全局工作台首页 `/`；
- 能力成长模块 `/ability`；
- 可编辑个人技能树、节点详情和可解释成长路径；
- 可按方向、阶段和技术项渲染的知识分类，并支持 AI/API 生成同结构内容；
- 新节点防重叠布局、同名候选合并和本地演示持久化；
- 决策沙盒入口 `/decision`，当前为待协作开发的占位页；
- 白色与浅色工作台视觉系统，以及桌面/360px 响应式布局；
- API service fallback：后端暂不可用时仍可完整演示模块一。

## 技术基线

```text
前端：Vue 3 + TypeScript + Vite + Vue Router + Pinia
图标：lucide-vue-next
后端计划：Java 21 + Spring Boot 3.5 + JPA + MySQL 8.4 + Flyway
接口前缀：/api/v1
```

## 目录结构

```text
frontend/
  src/
    app/                    路由与应用级结构
    core/                   共享类型和 API service
    modules/ability-growth/ 模块一前端
    pages/                  首页与模块入口
    styles/main.css         当前视觉 token 和公共样式
plans/
  00-shared-integration.md  技术与协作合同
  00-ui-style-contract.md   前端视觉合同
  01-*.md                   模块一任务书
  02-*.md                   模块二任务书
  03-*.md                   第三模块骨架任务书
  04-integration-review.md  合并前验收表
```

## 本地运行

需要 Node.js 20 或更高版本。

```bash
cd frontend
npm install
npm run dev
```

默认访问：

- 首页：`http://localhost:5173/`
- 能力成长：`http://localhost:5173/ability`
- 决策沙盒入口：`http://localhost:5173/decision`

生产构建：

```bash
cd frontend
npm run build
```

开发服务器会把 `/api` 代理到 `http://localhost:8000`。后端尚未启动时，模块一通过 service 层的确定性 fallback 提供演示数据。

## 协作入口

所有队员和 Codex 会话先阅读 [plans/README.md](plans/README.md)，再按其中顺序阅读共享技术合同、视觉合同、自己的模块任务书和合并验收表。

核心规则：

- 不重建现有前端工程，不覆盖其他模块；
- `/` 与 `/ability` 是当前视觉基准；
- 不修改共享视觉 token 来迁就单个模块；
- 新模块样式使用模块前缀；
- 通过 service 与 `/api/v1` 协作，组件不直接访问后端；
- 提交前实测 `1280 x 720`、`360 x 800` 并运行构建。

## 当前未完成

- Spring Boot、MySQL、JPA 和 Flyway 基础后端；
- 能力数据的服务端持久化与完整关系编辑；
- 决策沙盒正式业务页面和规则计算；
- 第三模块最终方向；
- 自动化测试与生产部署配置。

详细状态和 API 边界见 [frontend/README.md](frontend/README.md)。
