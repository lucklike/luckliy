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

    Class<?> value() default Void.class;

    String enable() default "";

    Class<?> mockClass() default Void.class;

    ObjectGenerate mockGenerate() default @ObjectGenerate;
}
