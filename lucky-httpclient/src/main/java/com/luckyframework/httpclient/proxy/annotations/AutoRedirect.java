package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.RedirectInterceptor;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动执行重定向的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:34
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(InterceptorRegister.class)
@InterceptorRegister(
        intercept = @ObjectGenerate(clazz = RedirectInterceptor.class, scope = Scope.METHOD) ,
        prohibition = RedirectProhibition.class,
        priority = Integer.MIN_VALUE
)
public @interface AutoRedirect {

    /**
     * 需要重定向的状态码
     */
    int[] status() default {};

    /**
     * 重定向地址表达式
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_EL_ENV}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#AN}
     *      {@value TAG#PN}
     *      {@value TAG#ARGS_N}
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
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     *
     *      <b>Void Response : </b>
     *      {@value TAG#VOID_RESPONSE}
     *      {@value TAG#VOID_RESPONSE_REQUEST}
     *      {@value TAG#VOID_RESPONSE_CONTENT_TYPE}
     *      {@value TAG#VOID_RESPONSE_CONTENT_LENGTH}
     *      {@value TAG#VOID_RESPONSE_STATUS}
     *      {@value TAG#VOID_RESPONSE_HEADER}
     *      {@value TAG#VOID_RESPONSE_COOKIE}
     * }
     * </pre>
     */
    String location() default "";

    /**
     * 重定向条件表达式
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_EL_ENV}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#AN}
     *      {@value TAG#PN}
     *      {@value TAG#ARGS_N}
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
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     *
     *      <b>Void Response : </b>
     *      {@value TAG#VOID_RESPONSE}
     *      {@value TAG#VOID_RESPONSE_REQUEST}
     *      {@value TAG#VOID_RESPONSE_CONTENT_TYPE}
     *      {@value TAG#VOID_RESPONSE_CONTENT_LENGTH}
     *      {@value TAG#VOID_RESPONSE_STATUS}
     *      {@value TAG#VOID_RESPONSE_HEADER}
     *      {@value TAG#VOID_RESPONSE_COOKIE}
     * }
     * </pre>
     */
    String condition() default "";
}
