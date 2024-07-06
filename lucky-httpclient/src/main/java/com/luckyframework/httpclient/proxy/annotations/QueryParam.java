package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.LookUpSpecialAnnotationDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.QueryParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Query参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StandardObjectParam(setter = @ObjectGenerate(QueryParameterSetter.class))
public @interface QueryParam {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";

    /**
     * 基基本参数解析器生成器
     */
    ObjectGenerate baseResolver() default @ObjectGenerate(LookUpSpecialAnnotationDynamicParamResolver.class);

}
