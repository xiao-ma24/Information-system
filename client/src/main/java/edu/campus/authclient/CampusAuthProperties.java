package edu.campus.authclient;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.auth")
public class CampusAuthProperties {
    /** 统一认证服务地址，例如 http://127.0.0.1:18080 或内网地址。 */
    private String serviceUrl = "http://127.0.0.1:18080";
    /** 与认证服务一致的内部密钥，仅服务端之间使用。 */
    private String internalSecret = "";
    /** 浏览器携带的会话 Cookie 名称。 */
    private String cookieName = "CAMPUS_SESSION";
    /** 需要认证的路径前缀。 */
    private List<String> protectedPrefixes = List.of("/api/");
    /** 不做认证的路径前缀。 */
    private List<String> excludePrefixes = List.of("/api/auth/", "/actuator/", "/error");
    /** 核验结果的本地缓存时长，降低认证服务压力。 */
    private Duration cacheTtl = Duration.ofSeconds(5);
    /** 调用认证服务的超时时间。 */
    private Duration timeout = Duration.ofSeconds(3);

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String internalSecret) { this.internalSecret = internalSecret; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public List<String> getProtectedPrefixes() { return protectedPrefixes; }
    public void setProtectedPrefixes(List<String> protectedPrefixes) { this.protectedPrefixes = protectedPrefixes; }
    public List<String> getExcludePrefixes() { return excludePrefixes; }
    public void setExcludePrefixes(List<String> excludePrefixes) { this.excludePrefixes = excludePrefixes; }
    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
