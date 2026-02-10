package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.logging.Masker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求响应日志输出处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface PrintLog {

    /**
     * 允许打印日志的最大响应体长度
     */
    long allowRespBodyMaxLength() default -1L;

    /**
     * 允许打印日志的最大请求体长度
     */
    long allowReqBodyMaxLength() default -1L;

    /**
     * 允许打印日志的MimeType
     */
    String[] allowMimeTypes() default {"application/json", "application/*+json", "application/xml", "application/*+xml", "text/xml", "application/x-protobuf", "application/x-java-serialized-object", "text/plain", "text/html"};

    /**
     * 打印响应日志的前提条件
     */
    String respCondition() default "";

    /**
     * 打印请求日志的前提条件
     */
    String reqCondition() default "";

    /**
     * 是否打印响应头信息
     */
    String printRespHeader() default "";

    /**
     * 用于日志打印的请求体SpEL表达式
     */
    String reqBodyExp() default "";

    /**
     * 用于日志打印的响应体SpEL表达式
     */
    String respBodyExp() default "";

    /**
     * 是否启用请求参数脱敏
     */
    String maskRequest() default "";


    /**
     * 是否启用响应参数脱敏
     */
    String maskResponse() default "";

    /**
     * 数脱敏配置
     */
    Masker[] maskers() default {};

}
