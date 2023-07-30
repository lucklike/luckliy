package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;

/**
 * 请求处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:41
 */
public interface RequestAfterProcessor {

    void process(Request request);
}
