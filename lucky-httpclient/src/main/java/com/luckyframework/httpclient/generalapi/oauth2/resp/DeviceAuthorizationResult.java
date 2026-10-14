package com.luckyframework.httpclient.generalapi.oauth2.resp;

/**
 * 设备授权响应（RFC 8628 §3.2）。
 * <pre>
 *     设备端调用 device_authorization 端点后获得的响应，字段名与标准 JSON 响应保持下划线风格一致。
 *     设备端随后需展示 user_code / verification_uri 供用户在另一台设备上完成授权，
 *     并按 interval 指示的间隔轮询令牌端点（grant_type=device_code）直至授权完成或过期。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class DeviceAuthorizationResult {

    /** 设备码，设备端后续用它轮询令牌端点（RFC 8628 §3.2） */
    private String device_code;

    /** 用户码，需展示给用户并在授权页面输入（RFC 8628 § 3.2） */
    private String user_code;

    /** 用户完成授权的验证地址（RFC 8628 §3.2） */
    private String verification_uri;

    /** 含 user_code 的完整验证地址，存在时用户无需手动输入用户码（RFC 8628 §3.2，可选） */
    private String verification_uri_complete;

    /** device_code 有效期（单位：秒） */
    private Long expires_in;

    /** 轮询令牌端点的最小间隔（单位：秒），服务端未返回时默认 5 秒 */
    private Long interval;

    public DeviceAuthorizationResult() {
    }

    public DeviceAuthorizationResult(String device_code, String user_code, String verification_uri,
                                     String verification_uri_complete, Long expires_in, Long interval) {
        this.device_code = device_code;
        this.user_code = user_code;
        this.verification_uri = verification_uri;
        this.verification_uri_complete = verification_uri_complete;
        this.expires_in = expires_in;
        this.interval = interval;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDevice_code() {
        return device_code;
    }

    public void setDevice_code(String device_code) {
        this.device_code = device_code;
    }

    public String getUser_code() {
        return user_code;
    }

    public void setUser_code(String user_code) {
        this.user_code = user_code;
    }

    public String getVerification_uri() {
        return verification_uri;
    }

    public void setVerification_uri(String verification_uri) {
        this.verification_uri = verification_uri;
    }

    public String getVerification_uri_complete() {
        return verification_uri_complete;
    }

    public void setVerification_uri_complete(String verification_uri_complete) {
        this.verification_uri_complete = verification_uri_complete;
    }

    public Long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Long expires_in) {
        this.expires_in = expires_in;
    }

    /**
     * 获取轮询间隔（秒）。服务端未返回 interval 时返回规范默认值 5。
     *
     * @return 轮询间隔秒数
     */
    public Long getInterval() {
        return interval == null ? 5L : interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    @Override
    public String toString() {
        return "DeviceAuthorizationResult{device_code=" + device_code
                + ", user_code=" + user_code
                + ", verification_uri=" + verification_uri
                + ", verification_uri_complete=" + verification_uri_complete
                + ", expires_in=" + expires_in
                + ", interval=" + interval + '}';
    }

    public static class Builder {
        private String device_code;
        private String user_code;
        private String verification_uri;
        private String verification_uri_complete;
        private Long expires_in;
        private Long interval;

        Builder() {
        }

        public Builder device_code(String device_code) {
            this.device_code = device_code;
            return this;
        }

        public Builder user_code(String user_code) {
            this.user_code = user_code;
            return this;
        }

        public Builder verification_uri(String verification_uri) {
            this.verification_uri = verification_uri;
            return this;
        }

        public Builder verification_uri_complete(String verification_uri_complete) {
            this.verification_uri_complete = verification_uri_complete;
            return this;
        }

        public Builder expires_in(Long expires_in) {
            this.expires_in = expires_in;
            return this;
        }

        public Builder interval(Long interval) {
            this.interval = interval;
            return this;
        }

        public DeviceAuthorizationResult build() {
            return new DeviceAuthorizationResult(this.device_code, this.user_code, this.verification_uri,
                    this.verification_uri_complete, this.expires_in, this.interval);
        }
    }
}
