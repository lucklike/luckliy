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
     * 打印请求日志的前提条件
     */
    @AliasFor(annotation = PrintLog.class, attribute = "reqCondition")
    String reqCondition() default "";
}
