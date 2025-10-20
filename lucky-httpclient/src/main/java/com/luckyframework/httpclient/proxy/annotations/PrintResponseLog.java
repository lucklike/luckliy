package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应日志输出处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PrintLog(reqCondition = "false")
@Combination(PrintLog.class)
public @interface PrintResponseLog {

    /**
     * 允许打印日志的最大响应体长度
     */
    @AliasFor(annotation = PrintLog.class, attribute = "allowRespBodyMaxLength")
    long allowRespBodyMaxLength() default -1L;

    /**
     * 允许打印日志的MimeType
     */
    @AliasFor(annotation = PrintLog.class, attribute = "allowMimeTypes")
    String[] allowMimeTypes() default {"application/json", "application/*+json", "application/xml", "application/*+xml", "text/xml", "application/x-protobuf", "application/x-java-serialized-object", "text/plain", "text/html"};

    /**
     * 打印响应日志的前提条件
     */
    @AliasFor(annotation = PrintLog.class, attribute = "respCondition")
    String respCondition() default "";

    /**
     * 是否打印响应头信息
     */
    @AliasFor(annotation = PrintLog.class, attribute = "printRespHeader")
    boolean printRespHeader() default true;

    /**
     * 触发警告标志的最小耗时
     */
    @AliasFor(annotation = PrintLog.class, attribute = "warnTime")
    long warnTime() default -1L;

    /**
     * 触发错误标志的最小耗时
     */
    @AliasFor(annotation = PrintLog.class, attribute = "slowTime")
    long slowTime() default -1L;
}
