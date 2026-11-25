package edu.campus.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 仅本地联调使用：campus.auth.demo-data=true 时写入演示账号，统一密码 campus123。
 * 正式环境必须关闭。
 */
@Configuration
@ConditionalOnProperty(name = "campus.auth.demo-data", havingValue = "true")
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    record Demo(String loginName, String displayName, String role, String businessId) {}

    @Bean
    ApplicationRunner seedAccounts(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return (ApplicationArguments args) -> {
            String hash = encoder.encode("campus123");
            Demo[] demos = {
                new Demo("stu01", "演示学生", "STUDENT", "S20260001"),
                new Demo("teacher01", "演示教师", "TEACHER", "T20260001"),
                new Demo("staff01", "演示职工", "STAFF", "E20260001")
            };
            for (Demo d : demos) {
                Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM account WHERE login_name = ?", Integer.class, d.loginName());
                if (exists != null && exists == 0) {
                    jdbc.update("""
                        INSERT INTO account (login_name, password_hash, role_code, business_id, display_name, enabled)
                        VALUES (?, ?, ?, ?, ?, 1)
                        """, d.loginName(), hash, d.role(), d.businessId(), d.displayName());
                    log.info("已创建演示账号 {}", d.loginName());
                }
            }
        };
    }
}
