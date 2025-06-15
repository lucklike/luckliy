package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mock插件注解
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Inherited
@Plugin(pluginClass = MockProxyPlugin.class, prohibition = MockPluginProhibition.class)
public @interface MockPlugin {

    /**
     * 是否开启Mock功能
     */
    String enable() default "";

    /**
     * 同mockClass
     */
    Class<?> value() default Void.class;

    /**
     * Mock实现类的Class
     */
    Class<?> mockClass() default Void.class;

    /**
     * Mock实现类的Class的生成器
     */
    ObjectGenerate mockGenerate() default @ObjectGenerate;
}
