package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.*;

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
    ObjectGenerate mock() default @ObjectGenerate(MockResponseFactory.class);

    /**
     * {@link MockResponseFactory}对象Class, 使用此配置默认创建单例对象
     */
    Class<? extends MockResponseFactory> mockClass() default MockResponseFactory.class;

    /**
     * 启用Mock的条件表达式
     */
    String enable() default "";

    /**
     * 指定一个函数来决定是否启用 Mock
     */
    String enableFunc() default "";

}
