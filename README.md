# 校园信息系统 · 统一认证与门户

DBA 侧代码仓库：统一认证服务、登录页与模块门户、各组接入组件，以及接口规范和组间约定文档。

## 目录

| 路径 | 内容 |
| --- | --- |
| `backend/` | 统一认证服务（Spring Boot 3.4 + MySQL，Java 17） |
| `web/` | 登录页与模块门户，构建时打包进认证服务的静态目录 |
| `client/` | 提供给各业务组的接入组件（`edu.campus.authclient`） |
| `接口规范_v1.md` | 认证服务对外接口、Cookie、核验接口与状态码约定 |
| `给各组的接入通知.md` | 发给 15 个组的接入建议与交付要求 |
| `docs/DBA工作梳理.md` | 整体方案、分工与部署安排 |

## 快速开始

```bash
# 1. 建表（在 account 库执行一次）
mysql -h <地址> -P 3306 -u <账号> -p account < backend/schema.sql

# 2. 启动认证服务（默认 18080）
export DB_URL='jdbc:mysql://<地址>:3306/account?allowPublicKeyRetrieval=true'
export DB_USER='<账号>'
export DB_PASSWORD='<密码>'
export AUTH_INTERNAL_SECRET='<与各组约定的密钥>'
cd backend && mvn spring-boot:run
```

启动后访问 `http://127.0.0.1:18080/` 是登录页，登录成功进入 `/portal.html`。

本地没有 MySQL 时可用内存库跑通流程：

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 模式会自动建表并写入三个演示账号（`stu01` / `teacher01` / `staff01`，密码 `campus123`）。

## 账号

账号统一存放在 `account` 库的 `account` 表：登录名、BCrypt 密码哈希、身份、业务编号。一个身份一个账号，登录时不需要选择身份。

学生账号已从 `student_status_mgmt.student` 导入，脚本见 `backend/seed-student-accounts.sql`，初始密码 `Campus@123`。

## 接入方式

见 [client/README.md](./client/README.md)：复制 `authclient` 包 → 启动类加 `@Import` → 配置 `service-url` 与 `internal-secret` → 业务代码用 `CampusUserContext.get()` 取当前用户。

## 部署

同源反向代理把 `/api/auth/` 转发到认证服务，`/modules/m01/` 与 `/api/m01/` 转发到各组服务。正式环境启用 HTTPS，并设置 `COOKIE_SECURE=true`。
