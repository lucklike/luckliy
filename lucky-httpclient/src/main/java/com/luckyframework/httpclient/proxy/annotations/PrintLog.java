package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.PrintLogInterceptor;
import com.luckyframework.reflect.Combination;

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
@Combination(InterceptorRegister.class)
@InterceptorRegister(
        intercept = @ObjectGenerate(clazz = PrintLogInterceptor.class, scope = Scope.METHOD_CONTEXT),
        prohibition = PrintLogProhibition.class
)
public @interface PrintLog {

    /**
     * 允许打印日志的最大响应体长度
     */
    long allowBodyMaxLength() default -1L;

    /**
     * 允许打印日志的MimeType
     */
    String[] allowMimeTypes() default {"application/json", "application/xml", "text/xml", "text/plain", "text/html"};

    /**
     * 打印响应日志的前提条件
     */
    String respCondition() default "";

    /**
     * 打印请求日志的前提条件
     */
    String reqCondition() default "";

    /**
     * 是否打印注解信息，默认不打印
     */
    boolean printAnnotationInfo() default false;

    /**
     * 是否打印参数信息，默认不打印
     */
    boolean printArgsInfo() default false;
}
