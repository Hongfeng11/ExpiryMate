# ExpiryMate 后端

基于 Java 21、Spring Boot 3.5、Spring Web、Spring Data JPA、Flyway 与 MySQL 8.0.16+。后端独立于 Vue 前端，默认 API 地址为 `http://localhost:8080`。

## 本地启动

1. 安装 JDK 21、Maven 3.6.3+ 和 MySQL 8.0.16+。
2. 创建数据库：`CREATE DATABASE expirymate CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`
3. 设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量，或修改本机环境配置。
4. 在本目录运行 `mvn spring-boot:run`。Flyway 会在数据库中执行初始迁移。
5. 在 `frontend/.env.local` 中设置 `VITE_API_BASE_URL=http://localhost:8080`，重启 Vite。

API：`GET/POST /api/v1/items`、`GET/PATCH/DELETE /api/v1/items/{id}`。列表响应含 `items` 数组，字段使用 camelCase，和当前前端请求约定一致。

## 当前边界

默认 `local-demo` 模式使用固定演示用户，便于当前尚无登录界面的前端连通数据库。它只用于本机开发，不具备身份认证或多用户隔离能力；部署到共享或生产环境前，必须实现 SRS 中的注册/登录、Spring Security 会话认证，并从认证主体解析 userId，不能继续使用演示用户配置。提醒偏好、通知生成、Web Push 和仪表盘汇总 API 尚未实现。

项目数据库定义以 `ExpiryMate-doc/ExpiryMate-database.sql` 为评审参考；运行时结构由 `src/main/resources/db/migration` 中 Flyway 迁移管理。
