package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Response;

/**
 * 相应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:41
 */
@FunctionalInterface
public interface ResponseAfterProcessor {

    /**
     * 获取到响应结果之后执行
     *
     * @param response 响应
     */
    void responseProcess(Response response);
}
