package com.luckyframework.httpclient.generalapi.oauth2;

/**
 * OAuth2 配置与编码异常。
 * <pre>
 *     当注解属性中的 charset/algorithm 等配置不合法，
 *     或 Basic 凭证编码过程中出现 IOException 时抛出，
 *     与网络异常区分——本异常表示"配置/编码问题"，非请求失败。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class OAuth2ConfigException extends RuntimeException {

    public OAuth2ConfigException(String message) {
        super(message);
    }

    public OAuth2ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
