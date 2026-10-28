# PixForge

基于 Next.js、Spring Boot、MyBatis 和 MySQL 的图片素材创作工作台。用户可浏览模板、上传图片并提交异步制作任务；任务在 Java 后台处理，历史记录、阶段状态和图片由服务端保存。生成器只做可控制耗时的模拟：成功结果复制第一张已上传图片，不调用 AI。

## 完成范围

- 8 个数据库种子模板，支持分类筛选、详情和不同素材数量要求。
- 后端校验并保存 JPEG/PNG 图片；按演示用户隔离素材、任务和结果访问。
- Java 事务中创建任务与三个阶段，提交返回 HTTP 202；固定大小线程池后台按“素材检查 → 模拟生成 → 结果整理”执行，可指定阶段失败和耗时。
- 任务与阶段写入 MySQL，支持分页历史、未完成任务和单任务查询；队列满和服务重启遗留任务会记录失败原因。
- 前端默认使用真实 Java API，提供上传反馈、多任务追踪、请求错误保留旧数据、刷新恢复和演示用户切换。设置 `NEXT_PUBLIC_USE_DEMO_API=true` 可使用仅存于浏览器的独立演示适配器。

## 技术版本与结构

| 部分 | 版本 / 位置 |
| --- | --- |
| 前端 | Next.js 16.4.3、React 19.3.1、TypeScript 5.9.3；`frontend/` |
| 后端 | Java 17、Spring Boot 3.3.5、MyBatis starter 3.0.4；`backend/` |
| 数据库 | MySQL 8.0；`backend/src/main/resources/schema.sql` 和 `data.sql` |
| 素材 | `assets/templates/` 原始演示素材，`frontend/public/templates/` 运行时副本；来源见 [素材来源](assets/SOURCES.md) |
| CI | [GitHub Actions](.github/workflows/ci.yml) |

## 启动

推荐在仓库根目录运行：

```bash
docker compose up --build
```

Compose 启动 MySQL、Java 和 Next.js，首次启动时后端自动建表和写入演示用户 A/B 及 8 个模板。访问 `http://localhost:3000`；后端健康检查为 `http://localhost:8080/api/health`。MySQL 数据和上传文件分别保存在 `mysql_data`、`uploads_data` 卷。Compose 中的数据库密码只用于本地演示，不应用于公网部署。

本机分别运行需要 Node.js 22、npm、JDK 17、Maven 3.9 和 MySQL 8.0。先创建数据库及用户，例如：

```sql
CREATE DATABASE pixforge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'pixforge'@'localhost' IDENTIFIED BY 'pixforge';
GRANT ALL PRIVILEGES ON pixforge.* TO 'pixforge'@'localhost';
```

```bash
cd backend
mvn test package
java -jar target/pixforge-service.jar
```

```bash
cd frontend
npm ci
npm run dev
```

前端通过 Next.js 同源代理访问 Java；本机代理默认指向 `http://localhost:8080`。在其他地址运行后端时设置 `BACKEND_URL`。生产构建须在**构建时**提供此变量（Compose 已通过前端构建参数传入）；开发服务器启动前设置即可。前端默认调用真实接口；`NEXT_PUBLIC_USE_DEMO_API` 也是构建期变量，改变后须重新构建。后端配置项：`DB_URL`、`DB_USER`、`DB_PASSWORD`、`STORAGE_ROOT`、`DEMO_CONTROLS_ENABLED`、`WORKER_COUNT`、`QUEUE_CAPACITY`、`PORT`。默认数据库地址为 `localhost:3306/pixforge`，上传目录为 `./data`，工作线程 2 个、等待队列 16 个。

## 演示

1. 在首页筛选模板，进入详情并上传要求数量的 JPEG/PNG 图片（每张不超过 5 MB）。
2. 展开“演示设置”，将每阶段耗时设为 1、2、4 或 8 秒；失败阶段可选素材检查、模拟生成或结果整理。提交后立即进入“我的制作”。
3. 制作期间继续提交其他任务，观察排队和各阶段状态。刷新历史页后记录从 MySQL 恢复，未完成任务继续轮询。
4. 右上角切换演示用户 A/B，验证各自只能查看自己的任务和图片。

