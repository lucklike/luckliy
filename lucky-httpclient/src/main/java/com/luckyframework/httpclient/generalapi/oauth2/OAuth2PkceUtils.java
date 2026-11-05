package com.luckyframework.httpclient.generalapi.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE（RFC 7636）与 OAuth2 state 参数生成工具类。
 * <pre>
 *     提供 code_verifier 生成、code_challenge 计算、state 参数生成等便利方法，
 *     供 AuthorizationUrlBuilder 与 @OAuth2AuthorizationCodeGrant 的 PKCE 场景使用。
 *
 *     纯静态工具类，无状态、线程安全。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public final class OAuth2PkceUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** code_verifier 字符集（RFC 7636 §4.1: unreserved characters） */
    private static final char[] VERIFIER_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toCharArray();

    /** code_verifier 最小长度 */
    private static final int VERIFIER_MIN_LENGTH = 43;
    /** code_verifier 最大长度 */
    private static final int VERIFIER_MAX_LENGTH = 128;

    private OAuth2PkceUtils() {
    }

    /**
     * 生成符合 RFC 7636 §4.1 要求的 code_verifier（43-128字符，高熵随机串）。
     * 默认生成 64 字符长度。
     *
     * @return code_verifier 字符串
     */
    public static String generateCodeVerifier() {
        return generateCodeVerifier(64);
    }

    /**
     * 生成指定长度的 code_verifier。
     *
     * @param length 长度（43-128），超出范围将被截断到合法区间
     * @return code_verifier 字符串
     */
    public static String generateCodeVerifier(int length) {
        int len = Math.max(VERIFIER_MIN_LENGTH, Math.min(VERIFIER_MAX_LENGTH, length));
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(VERIFIER_CHARS[SECURE_RANDOM.nextInt(VERIFIER_CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 根据 code_verifier 计算 code_challenge（RFC 7636 §4.2）。
     *
     * @param codeVerifier PKCE code_verifier
     * @param method       变换方式："S256" 或 "plain"
     * @return code_challenge 字符串
     */
    public static String computeCodeChallenge(String codeVerifier, String method) {
        if ("plain".equalsIgnoreCase(method)) {
            return codeVerifier;
        }
        // S256: BASE64URL(SHA256(ASCII(code_verifier)))
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return BASE64_URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new OAuth2ConfigException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 生成用于 CSRF 防护的 state 参数（RFC 6749 §10.12）。
     * 使用 32 字节安全随机数经 Base64URL 编码，输出 43 字符。
     *
     * @return state 字符串
     */
    public static String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }
}
