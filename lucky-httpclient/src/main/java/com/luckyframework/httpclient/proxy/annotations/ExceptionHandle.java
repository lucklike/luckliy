package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.handle.SpELHttpExceptionHandle;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 支持使用SpEL表达式进行异常处理的注解
 * <pre>
 *     约定大于配置
 *     当excHandleExp不做任何配置时，Lucky会检测当前代理接口中是否存在方法名+<b>ExceptionHandle</b>的静态方法，如果有则会自动使用该方法来进行异常处理
 *     ExceptionHandle方法的参数列表可以是如下类型：
 *     {@link MethodContext}、{@link ClassContext}、{@link Method Method(当前HTTP方法示例)}
 *     {@link Class Class(当前HTTP接口类型)}、{@link Request} 、<b>当前HTTP接口类型（将注入该代理对象）</b>
 *     {@link Throwable}
 *
 *     {@code
 *     @HttpClientComponent
 *     public interface ExceptionHandleApi{
 *
 *
 *         @ExceptionHandle
 *         @Get("/hello")
 *         String hello();
 *
 *         // hello方法的默认异常处理方法helloExceptionHandle()
 *         static void helloExceptionHandle(Exception e) {
 *               e.printStackTrace();
 *               System.out.println("出异常了老铁！");
 *         }
 *
 *         // helloExceptionHandle方法也可以带上其他参数如：
 *         static void helloExceptionHandle(MethodContext context, MockApi api, Request request, Exception e) {
 *               e.printStackTrace();
 *               System.out.println("出异常了老铁！");
 *         }
 *     }
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExceptionHandleMeta(handle = @ObjectGenerate(SpELHttpExceptionHandle.class))
public @interface ExceptionHandle {

    /**
     * 同{@link #excHandleExp()}
     *
     * 用于处理异常的表达式，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Throwable : </b>
     *      {@value TAG#THROWABLE}
     * }
     * </pre>
     *
     */
    @AliasFor("excHandleExp")
    String value() default "";


    /**
     * 用于处理异常的表达式，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *       <b>Throwable : </b>
     *       {@value TAG#THROWABLE}
     * }
     * </pre>
     */
    @AliasFor("value")
    String excHandleExp() default "";

    /**
     * 条件表达式，当条件表达式成立时使用该处理器
     */
    @AliasFor(annotation = ExceptionHandleMeta.class, attribute = "condition")
    String condition() default "";

    /**
     * 需要该处理器处理的异常
     */
    @AliasFor(annotation = ExceptionHandleMeta.class, attribute = "exceptions")
    Class<? extends Throwable>[] exceptions() default {Exception.class};

}
