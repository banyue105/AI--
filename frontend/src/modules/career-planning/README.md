# 模块四：职业规划

首页只有一个“职业规划”入口，进入后可切换三个子页面：

- `/module4/match`：按当前能力图谱计算演示岗位的参考匹配度，并保存意向方向和城市。
- `/module4/jobs`：按关键词、方向、城市、类型筛选本地演示岗位，可保存目标岗位。
- `/module4/jd`：带入目标岗位或粘贴 JD，识别技能要求、对照能力等级，生成匹配点、缺口与行动建议，并保存报告快照。

岗位、公司和薪资均为演示数据。匹配与 JD 分析使用明确的技能词规则；参考分数不是录用概率。当前没有接入真实招聘平台或简历生成功能。用户偏好、目标岗位和报告由后端保存；报告保留生成时的结果。

需同时启动前端与后端。前端默认端口 5173，后端 API 前缀 `/api/v1/career`。本机若由其他程序占用后端默认的 8000 端口，可在 `backend/.env` 设置 `SERVER_PORT=8001`，并在 `frontend/.env.local` 设置 `API_PROXY_TARGET=http://127.0.0.1:8001`。

主要接口：

```text
GET  /api/v1/career/jobs
GET  /api/v1/career/matches
GET  /api/v1/career/preferences
PUT  /api/v1/career/preferences
GET  /api/v1/career/target
PUT  /api/v1/career/target
GET  /api/v1/career/reports
POST /api/v1/career/reports
GET  /api/v1/career/reports/{id}
```
