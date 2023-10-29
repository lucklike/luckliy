package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.ParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DynamicParam {

    /**
     * 参数名称
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 参数名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 指定参数设置器，用于将参数设置到Http请求实例中
     */
    Class<? extends ParameterSetter> paramSetter();

    /**
     * 参数设置器的额外创建信息
     */
    String paramSetterMsg() default "";


    /**
     * 指定参数解析器，用于将原始参数转化为目标参数集合
     */
    Class<? extends DynamicParamResolver> paramResolver();

    /**
     * 参数解析器的额外创建信息
     */
    String paramResolverMsg() default "";

}
