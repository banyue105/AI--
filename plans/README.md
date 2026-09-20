# Codex 协作计划书索引

这组文件可以直接交给三位队员各自的 Codex。目标不仅是避免代码冲突，也要保证不同会话生成的页面属于同一套产品。

## 必读顺序

所有人按顺序完整阅读：

1. `plans/00-shared-integration.md`：技术栈、目录、接口和协作边界；
2. `plans/00-ui-style-contract.md`：视觉单一事实来源，前端任务必须遵守；
3. 自己负责的模块任务书；
4. `plans/04-integration-review.md`：提交前的统一验收表。

涉及前端时，还必须阅读并实际查看：

```text
frontend/src/styles/main.css
frontend/src/pages/HomePage.vue
frontend/src/modules/ability-growth/AbilityGrowthPage.vue
http://localhost:5173/
http://localhost:5173/ability
```

禁止只把自己的模块任务书单独交给 Codex，否则它缺少共享合同和视觉基准。

## 当前分工

| 人员 | 负责内容 | 分支建议 |
|---|---|---|
| 负责人 | 维护首页、能力成长、共享视觉 token、公共样式和最终前端集成 | `feature/ability-home` |
| 队员2 | 决策沙盒前后端，只新增 `.decision-*` 视觉实现 | `feature/decision-sandbox` |
| 队员3 | 后端基础骨架、第三模块占位和候选方向预研 | `feature/module3-shell` |

## 合并顺序

先合并队员3的后端基础骨架，再以当前 `frontend` 和模块1作为前端基线，之后合并决策沙盒，最后确认第三模块占位骨架。共享 token、路由入口或公共类型有冲突时由负责人统一处理，模块开发者不要覆盖整个文件。

## 当前产品叙事

第一阶段只承诺两个正式模块：

1. 能力成长：建立能力结构和成长路径；
2. 决策沙盒：模拟选择及其后果。

第三模块在没有充分论证前保持待定，不在比赛材料中虚构功能。所有模块都应遵循：

```text
AI 处理信息 → 形成结构化数据 → 网页进行可视化展示 → 用户继续操作和修正
```

## 给 Codex 的起始指令

可把下面文字和对应文件一起交给队友的 Codex：

```text
先完整阅读 plans/00-shared-integration.md、plans/00-ui-style-contract.md、你的模块任务书和 plans/04-integration-review.md。先检查仓库现状并运行现有页面，不要重建工程、不要修改共享视觉 token、不要覆盖其他模块。按任务书实现后，在 1280x720 和 360x800 验收，运行构建，并列出变更文件、接口、视觉复用项和已知问题。
```

## 本地启动

```text
数据库：docker compose -f backend/docker-compose.yml up -d
后端：cd backend && .\\mvnw.cmd spring-boot:run
前端：cd frontend && npm install && npm run dev
```

前端开发服务器通过 `/api` 代理到 `http://localhost:8000`。生产部署时使用环境变量配置后端地址，不能把本地地址写死在业务组件中。

后端使用 MyBatis、Flyway 和本地 MySQL。团队开始模块开发前，必须阅读 `00-shared-integration.md` 中的受控 AI 目录约束，以及 `00-ui-style-contract.md` 的能力成长动画基线。
