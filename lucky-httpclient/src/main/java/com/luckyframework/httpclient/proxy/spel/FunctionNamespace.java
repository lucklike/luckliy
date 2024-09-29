package com.luckyframework.httpclient.proxy.spel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为某个SpEl函数工具类指定一个命名空间
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/1514 15:23
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FunctionNamespace {

    /**
     * 命名空间名称
     */
    String value() default "";

}
