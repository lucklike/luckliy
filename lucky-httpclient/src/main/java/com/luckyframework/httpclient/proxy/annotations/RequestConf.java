package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.RequestAfterProcessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestConf {

    /**
     * 连接超时时间
     */
    int connectTimeout() default -1;

    /**
     * 读超时时间
     */
    int readTimeout() default -1;

    /**
     * 写超时时间
     */
    int writeTimeout() default -1;

    /**
     * 公共请求头
     */
    KV[] commonHeaders() default {};

    /**
     * 公共Query参数
     */
    KV[] commonQueryParams() default {};

    /**
     * 公共路径参数
     */
    KV[] commonPathParams() default {};

    /**
     * 公共Request参数
     */
    KV[] commonRequestParams() default {};

    /**
     * 请求处理器
     */
    Class<? extends RequestAfterProcessor> afterProcessor() default RequestAfterProcessor.class;

}
