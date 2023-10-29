package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.core.JsonBodySerialization;
import com.luckyframework.httpclient.proxy.impl.dynamic.BodyDynamicParamResolver;
import com.luckyframework.httpclient.proxy.impl.setter.BodyParameterSetter;

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
@DynamicParam(paramSetter = BodyParameterSetter.class, paramResolver = BodyDynamicParamResolver.class)
public @interface BodyParam {

    /**
     * mimeType
     */
    String mimeType() default "application/json";

    /**
     * charset
     */
    String charset() default "UTF-8";

    /**
     * 序列化方案
     */
    Class<? extends BodySerialization>  serializationClass() default JsonBodySerialization.class;

}
