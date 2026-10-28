# PixForge

基于 Next.js 的图片素材创作工作台。当前交付重点是**完整的前端演示与 Java 后端可运行框架**；后端上传、任务持久化和异步制作业务留待下一阶段实现。

## 当前完成范围

- 前端：模板分类、搜索、详情和效果预览；按模板数量上传图片、校验格式和大小、预览／替换／移除、上传进度与错误反馈；任务受理、多任务独立状态、三个阶段展示、失败原因和结果预览；刷新恢复、演示用户 A/B 隔离、只轮询未完成任务、查询错误保留旧数据。适配桌面与小屏。
- 前端默认使用 `localStorage` 演示适配器，因此不依赖后端业务也能完整体验上述流程。可设 `NEXT_PUBLIC_USE_DEMO_API=false` 切换到 Java API。演示任务与素材只存在当前浏览器，清理浏览器数据会丢失；演示结果使用预置示例图。
- 后端：Spring Boot 入口、MyBatis 和 MySQL 驱动依赖、健康检查、演示用户会话、7 个演示模板的列表与详情。**尚未接入数据库**，暂时排除了数据源自动配置。上传、任务、历史与图片私有访问接口尚未实现；关闭前端演示模式后，这些操作会显示接口错误。

## 技术版本与结构

| 部分 | 版本 / 位置 |
| --- | --- |
| 前端 | Next.js 16.4.3、React 19.3.1、TypeScript 5.9.3（锁文件版本）；`frontend/` |
| 后端 | Java 17、Spring Boot 3.3.5、MyBatis starter 3.0.4；`backend/` |
| 素材 | `assets/templates/` 原始演示素材；`frontend/public/templates/` 运行时副本；来源见 [`assets/SOURCES.md`](assets/SOURCES.md) |
| CI | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

## 启动

本机需要 Node.js 22、npm、JDK 17 和 Maven 3.9。前后端可分别运行：

```bash
cd frontend
npm ci
npm run dev
```

```bash
cd backend
mvn test package
java -jar target/pixforge-backend-0.1.0.jar
```

前端地址 `http://localhost:3000`，后端健康检查 `http://localhost:8080/api/health`。前端默认开启演示适配器；需要改用 Java API 时，在构建或启动前设置 `NEXT_PUBLIC_USE_DEMO_API=false`，并用 `BACKEND_URL` 指向 Java 服务（本机默认 `http://localhost:8080`）。`NEXT_PUBLIC_*` 变量会进入前端构建产物，切换后需重新构建。

也可使用 Docker：

```bash
docker compose up --build
```

容器中前端仍使用演示适配器，后端独立启动供健康检查和模板接口验证。当前阶段无需 MySQL；业务开发时再加入数据库服务、表结构和持久化配置。

## 演示方式

1. 在首页按分类筛选或搜索模板，进入详情。
2. 上传模板要求的图片。可测试错误格式、超出 5 MB、替换和移除。
3. 展开「演示设置」，选择每阶段 1–8 秒和可选的失败阶段，提交制作。
4. 在「我的制作」查看排队、素材检查、模拟生成、结果整理、完成／失败。制作期间继续创建任务；刷新页面后记录仍可恢复。
5. 右上角切换演示用户 A/B，查看各自独立的任务列表。

演示适配器按创建时间计算任务进度，因此关闭浏览器后再次打开仍能呈现应有的终态；这只是前端演示逻辑，**不代表 Java 后台执行或数据库持久化已经完成**。

## 当前 Java 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 返回 `{ "status": "UP", "service": "pixforge-backend" }` |
| GET / POST | `/api/demo/session` | 返回 `{ id, demoKey, displayName }`；POST 输入 `{ "demoKey": "A" }` 或 `B` |
| GET | `/api/templates?category=...` | 返回 `{ categories, items }`，分类可选 |
| GET | `/api/templates/{id}` | 返回模板详情与素材约束 |

前端接口层已经按技术方案预留 `POST /api/assets`、`POST /api/tasks`、`GET /api/tasks`、`GET /api/tasks/{id}`。计划中的任务状态为 `QUEUED → RUNNING → SUCCEEDED/FAILED`；阶段为 `CHECK_ASSETS → GENERATE → FINALIZE`。这些 Java 接口和 MySQL 表尚未实现，请勿将演示流程用于实际素材保存。

## 检查与交付状态

前端检查：`npm run lint`、`npm run typecheck`、`npm run test`（2 项）、`npm run build`。后端检查：`mvn test package`（1 项 MockMvc 冒烟测试）。CI 在 push 和 pull request 执行相同检查。当前环境没有 Docker，无法验证容器构建；浏览器也无法连接沙箱中的本地开发服务器，因此本轮没有完成浏览器目视验收。构建和接口测试已执行。

已知限制：前端演示数据仅保存在浏览器；演示上传未发送到 Java；后端没有数据库和业务执行器；服务重启恢复策略尚未实现。下一阶段应按技术方案补齐 MySQL 表、文件存储、任务受理、固定线程池、阶段记录、用户隔离和异常处理，再切换前端为真实 API 模式并做端到端验证。

部署时使用 `docker compose up --build -d`；回滚到先前 Git commit 后重新构建镜像。当前没有数据库迁移，故不存在数据回滚步骤。AI 编程工具用于阅读方案、搭建组件与 API 框架、补充测试，并以实际 lint、类型检查、构建和 Java 测试结果修正代码。本轮实际投入约 20 分钟，重点放在前端交互与可运行骨架。
