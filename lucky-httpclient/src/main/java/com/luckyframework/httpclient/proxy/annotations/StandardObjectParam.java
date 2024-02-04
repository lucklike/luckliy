package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.dynamic.StandardObjectDynamicParamResolver;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求体参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(paramSetter = ParameterSetter.class, paramResolver = StandardObjectDynamicParamResolver.class)
public @interface StandardObjectParam {

    String ATTRIBUTE_BASE_RESOLVER = "baseResolver";
    String ATTRIBUTE_BASE_RESOLVER_MSG = "baseResolverMsg";

    /**
     * 基本参数解析器
     */
    Class<? extends DynamicParamResolver>  baseResolver() default DynamicParamResolver.class;

    /**
     * 基本参数解析器的额外创建信息
     */
    String baseResolverMsg() default "";

    /**
     * 指定参数设置器，用于将参数设置到Http请求实例中
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "paramSetter")
    Class<? extends ParameterSetter> paramSetter();

    /**
     * 参数设置器的额外创建信息
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "paramSetterMsg")
    String paramSetterMsg() default "";

}
