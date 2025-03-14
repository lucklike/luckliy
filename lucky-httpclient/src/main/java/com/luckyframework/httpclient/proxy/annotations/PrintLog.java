package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.PrintLogInterceptor;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.interceptor.PriorityConstant.ANNOTATION_LOGGER_PRIORITY;

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
@Combination(InterceptorMeta.class)
@InterceptorMeta(
        intercept = @ObjectGenerate(clazz = PrintLogInterceptor.class, scope = Scope.METHOD_CONTEXT),
        prohibition = PrintLogProhibition.class,
        priority = ANNOTATION_LOGGER_PRIORITY
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

    /**
     * 是否开启强制打印响应体功能
     */
    boolean forcePrintBody() default false;

    /**
     * 是否打印响应头信息
     */
    boolean printRespHeader() default true;

    /**
     * 优先级，数值越高优先级越低
     */
    @AliasFor(annotation = InterceptorMeta.class, attribute = "priority")
    int priority() default PriorityConstant.DEFAULT_PRIORITY;

}
