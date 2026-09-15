# iCAN AI 个人助手项目：统一协作与集成规范

> 用途：三位队员在各自 Codex 会话中开发时，先阅读本文件，再阅读 `plans/00-ui-style-contract.md` 和自己的任务书。本文不是业务模块计划，而是跨模块的技术合同。

## 项目定位

项目暂定名称：AI 个人成长与决策工作台。

当前确定的两个业务模块：

1. 个人能力成长模块：建立能力结构、掌握程度和成长路径。
2. AI 决策沙盒模块：建立现实条件，比较不同选择的时间、资金、人力、资源和风险后果。

第三个业务模块尚未确定。本阶段只建设可插拔模块骨架和占位页，不得自行确定第三模块的产品方向。

项目面向更广泛的个人用户，但第一版演示案例可以使用大学生创新项目/专业成长场景。产品形态为移动端优先的响应式网页，报名时优先考虑软件赛道。

## 比赛约束（必须记住）

本项目面向 2026 年 iCAN 大学生创新创业大赛 AI 应用创新挑战赛。

- 软件赛道接受 Agent、网页应用、移动应用和行业解决方案；必须可以在线演示或提供可运行程序。
- 作品必须原创，核心技术和知识产权归团队所有或获得书面授权。
- 应用方案 PDF 不超过 20 页。
- 演示视频 MP4 不超过 5 分钟，必须展示 Agent 接收请求、分析决策、反馈结果和模块协同。
- 报名及作品提交截止日期为 2026 年 9 月 30 日；报名后不能更换赛道或修改团队信息。
- 评分：创新性 30 分、技术实现 30 分、实用价值 20 分、用户体验 10 分、展示效果 10 分。

所有开发决策都要服务于：清晰场景、真实可运行、AI 有实际作用、可视化能解释复杂关系。

## 统一技术基线

本项目统一采用前后端分离的 Vue 3 + TypeScript + Java Spring Boot 方案：

### 前端

- Vue 3 + TypeScript + Vite；
- Vue Router 负责路由，Pinia 只保存界面状态；
- 移动端优先的响应式 CSS，支持 360px 宽度；
- 不依赖重量级 UI 模板，优先使用普通 CSS 和项目内组件；
- 图谱/流程可使用 Vue Flow，统计图可使用 ECharts；如依赖尚未安装，先确认 `frontend/package.json` 再添加；
- 所有后端请求通过 `frontend/src/core/api/`，页面不得直接调用 `fetch` 或 axios；
- 开发环境使用 Vite 代理将 `/api` 转发到 Spring Boot。

### 前端视觉基线

- 当前 `/` 和 `/ability` 是已落地的产品视觉基准，不得另起设计语言；
- `plans/00-ui-style-contract.md` 是强制合同，不是灵感参考；
- `frontend/src/styles/main.css` 中的 `:root` token 由集成人维护，模块开发者不得修改 token 值；
- 模块页面优先复用既有公共类，新样式必须使用模块前缀；
- 所有正式模块都使用浅灰页面背景、白色工作面、蓝色主要操作、绿色成功状态和琥珀色注意状态；
- 所有前端交付必须在 `1280 x 720` 与 `360 x 800` 两个视口验收，并使用 `plans/04-integration-review.md` 自检。

### 后端

- Java 21 LTS；
- Spring Boot 3.5.x + Spring Web + Bean Validation；
- Spring Data JPA + MySQL 8.4 LTS；
- Flyway 管理数据库迁移；
- Maven 管理依赖和构建；
- JUnit 5 + Spring Boot Test 做接口和服务测试；
- AI 调用统一放在 `backend/src/main/java/com/ican/assistant/core/ai/`，没有 API Key 时必须使用 deterministic mock；
- CORS、环境变量和错误响应统一配置；
- 不把模型密钥或数据库密码写入前端、代码或提交记录。

### 前后端协作

- 前端显示的数据必须来自 API 或明确的 mock service，不能在组件内写死业务结果；
- 后端返回结构化 JSON，页面不依赖不可控的长文本；
- 后端 API 前缀统一为 `/api/v1`；
- 接口请求/响应类型在前端 `frontend/src/core/types.ts` 中维护，后端使用语义一致的 Java DTO/record；
- 每个接口写清输入、输出、错误状态和 mock 行为；
- 前后端都要提供独立启动方式和根目录 README。

## 目录与边界

建议目录：

```text
frontend/
  src/
    app/                    # 路由、布局、全局导航
    components/             # 跨模块通用组件
    core/                   # API client、类型、状态、事件
    modules/
      ability-growth/       # 模块1前端
      decision-sandbox/     # 模块2前端
      module3-placeholder/  # 第三模块待定占位
    pages/                  # 页面级组合
    styles/
backend/
  pom.xml
  src/main/java/com/ican/assistant/
    AssistantApplication.java # Spring Boot 入口
    core/                      # 配置、数据库、AI 适配、异常
    api/                       # 全局 API、健康检查、模块清单
    modules/
      abilitygrowth/           # 模块1后端
      decisionsandbox/         # 模块2后端
      module3placeholder/      # 第三模块待定占位
    common/                    # 通用响应、校验、工具
  src/main/resources/
    application.yml
    db/migration/              # Flyway SQL
  src/test/java/
contracts/                  # 可选：API 示例 JSON 与接口说明
```

