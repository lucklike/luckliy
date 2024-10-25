package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.dynamic.StandardObjectDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标准动态参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 *
 * @see HeaderParam
 * @see QueryParam
 * @see PathParam
 * @see FormParam
 * @see CookieParam
 * @see URLEncoderPath
 * @see URLEncoderQuery
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(
        setter = @ObjectGenerate(ParameterSetter.class),
        resolver = @ObjectGenerate(StandardObjectDynamicParamResolver.class)
)
public @interface StandardObjectParam {


    /**
     * 基本参数解析器{@link DynamicParamResolver}生成器
     */
    ObjectGenerate baseResolver() default @ObjectGenerate(DynamicParamResolver.class);

    /**
     * 参数设置器{@link ParameterSetter}生成器
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "setter")
    ObjectGenerate setter() default @ObjectGenerate(ParameterSetter.class);

}
