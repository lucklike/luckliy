package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.ResponseAfterHandle;

import java.lang.annotation.Annotation;

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
     * @param response               响应
     * @param responseAfterHandleAnn 当前{@link ResponseAfterHandle}注解实例
     */
    void responseProcess(Response response, Annotation responseAfterHandleAnn);
}