模块开发者只修改自己的前后端模块目录和必要的测试/类型文件，不重写其他模块的业务逻辑。主页、全局布局、共享视觉 token 和跨模块公共样式由模块1负责人维护。其他模块若确需新增公共原语，先提交最小提案；未经确认只添加带模块前缀的样式。

## 最小共享数据类型

前端共享类型放在 `frontend/src/core/types.ts`，后端使用语义一致的 Java DTO/record。需要保持稳定：

```ts
export type ModuleId = 'ability-growth' | 'decision-sandbox' | 'module3';

export interface UserProfile {
  id: string;
  name: string;
  avatar?: string;
  goals: string[];
}

export interface Goal {
  id: string;
  title: string;
  description?: string;
  deadline?: string;
  status: 'active' | 'paused' | 'completed';
}

export interface ModuleManifest {
  id: ModuleId;
  title: string;
  description: string;
  route: string;
  status: 'ready' | 'prototype' | 'pending';
}
```

模块对外暴露 `ModuleManifest`、一个页面组件和一组明确的 API service，不能直接引用另一个模块的内部状态。跨模块信息通过共享类型、API、服务函数或事件传递。

## 后端 API 约定

后端至少提供以下基础接口：

```text
GET  /api/v1/health
GET  /api/v1/profile
PUT  /api/v1/profile
GET  /api/v1/modules
```

统一错误格式：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "可读的错误说明",
    "details": {}
  }
}
```

后端本地开发默认使用 MySQL 8.4，连接信息通过环境变量提供。必须提供 `backend/docker-compose.yml` 作为本地 MySQL 服务的统一启动方式，但不能提交真实密码。所有数据库访问通过 Spring Data JPA Repository/Service 层，Controller 不直接写 SQL；数据库结构通过 Flyway migration 管理。

推荐的本地环境变量：

```text
DB_URL=jdbc:mysql://localhost:3306/ican_assistant?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=ican
DB_PASSWORD=ican_dev_only
```

## AI 输出规范

AI 调用必须经过 `backend/src/main/java/com/ican/assistant/core/ai/` 的适配层，页面不直接拼接模型请求。每个调用必须定义：

- 输入 JSON schema；
- 输出 JSON schema；
- 失败时的本地 fallback；
- 置信度或假设条件（适用时）；
- 用户可编辑/撤销入口。

开发阶段允许使用 mock AI 响应，不能因为没有 API Key 导致页面无法演示。mock 必须位于 service/adapter 层，不要把 mock JSON 散落到 Vue 组件中。

## 手机端交互要求

- 首屏优先显示当前用户、核心状态和模块入口，不做营销型落地页；
- 最小触控区域 44px；
- 关键操作固定在底部或可见区域；
- 图谱和分支图支持横向滚动、缩放或折叠；
- 任何动态内容都不能导致布局跳动；
- 空状态、加载、错误和无数据状态都要实现；
- 页面不能依赖 hover 才能理解核心信息。

具体断点、按钮尺寸、页头结构、卡片使用边界、状态颜色和禁用样式以 `plans/00-ui-style-contract.md` 为准。本节与视觉合同冲突时，以视觉合同为准。

## 分支与合并规则

每个人在自己的 Git 分支开发：

```text
feature/ability-home
feature/decision-sandbox
feature/module3-shell
```

提交前必须：

1. `npm install` 后前端可以启动；
2. `mvn test` 成功，或使用项目约定的 Maven Wrapper 执行测试；
3. `docker compose -f backend/docker-compose.yml up -d` 可以启动本地 MySQL，随后 `mvn spring-boot:run` 可以启动后端；
4. `npm run build` 成功；
5. 后端接口测试或模块手动验收通过；
6. 不提交 API Key、数据库密码、MySQL 数据目录、个人数据或大体积二进制文件；
7. 在提交说明中列出前端/后端修改文件、运行方式、环境变量、数据库迁移、已知问题和集成注意事项。

前端提交还必须附带：复用的公共视觉类、新增样式前缀、桌面/手机验收结果和页面级横向溢出检查。

合并顺序：先合并队员3的 Spring Boot/MySQL/AI 基础骨架，再合并负责人的 Vue3 主页、共享前端结构和模块1，之后合并决策沙盒，最后确认第三模块占位骨架。发生冲突时优先保留共享类型、API 路径、数据库迁移和路由注册方式，不直接删除队友目录。

## 统一完成标准

一个模块只有同时满足以下条件才算完成：

- 有独立路由和移动端布局；
- 有真实可点击的核心交互，而不是静态截图；
- 有 mock 数据，断网或没有模型密钥也能演示；
- AI 输入、输出和失败状态有明确处理；
- 有至少一个从输入到可视化结果的完整流程；
- 不依赖其他模块内部实现；
- 能在 5 分钟总演示中被清楚展示。
