package com.luckyframework.httpclient.generalapi.plugin.cache;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.plugin.Plugin;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存插件元注解，标注该注解的类或者方法会启用缓存插件功能
 *
 * <p>示例：
 * <pre>
 *     {@code @CachePluginMeta(key = "#id", expires = "60000")}
 *     {@code @Get("/user/#{#id}")}
 *     {@code User getUser(Long id);}
 * </pre>
 *
 * <p>该注解也可以作为元注解标注在自定义注解上使用，如{@link MemoryCache @MemoryCache}
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Inherited
@Plugin(pluginClass = CacheProxyPlugin.class, prohibition = CachePluginProhibition.class)
@Combination({Plugin.class})
public @interface CachePluginMeta {

    /**
     * 决定是否启用当前插件的SpEL表达式，表达式结果必须是{@code boolean}类型
     *
     * @see SpELVariableNote
     */
    @AliasFor(annotation = Plugin.class, attribute = "enable")
    String enable() default "";

    /**
     * 缓存key（支持SpEL表达式）
     */
    String key() default "";

    /**
     * 缓存过期时间，单位：毫秒（支持SpEL表达式），小于0时表示永不过期
     */
    String expires() default "-1";

    /**
     * 缓存实现类，默认值为{@link ICache}本身，表示未配置
     */
    Class<? extends ICache> cache() default ICache.class;

    /**
     * 缓存对象生成器，优先级高于{@link #cache()}
     */
    ObjectGenerate cacheGenerate() default @ObjectGenerate;

}
