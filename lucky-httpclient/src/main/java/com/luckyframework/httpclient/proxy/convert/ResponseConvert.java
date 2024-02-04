package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.core.Response;

/**
 * 响应体转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 10:18
 */
@FunctionalInterface
public interface ResponseConvert {

    /**
     * 将相应实体转化为指定类型的实体
     *
     * @param response 响应实体
     * @param context  转化器注解上下文
     * @param <T>      返回实体类型
     * @return 返回实体
     * @throws Exception 转换失败会抛出异常
     */
    <T> T convert(Response response, ConvertContext context) throws Exception;
}
