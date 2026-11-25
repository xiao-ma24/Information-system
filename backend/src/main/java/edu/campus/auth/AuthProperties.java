package edu.campus.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.auth")
public class AuthProperties {
    /** 会话 Cookie 名称，各组后端按同一名称读取。 */
    private String cookieName = "CAMPUS_SESSION";
    /** 登录有效期。 */
    private Duration sessionTtl = Duration.ofHours(8);
    /** 正式部署启用 HTTPS 后设为 true。 */
    private boolean secureCookie = false;
    /** 服务之间调用核验接口的共享密钥，必须配置且不可外泄。 */
    private String internalSecret = "";
    /** 演示账号初始化开关，仅本地联调使用。 */
    private boolean demoData = false;

    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public Duration getSessionTtl() { return sessionTtl; }
    public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }
    public boolean isSecureCookie() { return secureCookie; }
    public void setSecureCookie(boolean secureCookie) { this.secureCookie = secureCookie; }
    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String internalSecret) { this.internalSecret = internalSecret; }
    public boolean isDemoData() { return demoData; }
    public void setDemoData(boolean demoData) { this.demoData = demoData; }
}
