package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.ReturnOriginalDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.JsonPropertyParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSON请求体参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StandardObjectParam(setter = @ObjectGenerate(JsonPropertyParameterSetter.class))
public @interface JsonParam {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name") String value() default "";

    /**
     * 基本参数解析器生成器
     */
    ObjectGenerate baseResolver() default @ObjectGenerate(ReturnOriginalDynamicParamResolver.class);

}
