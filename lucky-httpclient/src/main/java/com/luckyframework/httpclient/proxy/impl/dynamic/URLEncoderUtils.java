package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.core.HttpExecutorException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * URL编码工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/5 03:38
 */
public class URLEncoderUtils {

    public static final String CHARSET = "charset";

    public static String encode(Object value, String charset) {
        try {
            return  URLEncoder.encode(String.valueOf(value), charset);
        } catch (UnsupportedEncodingException e) {
            throw new HttpExecutorException("url encoding(" + charset + ") exception: value='" + value + "'", e);
        }
    }
}
