package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.ReturnOriginalDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.NonParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 混合参数注解
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StandardObjectParam(setter = @ObjectGenerate(NonParameterSetter.class))
public @interface MixParam {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";

    /**
     * 基本参数解析器生成器
     */
    ObjectGenerate baseResolver() default @ObjectGenerate(ReturnOriginalDynamicParamResolver.class);

}
