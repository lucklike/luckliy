package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.logging.Masker;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求日志输出处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PrintLog(respCondition = "false")
@Combination(PrintLog.class)
public @interface PrintRequestLog {


    /**
     * 用于获取唯一ID的SpEL表达式
     */
    @AliasFor(annotation = PrintLog.class, attribute = "uniqueId")
    String uniqueId() default "";

    /**
     * 允许打印日志的最大请求体长度
     */
    @AliasFor(annotation = PrintLog.class, attribute = "allowReqBodyMaxLength")
    long allowReqBodyMaxLength() default -1L;


    /**
     * 打印请求日志的前提条件
     */
    @AliasFor(annotation = PrintLog.class, attribute = "reqCondition")
    String reqCondition() default "";


    /**
     * 用于日志打印的请求体SpEL表达式
     */
    @AliasFor(annotation = PrintLog.class, attribute = "reqBodyExp")
    String reqBodyExp() default "";

    /**
     * 是否启用请求参数脱敏
     */
    @AliasFor(annotation = PrintLog.class, attribute = "maskRequest")
    String maskRequest() default "";

    /**
     * 数脱敏配置
     */
    @AliasFor(annotation = PrintLog.class, attribute = "maskers")
    Masker[] maskers() default {};

}
