# 首页改版实施与验收记录

验收日期：2026-09-29。依据 [06 首页改版任务书](06-homepage-redesign-prompt.md)，已在现有 Vue 3 + TypeScript 工程内完成首页重排、应用级导航与四模块亮点定位。默认预览地址：<http://127.0.0.1:5173/>。

发布提交基于最新 `origin/main` 的 `716d027`，在独立工作区整理首页增量，并以 <http://127.0.0.1:5174/> 验证。原开发工作区的模块二、三 UI 改动仍保留在本地；本次只将介绍组件、页头偏移和主内容标识接入远端现有业务页面，保留远端的能力保存与 API 失败处理逻辑。

发布验证发现远端能力页的知识目录请求失败会产生未处理 Promise；已在页面初始化中接入现有错误反馈，保留介绍区可访问，不修改目录 service 或伪造成功数据。

## 实施结果

首页顺序为「全局顶部导航 → 项目介绍 Banner → 四模块简介入口 → 四模块亮点图文 → 简洁页尾」。Banner 使用真实产品界面制作的本地图片，项目介绍和按钮保留为 HTML；主要按钮定位 `/#home-modules`，次要按钮进入 `/ability`。

应用级导航只在 `App.vue` 渲染一份。桌面横向展示首页与四模块；手机使用可展开菜单，支持 Tab、Escape、选中后关闭与焦点恢复。实践详情及职业规划三个子页保持正确的模块高亮，职业规划继续保留模块内标签。

中部入口进入默认工作页面；下部亮点按钮进入对应介绍区。模块介绍不依赖业务数据加载，普通进入时折叠，从亮点链接进入时展开并定位；介绍末尾的按钮定位到原有工作区。支持直接访问、带 hash 刷新、重复点击、返回后重新进入与历史滚动恢复。

## 路由与锚点

| 模块 ID | 默认入口 | 亮点介绍 | 工作区锚点 | 当前状态 |
| --- | --- | --- | --- | --- |
| `ability-growth` | `/ability` | `/ability#ability-highlights` | `ability-workspace` | `ready` / 可使用 |
| `decision-sandbox` | `/decision` | `/decision#decision-highlights` | `decision-workspace` | `ready` / 可使用 |
| `module3` | `/module3` | `/module3#practice-highlights` | `practice-workspace` | `prototype` / 演示原型 |
| `module4` | `/module4/match` | `/module4/match#career-highlights` | `career-workspace` | `prototype` / 演示原型 |

首页另有 `home-top`、`home-modules`、`home-highlights`。既有 `/module3/:projectId`、`/module4/jobs`、`/module4/jd` 和 `/module4` 重定向继续保留；未新增业务接口或数据库迁移。

## 主要文件

| 文件 / 范围 | 本次修改 |
| --- | --- |
| `frontend/src/pages/HomePage.vue`、`home.css` | 项目介绍、四模块入口、交错亮点预览、手机单列与图片失败回退 |
| `frontend/src/app/AppNavigation.vue`、`navigation.css` | 全站导航、手机菜单、跳过导航链接、共享介绍区样式 |
| `frontend/src/app/modulePresentation.ts` | 四模块共用的类型化简介、亮点、图片及锚点配置 |
| `frontend/src/app/ModuleHighlights.vue` | 静态亮点介绍、可展开区域与进入工作区按钮 |
| `frontend/src/app/scrollBehavior.ts`、`router.ts` | hash 定位、异步挂载、焦点、历史恢复与减少动态效果 |
| `frontend/src/App.vue`、`core/api/homeService.ts` | 挂载一份全局导航，导出现有 fallback 模块注册配置供展示复用 |
| 四模块页面及其必要 CSS、`styles/main.css` | 增加介绍组件和主内容标识；调整工具栏和既有锚点吸顶偏移 |
| `frontend/src/assets/home/` | 五张本地 JPEG 产品预览及素材来源说明 |
| `frontend/tests/home-e2e.mjs`、`frontend/package.json` | 新增 `npm run test:home`，未新增依赖 |
| 根目录 / 前端 README、`plans/00-*`、`01-*`、`04-integration-review.md`、计划索引 | 同步首页结构、全局导航、四模块状态及验收要求 |

