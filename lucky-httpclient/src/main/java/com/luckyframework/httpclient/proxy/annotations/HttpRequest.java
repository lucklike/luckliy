package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.url.SpELURLGetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个http请求方法的注解
 *
 * @see Connect
 * @see Trace
 * @see Get
 * @see Post
 * @see Put
 * @see Delete
 * @see Options
 * @see Head
 * @see Patch
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HttpRequest {

    String ATTRIBUTE_URL = "url";

    /**
     * 定义http请求的Url信息，同url()
     */
    @AliasFor("url")
    String value() default "";

    /**
     * 定义http请求的Url信息，支持SpEL表达式，SpEL表达式部分需要写在#{}中
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
     * }
     * </pre>
     */
    @AliasFor("value")
    String url() default "";

    /**
     * 定义http请求的Method
     */
    RequestMethod method() default RequestMethod.NON;

    /**
     * URL路径获取器生成器
     */
    ObjectGenerate urlGetter() default @ObjectGenerate(clazz = SpELURLGetter.class);

}
