package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模拟元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MockMeta {

    /**
     * 用于生成{@link MockResponseFactory}对象的生成器
     */
    ObjectGenerate mockResp() default @ObjectGenerate(MockResponseFactory.class);

    /**
     * 是否启用模拟
     */
    boolean enable() default true;

}