`DEMO_CONTROLS_ENABLED=false` 时，Java 拒绝带有 `simulation` 对象的提交请求；此配置适用于不开放演示控制的环境。当前前端始终会提交该对象，关闭演示控制时需同步调整前端请求。

## 接口与状态

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET / POST | `/api/demo/session` | 获取或切换演示用户；POST 请求为 `{"demoKey":"A"}` 或 `B` |
| GET | `/api/templates?category=...`、`/api/templates/{id}` | 分类、模板列表及详情 |
| POST | `/api/assets` | `multipart/form-data` 字段 `file`；返回 `id`、`previewUrl`、文件信息 |
| GET | `/api/assets/{id}/content` | 预览本人图片 |
| POST | `/api/tasks` | 返回 HTTP 202 和任务快照；请求示例见下方 |
| GET | `/api/tasks?page=0&size=20` | 个人历史，返回 `items`、`total`、`page`、`size` |
| GET | `/api/tasks?status=ACTIVE` | 只查询 `QUEUED` / `RUNNING` |
| GET | `/api/tasks/{id}`、`/api/tasks/{id}/result` | 任务详情或本人成功图片 |

```json
{
  "templateId": "travel-postcard",
  "assetIds": ["已上传素材的 UUID 1", "已上传素材的 UUID 2"],
  "simulation": { "failAtStage": "GENERATE", "stepDelayMs": 2000 }
}
```

整体状态为 `QUEUED → RUNNING → SUCCEEDED / FAILED`，阶段为 `CHECK_ASSETS → GENERATE → FINALIZE`。每阶段保存独立的 `PENDING / RUNNING / SUCCEEDED / FAILED` 状态、时间和失败原因。终态不会因查询回退。任务查询返回 `id`、`templateId`、`templateTitle`、`templateCoverUrl`、`status`、`currentStage`、`steps`、带 UTC 偏移的 `createdAt`、`resultUrl`、`failureCode`、`failureMessage`。错误响应包含 `code` 和 `message`，字段校验错误还包含 `fields`。

Java 会话决定当前演示用户；新会话默认为 A，POST 会话接口可切至 B。不存在或不属于当前用户的私人资源统一返回 404。前端对轮询请求失败保留最近任务数据；只有服务端任务状态为 `FAILED` 才显示制作失败。

## 验证与限制

前端检查命令：`npm run lint`、`npm run typecheck`、`npm run test`、`npm run build`。后端检查命令：`mvn test package`。CI 在 push 和 pull request 运行这些检查。当前后端单元测试覆盖阶段顺序、指定阶段失败后停止、终态保护、用户任务隔离；数据库和 HTTP 全流程尚无自动集成测试。

已知限制：成功结果仅复制第一张素材；没有真正图像处理。单实例的会话和执行器不适合横向扩容。重启后上次遗留的 `QUEUED` / `RUNNING` 任务会被标记为 `SERVICE_INTERRUPTED`，不会续跑。已受理任务在队列满时标记为 `EXECUTOR_OVERLOADED`。数据库表结构通过启动 SQL 初始化，尚无版本化迁移；已有表不会自动升级。上传文件与数据库记录的写入并非一个原子事务，存储故障可能留下孤立文件。后端生产部署应配置独立凭据、持久化存储和正式身份系统。

本轮在本机完成 Java 单元测试与打包、前端 lint、类型检查、测试和构建。临时 MySQL 8.0 实例上的 HTTP 验证覆盖建表及 8 个种子模板、上传、成功与指定阶段失败、结果读取、用户隔离和重启中断；生产构建的 Next.js 同源代理也通过上传及任务提交验证。当前执行环境没有 Docker，因此 Compose 镜像构建与容器启动尚未实际验证。部署时先备份数据库和上传卷，再更新代码并运行 `docker compose up --build -d`；回滚时恢复先前镜像/代码及相应数据备份。不要在需要保留历史时使用 `docker compose down -v`。

项目先前阶段约投入 20 分钟，本轮后端开发与配置约投入 30 分钟。使用 AI 编程工具阅读技术方案、实现数据库映射/任务服务及测试，再依据实际编译和测试结果修正代码；例如 Windows 本机 Maven 测试需加 `-DforkCount=0` 避开跨盘符 Surefire 启动问题。源码、测试结果和上述未验证边界分别记录。
