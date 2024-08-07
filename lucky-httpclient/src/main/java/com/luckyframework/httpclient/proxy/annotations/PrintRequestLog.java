package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
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

    /**
     * 是否打印注解信息，默认不打印
     */
    @AliasFor(annotation = PrintLog.class, attribute = "printAnnotationInfo")
    boolean printAnnotationInfo() default false;

    /**
     * 是否打印参数信息，默认不打印
     */
    @AliasFor(annotation = PrintLog.class, attribute = "printArgsInfo")
    boolean printArgsInfo() default false;

    /**
     * 优先级，数值越高优先级越低
     */
    @AliasFor(annotation = PrintLog.class, attribute = "priority")
    int priority() default PriorityConstant.DEFAULT_PRIORITY;
}
