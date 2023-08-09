package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.ResponseConvert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应结果转换器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResultConvert {

    /**
     * 响应结果转换器
     */
    Class<? extends ResponseConvert> value();

    /**
     * 响应结果转换器
     */
    String convertMsg() default "";

}
