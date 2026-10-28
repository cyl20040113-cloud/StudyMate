# StudyMate · 伴学星球

面向学习组队场景的个人学习实践项目。本仓库统一存放前后端源码，方便查看代码、搭建开发环境与展示项目。

## 功能与技术栈

- 用户注册、登录、退出登录、个人信息维护；标签搜索、用户推荐与匹配。
- 队伍创建、查询、修改、加入、退出与删除，支持公开、私有和加密队伍。
- Redis 推荐列表缓存、Redis Session、定时预热与 Redisson 分布式锁。
- 后端：Java 8、Spring Boot 2.6.4、MyBatis-Plus、MySQL 8.0+、Redis、Redisson。
- 前端：Vue 3、TypeScript、Vite、Vant、Axios。
- 构建：Maven、npm；backend 中保留原有 Dockerfile。

依赖版本以 backend/pom.xml、frontend/package.json 和锁文件为准，本次整合未修改依赖声明或升级已有锁定版本；仅补齐 npm 锁文件缺失的跨平台可选依赖记录，以便 npm ci 可重复安装。

## 仓库结构

~~~text
StudyMate/
├── backend/          Spring Boot 后端、已有测试及 Dockerfile
├── frontend/         Vue 前端
├── sql/schema.sql    MySQL 初始化表结构，不含业务数据
├── .env.example      环境变量参考
└── .gitignore        构建产物、依赖与本地配置忽略规则
~~~

## 本地启动

准备 JDK 8、Maven 3、Node.js 24、npm，以及已启动的 MySQL 8.0+ 和 Redis。

### 初始化数据库

在空的开发数据库环境中，通过 MySQL 客户端执行，路径替换为实际克隆位置：

~~~sql
SOURCE D:/项目/StudyMate/sql/schema.sql;
~~~

脚本创建 studymate 数据库及 tag、user、team、user_team 表，不含业务记录，不删除已有表。已包含有效用户账号、星球编号及用户队伍关系的唯一约束。

backend/src/main/resources/db/schema-integrity.sql 是原有数据库的手工迁移参考。新建库使用 schema.sql 后，不要重复执行其中的 ALTER 语句。

### 启动后端

在 PowerShell 中设置本地配置：

~~~powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<填写你的本地数据库密码>"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_DATABASE = "1"
# Redis 配置了认证时，再设置 REDIS_PASSWORD。
cd backend
mvn spring-boot:run
~~~

后端地址为 http://localhost:8080/api；可用 DB_URL 指定数据库连接。

.env.example 只是配置参考，Spring Boot 不会自动读取。通过环境变量或 IDE 运行配置传入实际配置，不提交真实密码。

prod profile 默认数据库主机为 mysql、Redis 主机为 redis。现有 Dockerfile 只构建并运行后端，不自动启动数据库、Redis 和前端。

### 启动前端

在另一个终端中：

~~~powershell
cd frontend
npm ci
npm run dev
~~~

打开 Vite 输出的地址，默认 http://localhost:5173。开发代理将 /api 转发到 http://localhost:8080，Axios 使用 Cookie 传递登录会话。

生产构建输出在 frontend/dist。开发代理仅在开发服务器生效，部署时需要另行配置 /api 反向代理。

## 验证

~~~powershell
cd backend
mvn compile
mvn test

# 仓库根目录的另一个终端
cd frontend
npx --no-install vue-tsc -b
npm run build
~~~

已有常规后端测试使用 test profile、H2 数据库和模拟的 RedissonClient。批量写库与外部 Redis 手工测试原本已禁用；常规测试通过不等于真实 MySQL、Redis 或并发场景验证通过。

## 调用链与实现说明

- 页面 → Axios /api → Vite 开发代理 → Controller → Service → Mapper → MySQL。
- Spring Session Redis 保存登录状态；Service 校验队伍权限与业务约束。
- 队伍写操作使用 Redisson 锁和数据库事务，数据库提供有效记录唯一约束。
- 推荐缓存 Key：studymate:user:recommend:{userId}:{pageNum}:{pageSize}，普通查询回填 TTL 为 30 秒。
- 每天北京时间 12:00 预热用户 ID 1 的第一页 20 条记录，TTL 为 12 小时，以独立 Redisson 锁避免多实例重复预热。
- 当前推荐缓存依赖 TTL 失效，没有随用户信息更新主动清理；预热数据可能在 TTL 内保留旧值。没有额外实现空值缓存、查询互斥重建与随机过期，不声称已完成缓存穿透、击穿及雪崩治理。

## 仓库来源

从原有项目的本地源码快照整理为统一仓库，保留代码中的署名和注释。历史提交保留在原仓库，本次不改写或删除：

- 后端：https://github.com/cyl20040113-cloud/StudyMate-backend
- 前端：https://github.com/cyl20040113-cloud/StudyMate-frontend

本仓库不含原业务数据库导出记录、依赖目录、构建产物及个人学习笔记。

## 本次整合验证（2026-10-28）

| 检查 | 结果 |
| --- | --- |
| 后端 mvn compile | 通过 |
| 后端 mvn test | 9 项执行通过，3 项原有手工测试跳过，0 失败、0 错误 |
| 前端 npm ci | 通过 |
| 前端 vue-tsc -b | 通过 |
| 前端 npm run build | 通过 |
| 源码一致性 | 55 个后端源文件及 35 个前端源文件与原目录哈希一致；配置文件单独核对 |
| 依赖版本 | 132 项原锁定版本保持不变，补齐 24 项跨平台可选依赖记录 |
| 提交内容 | 排除原仓库 .git、依赖、构建产物及原业务数据记录 |

本次使用本地构建与 H2/模拟依赖测试，没有重新部署到生产环境，也没有连接真实业务数据库或 Redis 做回归。

本次新增或调整：根目录 README、.gitignore、.env.example、sql/schema.sql、前端 README、前端 npm 锁文件，以及后端默认配置中的数据库环境变量。业务 Java、Vue 与 TypeScript 源码保持原样。

相关 Java 学习知识点：Spring Session 与 Cookie、DTO/VO 分层、Redisson 看门狗与锁释放顺序、事务提交与并发一致性、逻辑删除下的数据库唯一约束。
