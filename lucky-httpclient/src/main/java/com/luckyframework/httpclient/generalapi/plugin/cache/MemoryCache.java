package com.luckyframework.httpclient.generalapi.plugin.cache;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内存缓存插件注解，使用{@link MemoryCacheImpl}作为缓存实现
 *
 * <p>示例：
 * <pre>
 *     {@code @MemoryCache(key = "#id", expires = "60000")}  // 缓存60秒
 *     {@code @Get("/user/#{#id}")}
 *     {@code User getUser(Long id);}
 * </pre>
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Inherited
@CachePluginMeta(cache = MemoryCacheImpl.class)
public @interface MemoryCache {

    /**
     * 缓存key（支持SpEL表达式）
     */
    @AliasFor(annotation = CachePluginMeta.class, attribute = "key")
    String key() default "";

    /**
     * 缓存过期时间，单位：毫秒（支持SpEL表达式），小于0时表示永不过期
     */
    @AliasFor(annotation = CachePluginMeta.class, attribute = "expires")
    String expires() default "-1";

}
