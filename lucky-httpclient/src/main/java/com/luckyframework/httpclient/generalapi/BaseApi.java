package com.luckyframework.httpclient.generalapi;


import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

/**
 * 通用API接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 14:00
 */
public interface BaseApi {

    /**
     * 执行请求得到响应
     *
     * @param request 请求对象
     * @return 响应对象
     */
    @HttpRequest
    Response execute(Request request);


}
