package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.serialization.BodySerialization;
import com.luckyframework.httpclient.proxy.dynamic.BodyDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.BodyParameterSetter;
import com.luckyframework.reflect.Combination;

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
@DynamicParam(
        setter = @ObjectGenerate(BodyParameterSetter.class),
        resolver = @ObjectGenerate(BodyDynamicParamResolver.class)
)
@Combination(DynamicParam.class)
public @interface BodyParam {

    /**
     * mimeType
     */
    String mimeType() default "";

    /**
     * charset
     */
    String charset() default "";

    /**
     * 序列化方案{@link BodySerialization}的创建器
     */
    ObjectGenerate serialization() default @ObjectGenerate(BodySerialization.class);

}
