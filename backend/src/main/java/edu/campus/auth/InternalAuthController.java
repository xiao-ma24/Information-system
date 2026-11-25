package edu.campus.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 仅供各组后端调用，不暴露给浏览器。
 * 调用方必须携带 X-Internal-Secret，且服务之间应放在同一内网或用反向代理限制来源。
 */
@RestController
@RequestMapping("/api/auth/internal")
public class InternalAuthController {

    private static final String SECRET_HEADER = "X-Internal-Secret";

    private final SessionStore sessions;
    private final AuthProperties props;

    public InternalAuthController(SessionStore sessions, AuthProperties props) {
        this.sessions = sessions;
        this.props = props;
    }

    record VerifyRequest(String token) {}

    record VerifyResponse(String user_id, String name, String role, String business_id, String expires_at) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/verify")
    public VerifyResponse verify(@RequestHeader(value = SECRET_HEADER, required = false) String secret,
                                 @RequestBody(required = false) VerifyRequest body) {
        requireSecret(secret);
        if (body == null || body.token() == null || body.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少会话凭证");
        }
        Optional<SessionStore.Session> session = sessions.find(body.token());
        if (session.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "凭证无效或已过期");
        }
        var s = session.get();
        return new VerifyResponse(s.userId(), s.name(), s.role(), s.businessId(), s.expiresAt().toString());
    }

    private void requireSecret(String secret) {
        String expected = props.getInternalSecret();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "服务端未配置内部密钥");
        }
        if (secret == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "内部调用凭据不正确");
        }
    }
}
