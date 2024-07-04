package com.luckyframework.httpclient.proxy.configapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * 编码工具类
 */
public class EncoderUtils {

    public static String base64(String str) {
        byte[] encode = Base64.getEncoder().encode(str.getBytes());
        return new String(encode);
    }

    public static String basicAuth(String username, String password) {
        String auth = "Basic " + username + ":" + password;
        return base64(auth);
    }

    public static String urlCharset(String str, String charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, charset);
    }

    public static String url(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }
}
