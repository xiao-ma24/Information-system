package edu.campus.authclient;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 认证服务核验后返回的当前用户。 */
public record CampusUser(
    @JsonProperty("user_id") String userId,
    @JsonProperty("name") String name,
    @JsonProperty("role") String role,
    @JsonProperty("business_id") String businessId,
    @JsonProperty("expires_at") String expiresAt) {
}
