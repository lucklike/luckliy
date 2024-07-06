package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.RequestMethodDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.RequestMethodSetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求方法参数设置注解
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
        setter = @ObjectGenerate(RequestMethodSetter.class),
        resolver = @ObjectGenerate(RequestMethodDynamicParamResolver.class)
)
public @interface MethodParam {


}
