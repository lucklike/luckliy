package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;

/**
 * 请求处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:41
 */
@FunctionalInterface
public interface RequestAfterProcessor {

    /**
     * 请求处理，在执行请求之前对请求进行最后的设置
     *
     * @param request 请求实例
     */
    void process(Request request);
}
