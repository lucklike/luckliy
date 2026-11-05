package com.luckyframework.httpclient.generalapi.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2 授权请求 URL 构建器（RFC 6749 §4.1.1）。
 * <pre>
 *     独立的链式构建工具，不依赖 lucky-httpclient 代理机制，纯 URL 字符串拼接。
 *     生成浏览器重定向用的完整 /authorize URL（含 query 参数）。
 *
 *     用法示例：
 *     {@code
 *     String verifier = OAuth2PkceUtils.generateCodeVerifier();
 *     String challenge = OAuth2PkceUtils.computeCodeChallenge(verifier, "S256");
 *     String url = AuthorizationUrlBuilder.of("https://auth-server.com/oauth2/authorize")
 *         .clientId("my-client")
 *         .redirectUri("https://myapp.com/callback")
 *         .scope("read write")
 *         .state(OAuth2PkceUtils.generateState())
 *         .codeChallenge(challenge)
 *         .codeChallengeMethod("S256")
 *         .build();
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class AuthorizationUrlBuilder {

    private final String authorizeEndpoint;
    private String responseType = "code";
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String codeChallenge;
    private String codeChallengeMethod;

    private AuthorizationUrlBuilder(String authorizeEndpoint) {
        this.authorizeEndpoint = authorizeEndpoint;
    }

    /**
     * 创建构建器实例。
     *
     * @param authorizeEndpoint 授权服务器端点地址
     * @return builder 实例
     */
    public static AuthorizationUrlBuilder of(String authorizeEndpoint) {
        if (authorizeEndpoint == null || authorizeEndpoint.isEmpty()) {
            throw new OAuth2ConfigException("authorizeEndpoint must not be empty");
        }
        return new AuthorizationUrlBuilder(authorizeEndpoint);
    }

    public AuthorizationUrlBuilder responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public AuthorizationUrlBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuthorizationUrlBuilder redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public AuthorizationUrlBuilder scope(String scope) {
        this.scope = scope;
        return this;
    }

    public AuthorizationUrlBuilder state(String state) {
        this.state = state;
        return this;
    }

    public AuthorizationUrlBuilder codeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
        return this;
    }

    public AuthorizationUrlBuilder codeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
        return this;
    }

    /**
     * 构建完整的授权请求 URL（RFC 6749 §4.1.1 + RFC 7636 §5.1）。
     *
     * @return 编码后的完整 URL 字符串
     */
    public String build() {
        if (clientId == null || clientId.isEmpty()) {
            throw new OAuth2ConfigException("clientId is required for authorization request");
        }

        Map<String, String> params = new LinkedHashMap<>();
        putIfPresent(params, "response_type", responseType);
        putIfPresent(params, "client_id", clientId);
        putIfPresent(params, "redirect_uri", redirectUri);
        putIfPresent(params, "scope", scope);
        putIfPresent(params, "state", state);
        // PKCE parameters (RFC 7636 §5.1)
        if (codeChallenge != null && !codeChallenge.isEmpty()) {
            putIfPresent(params, "code_challenge", codeChallenge);
            putIfPresent(params, "code_challenge_method",
                    codeChallengeMethod != null ? codeChallengeMethod : "S256");
        }

        StringBuilder sb = new StringBuilder(authorizeEndpoint);
        sb.append(sb.indexOf("?") >= 0 ? '&' : '?');
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            throw new OAuth2ConfigException("UTF-8 encoding not available", e);
        }
    }
}
