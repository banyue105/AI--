# 拓界后端

Java 21 + Spring Boot 3.5.16 + MyBatis Spring Boot Starter 3.0.5 + Spring Data JPA + MySQL 8.4。MyBatis 承载模块一的资料、能力图谱与知识目录；JPA 承载决策沙盒和实践复盘。所有表结构由 Flyway 管理。

版本组合遵循 [MyBatis Starter 兼容说明](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)和 [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)：MyBatis Starter 3.0 系列适配 Spring Boot 3.2–3.5，Java 21 位于本项目 Spring Boot 版本的支持范围内。

## 启动

需要 JDK 21、可运行的 Docker Compose。仓库提供 Maven Wrapper，无需单独安装 Maven；首次执行需要网络下载 Maven 和依赖。

在项目根目录打开 PowerShell：

```powershell
cd backend
Copy-Item .env.example .env
```

首次配置时复制该文件。已有 `.env` 时保留并检查现有配置。编辑 `.env`，将 `DB_PASSWORD` 和 `MYSQL_ROOT_PASSWORD` 改为本地使用的密码，然后启动：

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

Linux/macOS 对应命令：

```bash
cd backend
cp .env.example .env
# 编辑 .env 中的密码后继续
docker compose up -d --wait
./mvnw spring-boot:run
```

默认 MySQL 端口为 `127.0.0.1:3307`，API 为 `127.0.0.1:8000`。应用启动时自动执行尚未应用的 Flyway 迁移，创建表和演示数据。Compose 使用命名卷保存数据库，普通服务重启会保留数据。

本地无 MySQL 时，可使用 H2 演示配置运行三个模块：`mvn spring-boot:run -Dspring-boot.run.profiles=demo`。默认数据保存在 `backend/target/demo-db.mv.db`。

合并后的迁移顺序为模块一 V1–V4、决策沙盒 V5、实践复盘 V6–V7。原 `feature/practice-review` 分支已执行 V1–V3 的数据库与这里版本号含义不同；不要直接复用该数据库，先备份并使用新库，已有业务数据需单独迁移。

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/v1/health
```

数据库连通时，健康检查返回 `status: "UP"`、`database: "UP"` 和 `aiProvider: "mock"`。另开终端在 `frontend` 中运行 `npm run dev`，Vite 会代理 `/api` 请求。

## 配置

Spring Boot 从当前工作目录读取可选 `.env`，因此请在 `backend` 目录运行以上命令。系统环境变量可覆盖该配置；不要提交真实 `.env`。

| 变量 | 默认值/用途 |
|---|---|
| `DB_HOST` | `127.0.0.1` |
| `DB_PORT` | `3307`，同时作为 Compose 暴露到本机的端口 |
| `DB_NAME` | `ican_assistant` |
| `DB_USERNAME` | `ican` |
| `DB_PASSWORD` | 应用数据库密码，必须自行设置 |
| `MYSQL_ROOT_PASSWORD` | Compose 初始化 MySQL 使用的 root 密码，必须自行设置 |
| `DB_URL` | 可选完整 JDBC URL；设置后覆盖 `DB_HOST`、`DB_PORT`、`DB_NAME` 拼出的地址 |
| `SERVER_ADDRESS` | `127.0.0.1` |
| `SERVER_PORT` | `8000` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173`，逗号分隔 |
| `AI_PROVIDER` | `mock`；配置真实模型时使用 `openai-compatible` |
| `AI_BASE_URL` | OpenAI 兼容接口根地址，例如 `https://api.openai.com/v1` |
| `AI_API_KEY` | 模型服务密钥；为空时自动使用本地 mock |
| `AI_MODEL` | 模型名称，默认 `gpt-4o-mini` |
| `AI_TIMEOUT_MS` | 模型请求超时，默认 `30000` |
| `AI_FALLBACK_TO_MOCK` | 模型不可用时是否允许本地回退，默认 `true` |
| `AI_DISABLE_THINKING` | 对支持该参数的模型关闭推理过程；百炼 Qwen 的结构化调用建议设为 `true` |

启用真实模型时，在 `backend/.env` 中配置：

```env
AI_PROVIDER=openai-compatible
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=替换为你的密钥
AI_MODEL=gpt-4o-mini
AI_TIMEOUT_MS=30000
AI_FALLBACK_TO_MOCK=true
AI_DISABLE_THINKING=false
```

模型服务需兼容 OpenAI 的 `/chat/completions` JSON 响应格式。密钥只保存在本地 `.env`，不要提交到仓库。

