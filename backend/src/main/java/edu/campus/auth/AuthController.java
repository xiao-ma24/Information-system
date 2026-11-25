package edu.campus.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 账号存放在 account 库的单张 account 表：登录名、密码哈希、身份、业务编号。
 * 一个身份对应一个账号，登录时不需要选择身份。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SessionStore sessions;
    private final AuthProperties props;

    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder, SessionStore sessions, AuthProperties props) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.sessions = sessions;
        this.props = props;
    }

    record LoginRequest(String login_name, String password) {}

    record AccountRow(String loginName, String displayName, String passwordHash, String role, String businessId) {}

    record UserView(String user_id, String name, String role, String business_id, String expires_at) {}

    @PostMapping("/login")
    public UserView login(@RequestBody(required = false) LoginRequest body, HttpServletResponse response) {
        if (body == null || isBlank(body.login_name()) || isBlank(body.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写账号和密码");
        }
        if (body.password().length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度超出限制");
        }

        AccountRow account = jdbc.query("""
            SELECT login_name, display_name, password_hash, role_code, business_id
            FROM account
            WHERE login_name = ? AND enabled = 1
            """, rs -> rs.next() ? new AccountRow(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)) : null,
            body.login_name().trim());

        if (account == null || !encoder.matches(body.password(), account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码不正确");
        }

        String token = sessions.create(account.loginName(), account.displayName(), account.role(),
            account.businessId());
        response.addHeader("Set-Cookie", cookie(token, props.getSessionTtl()).toString());
        return view(account, props.getSessionTtl());
    }

    @GetMapping("/me")
    public UserView me(HttpServletRequest request) {
        Optional<SessionStore.Session> session = sessions.find(readToken(request));
        if (session.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        var s = session.get();
        return new UserView(s.userId(), s.name(), s.role(), s.businessId(), s.expiresAt().toString());
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.delete(readToken(request));
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
        return Map.of("success", true);
    }

    private UserView view(AccountRow account, Duration remaining) {
        long seconds = Math.max(0, remaining.toSeconds());
        return new UserView(account.loginName(), account.displayName(), account.role(),
            account.businessId(), Instant.now().plusSeconds(seconds).toString());
    }

    private String readToken(HttpServletRequest request) {
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

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(props.getCookieName(), value)
            .httpOnly(true)
            .secure(props.isSecureCookie())
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
