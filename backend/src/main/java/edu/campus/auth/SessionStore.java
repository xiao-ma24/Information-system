package edu.campus.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 会话存储：令牌只以 SHA-256 摘要形式落库，数据库泄漏也无法直接冒用。
 * 会话不依赖进程内存，因此各组后端可以独立核验。
 */
@Component
public class SessionStore {

    public record Session(String userId, String name, String role, String businessId, Instant expiresAt) {}

    private static final int TOKEN_BYTES = 32;
    private final JdbcTemplate jdbc;
    private final AuthProperties props;
    private final SecureRandom random = new SecureRandom();

    public SessionStore(JdbcTemplate jdbc, AuthProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    /** 创建会话，返回明文令牌（只在此处出现一次，之后仅保存摘要）。 */
    public String create(String userId, String name, String role, String businessId) {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        long seconds = props.getSessionTtl().toSeconds();

        jdbc.update("""
            INSERT INTO auth_session (token_hash, user_id, display_name, role_code, business_id, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, NOW(), TIMESTAMPADD(SECOND, ?, NOW()))
            """, hash(token), userId, name, role, businessId, seconds);
        return token;
    }

    public Optional<Session> find(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            return Optional.empty();
        }
        List<Session> rows = jdbc.query("""
            SELECT user_id, display_name, role_code, business_id, expires_at
            FROM auth_session
            WHERE token_hash = ? AND expires_at > NOW()
            """, (rs, i) -> new Session(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                toInstant(rs.getTimestamp(5))), hash(token));
        return rows.stream().findFirst();
    }

    public void delete(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        jdbc.update("DELETE FROM auth_session WHERE token_hash = ?", hash(token));
    }

    /** 每小时清理一次过期会话。 */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3_600_000)
    public void purgeExpiredScheduled() {
        purgeExpired();
    }

    public int purgeExpired() {
        return jdbc.update("DELETE FROM auth_session WHERE expires_at <= NOW()");
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 供测试与运维排查使用。 */
    @SuppressWarnings("unused")
    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
