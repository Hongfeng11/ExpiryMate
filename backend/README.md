# ExpiryMate 后端

基于 Java 21、Spring Boot 3.5、Spring Security、Spring Data JPA、Flyway 与 MySQL 8.0.16+。后端独立于 Vue 前端，默认 API 地址为 `http://localhost:8080`。

## 本地启动

1. 安装 JDK 21、Maven 3.6.3+ 和 MySQL 8.0.16+。
2. 创建数据库：`CREATE DATABASE expirymate CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`
3. 设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量。
4. 在本目录运行 `mvn spring-boot:run`。Flyway 会按版本迁移数据库。
5. 前端默认访问 `http://127.0.0.1:8080`。如使用其他主机名，在 `frontend/.env.local` 配置 `VITE_API_BASE_URL`，并使其与浏览器使用的主机名一致。

## 账号与共享清单

- `POST /api/v1/auth/register`、`POST /api/v1/auth/login`、`GET /api/v1/auth/me`、`POST /api/v1/auth/logout` 使用服务端会话 Cookie。
- `GET/POST /api/v1/lists` 用于查看和创建清单。
- `POST /api/v1/lists/{listId}/invitations` 生成 3 天有效的一次性邀请口令；角色可选 `EDITOR` 或 `VIEWER`。
- `POST /api/v1/invitations/accept` 使用口令加入清单。
- 物品接口位于 `/api/v1/lists/{listId}/items`；每次请求都会按当前登录账号检查清单成员身份和角色。
- `GET/PUT /api/v1/settings/reminders` 保存当前账号自己的提醒偏好。

清单角色：`OWNER` 管理清单并邀请成员，`EDITOR` 查看和修改物品，`VIEWER` 只读。邀请口令只在创建时返回原文，数据库只保存其 SHA-256 摘要。当前通过复制口令手动邀请，尚未接入邮件发送。

物品列表支持组合使用 `category`、`location` 和 `q`（名称、分类、位置、备注关键词）筛选；响应字段使用 camelCase。有效到期日按包装到期日或生产日期加未开封保质期计算；录入开封日期及开封后期限后，取两种期限中较早的日期。

## 当前边界

提醒偏好 API 已实现；后台定时生成通知、Web Push、成员管理（移除成员/转让所有权）和邀请邮件尚未实现。部署到公网前还应配置 HTTPS、安全的会话 Cookie、邮件验证和登录限流。

项目数据库定义以 `ExpiryMate-doc/ExpiryMate-database.sql` 为评审参考；运行时结构由 `src/main/resources/db/migration` 中 Flyway 迁移管理。V5 为旧用户创建默认清单，并将原有物品关联到对应清单。
