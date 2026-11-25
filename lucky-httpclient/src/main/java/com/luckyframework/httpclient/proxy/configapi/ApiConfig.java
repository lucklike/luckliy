package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 定义API名称的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:15
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@LuckyComponent(type = LuckyComponentType.API, group = "httpclient", priority = 1)
public @interface ApiConfig {

    /**
     * 用于标注一个API的配置ID
     */
    String value() default "";

}