保留已有业务 store、service、计算规则、数据模型和后端文件。原开发工作区开始前的未提交变更已保留；核对了范围外 47 个已有变更文件的 SHA-256，内容未被本次实施覆盖。发布提交不修改后端及各模块业务 store、service。

## 素材与视觉复用

四张模块预览来自已运行的合成演示界面：能力图谱、基准与变更方案、复盘材料与原文、演示岗位与技能对照。Banner 使用其中能力、决策、职业界面制作产品展示构图；实践界面单独展示在对应亮点区。详细来源与尺寸见 [素材说明](../frontend/src/assets/home/README.md)。

素材随工程保存，合计约 369 KB；首屏 Banner 优先加载，四张下部图片懒加载，均设置尺寸、替代文本和失败回退。首页未挂载完整模块业务页面，也未引入临时外链或虚构业务结果。

复用现有 DM Sans / Noto Sans SC、颜色 token、按钮、状态标签、5–8px 圆角和 900px / 620px 断点。首页正文宽度为 1120px；模块工作区仍沿用原有布局。新增样式主要使用 `.home-*`、`.app-nav-*`、`.app-module-*` 前缀；仅增加布局变量 `--app-nav-height` 和 `--app-module-header-height`，用于统一桌面 64px、手机 58px 的吸顶偏移。

共享视觉合同及模块一任务书中旧版个人概览、三模块、底部导航与首页禁止 Banner 的条款已按本次授权调整。模块内部仍以业务操作为主，继续遵守原有视觉合同。

## 实际验证

在现有前后端服务及 Chrome 上完成：

| 检查 | 结果 |
| --- | --- |
| `npm run build` | 通过：Vue TypeScript 检查与 Vite 生产构建成功 |
| `npm run test:home` | 39 项通过；1280×720、360×800 两种视口均实测 |
| 页面级横向溢出 | 两种视口：首页和已检查模块均为 0 |
| JavaScript / 控制台 | 无页面异常、无非预期控制台错误 |
| Git diff 空白检查 | 通过；按仓库 Windows 换行规则检查 |

首页测试覆盖结构、全部图片加载、四个普通入口、四个亮点跳转、刷新与重复 hash、历史恢复、子路由高亮、手机键盘菜单、焦点、减少动态效果、API 不可用及 Banner 失败回退，并回归能力节点 / 列表和职业规划筛选 / 空态 / 详情 / JD 带入。

原开发工作区另通过了模块二、三的 13 项 UI 回归，涵盖图 / 列表、方案与版本比较、引用定位、弹窗和手机布局；这些 UI 改动及测试脚本尚未包含在本次首页发布中，其结果不能替代发布版本的验收。原有缺失的 `/favicon.ico` 404 作为基线资源提示处理，未计入新增错误。

首页验收采用只读浏览器拦截，不写入后端记录。能力页原有初始化会使用 POST 计算路径；测试拦截 `/api/v1/ai/explainable-path` 和 `/api/v1/ability/path`，因此该项验证的是导航及前端交互，不代表真实计算服务或业务保存已重新验收。未修改后端，未扩大为后端迁移或远程 AI 验证。

本地原始报告与截图：

- [首页 39 项报告](../frontend/test-results/home-redesign/acceptance.json)
- [桌面首屏](../frontend/test-results/home-redesign/home-first-screen-1280.png)、[手机首屏](../frontend/test-results/home-redesign/home-first-screen-360.png)
- [桌面完整首页](../frontend/test-results/home-redesign/home-1280.png)、[手机完整首页](../frontend/test-results/home-redesign/home-360.png)

以上生成文件位于忽略的 `frontend/test-results/`，可通过对应 npm 命令重新生成。

## 保留的业务边界

模块一的 `ready` 表示入口可使用，并不表示全部后端能力完成。模块二数值来自规则估算；模块三反馈为参考记录，能力证据写入与决策反馈消费仍待联调；模块四岗位、公司、薪资为本地演示数据，技能词匹配与 JD 分析供规划参考，不代表真实招聘平台或录用概率。首页和模块亮点介绍均保留这些说明。