使用已有 MySQL 时，只需配置连接信息后启动应用。数据库需预先存在，应用用户需要执行 Flyway 建表及业务读写的权限。修改 `.env` 的 MySQL 初始化密码不会自动修改已存在数据卷中的账户密码。

## 接口

统一前缀 `/api/v1`。成功响应直接返回下表的对象或数组，不额外包装 `data`；日期使用 ISO 格式。

| 方法 | 路径 | 输入/行为 | 返回 |
|---|---|---|---|
| GET | `/health` | 执行数据库连通检查 | 健康状态与 `aiProvider` |
| GET | `/home` | 读取演示用户及模块入口 | `profile`、`currentGoal`、`recentActivity`、`modules` |
| GET | `/profile` | 读取固定演示用户 | `id`、`name`、`avatar`、`goals` |
| PUT | `/profile` | `name`、`avatar`、非空 `goals`；可传 `id: "demo-user"` | 更新后的用户资料 |
| GET | `/modules` | 读取模块注册清单 | 模块数组 |
| GET | `/ability/graph` | 读取技能、关系、证据和目标 | `AbilityGraph`，`source: "api"` |
| POST | `/ability/skills` | 完整 `SkillNode`；按 id 或规范化名称新增/合并 | 保存后的完整图谱 |
| PUT | `/ability/skills/{id}` | 完整 `SkillNode`；请求 id 必须与路径一致 | 更新后的完整图谱 |
| POST | `/ability/relations` | `from`、`to`、`type`、`confidence`；校验引用及前置关系循环 | 保存后的完整图谱 |
| POST | `/ability/skills/{id}/evidence` | `title`，可选 `id`、`note`、`createdAt` | 增加证据并关联技能后的图谱 |
| POST | `/ability/parse` | `{ "input": "我会 Python，想学 Docker" }` | 候选节点、候选关系、摘要和假设 |
| POST | `/ability/path` | `{ "nodes": [...], "relations": [...] }` | `GrowthPathStep[]`，不写入数据库 |
| GET | `/ability/skills/{id}/assessment/questions` | 为当前用户技能生成测评题 | 题目、选项、答案与解析 |
| GET | `/knowledge/catalog` | 合并预置与已保存方向 | `KnowledgeTrack[]` |
| POST | `/knowledge/generate` | `query` 和 `currentSkills` 数组 | 保存后的 `KnowledgeTrack`，`source: "mock"` |
| POST | `/ai/ability-update` | 自然语言能力描述 | 候选技能、熟练度、描述与关系 |
| POST | `/ai/skill-enrichment` | 新技能名称及现有技能摘要 | 描述、关系建议与测试题 |
| POST | `/ai/tech-stack` | 目标方向描述 | 方向、阶段及阶段技能，并保存到当前用户目录 |
| POST | `/ai/explainable-path` | 当前目标与技能 ID | 带节点说明和连线依据的学习路径 |

`SkillNode` 字段为 `id`、`name`、可选 `description`、`level`、`status`、`evidenceIds`、`x`、`y`。等级为 0–4；状态为 `mastered`、`developing`、`gap` 或 `target`。关系类型为 `prerequisite` 或 `related`，置信度范围为 0–1。

技能保存会验证证据引用；使用新增证据接口后再关联已有证据 ID。成长路径基于提交的完整图谱做拓扑排序，只将 `prerequisite` 视为前置依赖，以等级 2 作为能实践的判断阈值。

知识方向请求示例：

```json
{
  "query": "数据分析工程师",
  "currentSkills": [
    { "name": "SQL", "level": 2, "status": "developing" }
  ]
}
```

`query` 长度上限 120；`currentSkills` 必填、允许空数组，最多 200 项，每项验证名称、等级和状态。生成方向使用规范化查询的稳定 ID，同方向重试更新同一条记录。三个预置方向为 `frontend`、`backend`、`network`。

错误响应：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "请求参数不合法",
    "details": { "name": "不能为空" }
  }
}
```

常见状态码包括 400（参数/图关系无效）、404（用户或能力不存在）、409（名称或关联约束冲突）、503（数据库访问失败）。请求中的未知字段会被拒绝，前端应显示错误并允许用户重试。

## 架构与数据库

```text
src/main/java/com/ican/assistant/
  AssistantApplication.java       Spring Boot 入口
  api/                            用户、首页、健康检查与模块注册
  common/ApiExceptionHandler.java  统一错误响应
  core/config/WebConfig.java      CORS 配置
  core/ai/                        AbilityParser / KnowledgeGenerator 与本地实现
  modules/abilitygrowth/          DTO、Controller、Service、Mapper、成长路径规则
  modules/knowledge/              DTO、Controller、Service、Mapper
  modules/decisionsandbox/        决策沙盒 JPA、规则与 API
  modules/practicereview/          实践复盘 JPA、规则与 API
