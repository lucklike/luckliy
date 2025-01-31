package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 插件注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Plugins.class)
public @interface Plugin {

    /**
     * 插件Class
     */
    Class<? extends ProxyPlugin> pluginClass() default ProxyPlugin.class;

    /**
     * 用于生成插件对象的对象生成器
     */
    ObjectGenerate plugin() default @ObjectGenerate(ProxyPlugin.class);


    /**
     * 禁止使用当前拦截器的标志注解
     */
    Class<? extends Annotation> prohibition() default PluginProhibition.class;
}
