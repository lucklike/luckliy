package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.logging.Masker;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

import java.lang.annotation.*;

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
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.RESPONSE)
    String respCondition() default "";

    /**
     * 打印请求日志的前提条件
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.REQUEST)
    String reqCondition() default "";

    /**
     * 是否打印响应头信息
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String printRespHeader() default "";

    /**
     * 用于日志打印的请求体SpEL表达式
     */
    @SpelAttribute(result = String.class)
    String reqBodyExp() default "";

    /**
     * 用于日志打印的响应体SpEL表达式
     */
    @SpelAttribute(result = String.class)
    String respBodyExp() default "";

    /**
     * 是否启用请求参数脱敏
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String maskRequest() default "";


    /**
     * 是否启用响应参数脱敏
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class)
    String maskResponse() default "";

    /**
     * 数脱敏配置
     */
    Masker[] maskers() default {};

}
