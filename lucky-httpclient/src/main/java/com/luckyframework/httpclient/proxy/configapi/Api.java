package com.luckyframework.httpclient.proxy.configapi;

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
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Api {

    /**
     * 用于标注一个API的名称
     */
    String value();

}
