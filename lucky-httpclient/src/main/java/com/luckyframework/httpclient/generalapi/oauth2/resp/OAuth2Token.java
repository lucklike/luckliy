package com.luckyframework.httpclient.generalapi.oauth2.resp;

/**
 * OAuth2 令牌响应（RFC 6749 §5.1）。
 * 字段名与标准 JSON 响应保持下划线风格一致。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class OAuth2Token {

    /** 访问令牌（后续请求的资源凭证） */
    private String access_token;

    /** 令牌类型，通常为 Bearer（大小写不敏感） */
    private String token_type;

    /** 访问令牌有效期（单位：秒），调用方自行结合获取时间判断是否过期 */
    private Long expires_in;

    /** 刷新令牌（仅部分授权模式返回，可用于换取新的访问令牌） */
    private String refresh_token;

    /** 授权范围（服务端返回时才存在） */
    private String scope;

    public OAuth2Token() {
    }

    public OAuth2Token(String access_token, String token_type, Long expires_in, String refresh_token, String scope) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.scope = scope;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public Long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Long expires_in) {
        this.expires_in = expires_in;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * 判断令牌是否已过期。
     *
     * @param receivedAtMs 令牌获取时的时间戳（毫秒，由调用方记录）
     * @return true 表示已过期
     */
    public boolean isExpired(long receivedAtMs) {
        return isExpired(receivedAtMs, 0);
    }

    /**
     * 判断令牌是否已过期（含时钟偏差容忍）。
     *
     * @param receivedAtMs 令牌获取时的时间戳（毫秒）
     * @param skewSeconds  容错秒数（防网络延迟/时钟偏差，建议 30-60）
     * @return true 表示已过期或即将过期
     */
    public boolean isExpired(long receivedAtMs, long skewSeconds) {
        if (expires_in == null) {
            return false; // 服务端未返回有效期，无法判断
        }
        long expiresAt = getExpiresAtMs(receivedAtMs);
        return System.currentTimeMillis() >= (expiresAt - skewSeconds * 1000);
    }

    /**
     * 计算令牌的绝对过期时间戳。
     *
     * @param receivedAtMs 令牌获取时的时间戳（毫秒）
     * @return 过期时间戳（毫秒），若 expires_in 为 null 则返回 null
     */
    public Long getExpiresAtMs(long receivedAtMs) {
        if (expires_in == null) {
            return null;
        }
        return receivedAtMs + expires_in * 1000;
    }

    @Override
    public String toString() {
        return "OAuth2Token{access_token=" + access_token
                + ", token_type=" + token_type
                + ", expires_in=" + expires_in
                + ", refresh_token=" + refresh_token
                + ", scope=" + scope + '}';
    }

    public static class Builder {
        private String access_token;
        private String token_type;
        private Long expires_in;
        private String refresh_token;
        private String scope;

        Builder() {
        }

        public Builder access_token(String access_token) {
            this.access_token = access_token;
            return this;
        }

        public Builder token_type(String token_type) {
            this.token_type = token_type;
            return this;
        }

        public Builder expires_in(Long expires_in) {
            this.expires_in = expires_in;
            return this;
        }

        public Builder refresh_token(String refresh_token) {
            this.refresh_token = refresh_token;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public OAuth2Token build() {
            return new OAuth2Token(this.access_token, this.token_type, this.expires_in, this.refresh_token, this.scope);
        }
    }
}
