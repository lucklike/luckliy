package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
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
 *
 * @see StandardObjectParam
 * @see HeaderParam
 * @see QueryParam
 * @see PathParam
 * @see FormParam
 * @see CookieParam
 * @see MultiData
 * @see MultiFile
 * @see URLEncoderPath
 * @see URLEncoderQuery
 *
 * @see BodyParam
 * @see JsonBody
 * @see XmlBody
 * @see BinaryBody
 * @see JavaBody
 *
 * @see Url
 * @see MethodParam
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
     * 用于生成{@link ParameterSetter}参数设置器的对象生成器
     */
    ObjectGenerate setter() default @ObjectGenerate(ParameterSetter.class);

    /**
     * 用于生成{@link DynamicParamResolver}动态参数解析器的对象生成器
     */
    ObjectGenerate resolver() default @ObjectGenerate(DynamicParamResolver.class);

}
