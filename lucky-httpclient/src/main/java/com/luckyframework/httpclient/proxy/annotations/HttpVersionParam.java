package com.luckyframework.httpclient.proxy.annotations;


import com.luckyframework.httpclient.proxy.dynamic.HttpVersionDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.HttpVersionParameterSetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP版本参数设置注解
 *
 * @author fukang
 * @version 3.0.2
 * @date 2025/9/5 00:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(
        setter = @ObjectGenerate(HttpVersionParameterSetter.class),
        resolver = @ObjectGenerate(HttpVersionDynamicParamResolver.class)
)
public @interface HttpVersionParam {
}
