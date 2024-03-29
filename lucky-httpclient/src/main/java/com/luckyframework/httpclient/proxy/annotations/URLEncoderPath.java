package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.PathParameterSetter;
import com.luckyframework.httpclient.proxy.impl.URLEncoderParameterProcessor;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持URL编码的Path参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(paramSetter = PathParameterSetter.class, paramProcessor = URLEncoderParameterProcessor.class)
public @interface URLEncoderPath {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";

    String charset() default "UTF-8";

}
