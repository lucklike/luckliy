package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件选择器中的分支定义
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:22
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Branch {

    /**
     * <pre>
     * 断言SpEL表达式, <b>SpEL表达式部分需要写在#{}中</b>
     * 返回值必须是{@link Boolean}类型
     * </pre>
     * <pre>
     * SpEL表达式内置参数有：
     *
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
     * }
     *
     * </pre>
     */
    String assertion();

    /**
     * 结果表达式，这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     * <pre>
     * SpEL表达式内置参数有：
     *
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
     * }
     *
     * </pre>
     */
    String result() default "";;

    /**
     * 异常表达式，这里允许使用SpEL表达式来抛出一个异常，<b>SpEL表达式部分需要写在#{}中</b>
     * <pre>
     * SpEL表达式内置参数有：
     *
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
     * }
     *
     * </pre>
     */
    String exception() default "";

    /**
     * 返回值类型
     */
    Class<?> returnType() default Object.class;
}
