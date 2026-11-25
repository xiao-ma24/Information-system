package edu.campus.authclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 各组后端接入统一登录的公共过滤器。
 *
 * <p>流程：读取会话 Cookie → 调用认证服务 /api/auth/internal/verify → 写入 CampusUserContext。
 * 未登录返回 401；认证服务不可用时拒绝放行（fail closed），不降级为匿名访问。</p>
 */
public class CampusAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CampusAuthFilter.class);

    private final CampusAuthProperties props;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    record CacheEntry(CampusUser user, Instant expiresAt) {}

    public CampusAuthFilter(CampusAuthProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
            .connectTimeout(props.getTimeout())
            .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : props.getExcludePrefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : props.getProtectedPrefixes()) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = readCookie(request);
        if (token == null || token.isBlank()) {
            reject(response, 401, "unauthenticated", "未登录或登录已过期");
            return;
        }

        CampusUser user;
        try {
            user = verify(token);
        } catch (AuthServiceUnavailable e) {
            log.warn("认证服务不可用：{}", e.getMessage());
            reject(response, 503, "auth_unavailable", "统一认证服务暂时不可用");
            return;
        }

        if (user == null) {
            reject(response, 401, "unauthenticated", "凭证无效或已过期");
            return;
        }

        CampusUserContext.set(user);
        request.setAttribute("campusUser", user);
        try {
            chain.doFilter(request, response);
        } finally {
            CampusUserContext.clear();
        }
    }

    private CampusUser verify(String token) throws AuthServiceUnavailable {
        CacheEntry cached = cache.get(token);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.user();
        }
        cache.values().removeIf(entry -> entry.expiresAt().isBefore(Instant.now()));

        try {
            String body = mapper.writeValueAsString(Map.of("token", token));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getServiceUrl() + "/api/auth/internal/verify"))
                .timeout(props.getTimeout())
                .header("Content-Type", "application/json")
                .header("X-Internal-Secret", props.getInternalSecret())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                CampusUser user = mapper.readValue(response.body(), CampusUser.class);
                cache.put(token, new CacheEntry(user, Instant.now().plus(props.getCacheTtl())));
                return user;
            }
            if (response.statusCode() == 401) {
                cache.remove(token);
                return null;
            }
            throw new AuthServiceUnavailable("认证服务返回 " + response.statusCode());
        } catch (AuthServiceUnavailable e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthServiceUnavailable("调用认证服务被中断");
        } catch (Exception e) {
            throw new AuthServiceUnavailable(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (props.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(Map.of("error", code, "message", message)));
    }

    static class AuthServiceUnavailable extends Exception {
        AuthServiceUnavailable(String message) {
            super(message);
        }
    }

    /** 供测试注入超时等参数。 */
    Duration cacheTtl() {
        return props.getCacheTtl();
    }
}
