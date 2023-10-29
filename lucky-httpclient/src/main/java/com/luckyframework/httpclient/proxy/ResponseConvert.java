package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Response;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 响应体转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 10:18
 */
@FunctionalInterface
public interface ResponseConvert extends SupportSpELImport{

    /**
     * 将相应实体转化为指定类型的实体
     *
     * @param response         响应实体
     * @param methodContext    方法上下文
     * @param resultConvertAnn 转换注解实例
     * @param <T>              返回实体类型
     * @return 返回实体
     * @throws Exception 转换失败会抛出异常
     */
    <T> T convert(Response response, MethodContext methodContext, Annotation resultConvertAnn) throws Exception;
}
