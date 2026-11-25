# 统一登录接入组件（各组使用）

把 `src/main/java/edu/campus/authclient/` 整个包复制到自己项目的 `src/main/java/edu/campus/authclient/`，
然后在启动类加一行 `@Import(CampusAuthConfiguration.class)`，再配置三段参数即可。

## 1. 依赖

组件只依赖 Spring Boot Web 自带的包，不需要额外加依赖：

- `spring-boot-starter-web`（提供 Jackson、Servlet、Filter）
- 使用 JDK 11+ 自带的 `java.net.http.HttpClient` 调用认证服务

## 2. 配置

在 `application.yml` 中：

```yaml
campus:
  auth:
    service-url: http://127.0.0.1:18080
    internal-secret: ${AUTH_INTERNAL_SECRET}
    cookie-name: CAMPUS_SESSION
    protected-prefixes:
      - /api/
    exclude-prefixes:
      - /api/auth/
      - /actuator/
      - /error
```

`service-url` 在最终部署时改成内网地址；`internal-secret` 由 DBA 单独发给各组，不能写进前端。

## 3. 启动类

```java
@SpringBootApplication
@Import(CampusAuthConfiguration.class)
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 4. 业务代码中读取当前用户

```java
@RestController
@RequestMapping("/api/m01")
public class StudentController {

    @GetMapping("/me")
    public Map<String, String> me() {
        CampusUser user = CampusUserContext.get();
        return Map.of(
            "user_id", user.userId(),
            "name", user.name(),
            "role", user.role(),
            "business_id", user.businessId()
        );
    }
}
```

- `user.role()` 是当前身份：`STUDENT` / `TEACHER` / `STAFF` / `COUNSELOR` 等。
- `user.businessId()` 是该身份对应的业务编号（学号、工号），用它去查本组的业务表。
- 权限判断仍由各组自己做：例如只有 `TEACHER` 才能录入成绩，学生只能查 `business_id` 等于自己的记录。

## 5. 未登录时的行为

- 没带 Cookie、Cookie 失效、认证服务返回 401 → 本组接口返回 `401`，响应体：

```json
{"error":"unauthenticated","message":"未登录或登录已过期"}
```

- 认证服务连不上 → 返回 `503`，不会放行成匿名请求。这样故障时不会误开放数据。

## 6. 注意事项

1. 只有 `/api/` 开头的路径会被拦截，静态页面和 `/error` 不受影响。
2. 核验结果默认缓存 5 秒，退出登录后最多 5 秒内旧请求仍可能通过；需要立即失效可把 `cache-ttl` 调整为 0。
3. 该组件只负责“你是谁”，不负责“你能做什么”，记录级权限必须由各组后端判断。
4. 组件会调用认证服务，本地联调时认证服务必须先启动。