src/main/resources/
  application.yml                 连接、Flyway、MyBatis 和 Web 配置
  db/migration/
    V1__user_profile.sql           用户资料与目标文本
    V2__ability_growth.sql         技能、关系、证据、图谱更新时间与种子数据
    V3__knowledge_catalog.sql      已生成知识方向
    V4__knowledge_track_users.sql   知识方向用户归属
    V5__decision_sandbox.sql        决策沙盒
    V6__practice_review.sql         实践复盘
    V7__practice_unicode_capacity.sql  复盘 Unicode 容量
  knowledge/catalog.json          三个预置方向
src/test/
  java/                           MockMvc 集成测试
  resources/application-test.yml  H2 / 独立 MySQL 测试库配置
```

Controller 负责 HTTP 和 Bean Validation；Service 处理事务和业务校验；MyBatis `@Mapper` 接口使用参数绑定 SQL。Java record 用于请求、响应和查询结果。MyBatis 已开启下划线转驼峰及按参数名的构造器映射。

Flyway 是表结构的唯一迁移入口，JPA/Hibernate 只验证映射，不自动建表。新增业务变更新增递增版本的 SQL，保留已经应用的迁移文件。能力写操作会锁定演示用户的图谱状态行；知识方向将完整结构序列化到 `knowledge_track.track_json`。

## 测试

```powershell
cd backend
.\mvnw.cmd test
```

Linux/macOS 使用 `./mvnw test`。测试类激活 `test` profile，默认使用 H2 内存库的 MySQL 模式，并执行同一组 Flyway 迁移。

2026-09-17 验证：28 项后端测试分别在 H2 和隔离 MySQL 9.6 实例上全部通过，覆盖资料读写、图谱保存、关系校验、路径规则、输入解析、知识目录持久化与请求验证。可执行 JAR 打包及启动通过，数据库和 API 重启后新增技能与知识方向仍可读取。浏览器 1280×720、360×800 无页面横向溢出，新增、编辑、刷新、保存错误反馈和首次离线演示通过。

本次合并后，模块二、三的接口与并发测试共 13 项通过，V1–V7 在 H2 测试库执行成功。完整套件 57 项中 3 项失败；在未合并的 `codex/ability-tree-views` 提交 `2cf6602` 上单独运行对应模块一测试，也出现相同的 3 项断言失败。

Compose 配置校验通过，使用的 MySQL 8.4 容器尚未实跑，因为本机 Docker 引擎未运行。MySQL 9.6 实测时 Flyway 提示该数据库版本高于其已验证范围；三份迁移和接口测试均成功，但不能据此声称所有 MySQL 版本均已验证。

可通过以下变量将测试连接到预先创建的空 MySQL 专用测试库；不要使用已有业务数据的数据库，测试启动会执行迁移和种子数据初始化。

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3307/ican_assistant_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&sslMode=PREFERRED'
$env:TEST_DB_USERNAME = 'ican_test'
$env:TEST_DB_PASSWORD = '<测试库密码>'
$env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
.\mvnw.cmd test
```

这四个 `TEST_DB_*` 变量只覆盖测试数据源。应用正常启动仍使用 `DB_*` 配置。测试结束后，在新终端中运行可恢复默认 H2 行为。

打包并运行：

```powershell
.\mvnw.cmd package
java -jar target/assistant-backend-0.1.0-SNAPSHOT.jar
```

## 当前限制

- 尚未接入登录认证；当前使用 `X-User-Id` 作为临时用户边界。业务查询和写入已按用户隔离，后续可由认证主体替换该请求头。
- 未配置模型密钥时使用本地规则与模板，结果用于开发演示，不代表真实能力评估。
- 模型输出会经过字段归一化和本地回退，但仍属于候选建议；技能等级和学习路径需要用户结合实践确认。
- 自然语言解析不直接保存节点；成长路径不承诺精确学习时长，且不保存历史版本。
- 目标截止日期暂未提供编辑接口。
- 决策沙盒已可使用，实践复盘为可演示原型；实践反馈目前只保存为参考，尚未自动写入模块一或被模块二消费。
