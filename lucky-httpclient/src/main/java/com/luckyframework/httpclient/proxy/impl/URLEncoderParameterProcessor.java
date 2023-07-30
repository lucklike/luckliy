package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.proxy.ParameterProcessor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.impl.BodyParameterProcessor.CHARSET;

/**
 * URL编码处理的参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:16
 */
public class URLEncoderParameterProcessor implements ParameterProcessor {

    @Override
    public Object paramProcess(Object originalParam, Map<String, String> extraParmMap) {
        String charset = extraParmMap.getOrDefault(CHARSET, "ISO-8859-1");
        try {
            return URLEncoder.encode(String.valueOf(originalParam), charset);
        } catch (UnsupportedEncodingException e) {
            throw new HttpExecutorException("url encoding(" + charset + ") exception: value='" + originalParam + "'", e);
        }
    }
}
