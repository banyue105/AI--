# Codex 协作计划书索引

这组文件可以直接交给各模块队员的 Codex。目标不仅是避免代码冲突，也要保证不同会话生成的页面属于同一套产品。

## 必读顺序

所有人按顺序完整阅读：

1. `plans/00-shared-integration.md`：技术栈、目录、接口和协作边界；
2. `plans/00-ui-style-contract.md`：视觉单一事实来源，前端任务必须遵守；
3. 自己负责的模块任务书；
4. `plans/04-integration-review.md`：提交前的统一验收表。

当前业务任务书：

- [模块一：能力成长与首页](01-ability-growth-and-home-prompt.md)
- [模块二：决策沙盒](02-decision-sandbox-prompt.md)
- [模块三：实践验证与复盘](03-practice-review-prompt.md)
- [模块四：职业规划](04-career-planning-module.md)

模块四本地同步与验证范围见 [同步记录](08-module4-sync.md)；这份记录保留本地工作区与远端提交的区别，不能将定向测试通过等同于全量验收。

`03-module3-pending-prompt.md` 已归档，不再作为当前开发指令。

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
| 队员3 | 模块三“实践验证与复盘”前后端，复用现有基础设施 | `feature/practice-review` |
| 模块四开发者 | 职业规划前后端，新增 `.career-*`；读取能力图谱，保存意向与报告 | `feature/career-planning` |

## 合并顺序

现有前后端基础工程、能力成长原型和决策沙盒为开发基线。模块三先完成可独立运行的 P0，再由负责人集成路由和首页入口，最后与模块一、模块二完成 P1 联调。共享 token、路由入口或公共类型有冲突时由负责人统一处理，模块开发者不要覆盖整个文件。

## 当前产品叙事

已确定四个模块方向，完成状态按实际实现记录：

1. 能力成长：建立能力结构和成长路径；
2. 决策沙盒：模拟选择及其后果。
3. 实践验证与复盘：P0 已实现，可独立检查成果材料、对比投入并保存参考反馈；已实现决策版本/方案只读关联。能力证据写入与反馈消费仍待联调，入口为 prototype。
4. 职业规划：演示岗位匹配、筛选、JD 规则分析和报告快照；入口为 prototype，不是实际招聘服务。

模块三的实际实现和验证范围见 [验收记录](05-practice-review-acceptance.md)，操作与 API 见 [模块 README](../frontend/src/modules/practice-review/README.md)。跨模块接口未打通时明确标记待联调，不在比赛材料中虚构功能。所有模块都应遵循：

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

后端共享 MySQL 和 Flyway；模块一使用 MyBatis，模块二/三使用 JPA，模块四使用 JDBC。无 MySQL 时可用 demo profile 的 H2，但不能代替 MySQL 8.4 验收。团队开始模块开发前，必须阅读 `00-shared-integration.md` 中的受控 AI 目录约束，以及 `00-ui-style-contract.md` 的能力成长动画基线。
