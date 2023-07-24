package com.luckyframework.httpclient.proxy.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 04:16
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpParam(location = ParamLocation.QUERY)
public @interface QueryParam {

    @AliasFor(annotation = HttpParam.class, attribute = "name")
    String value() default "";


}
