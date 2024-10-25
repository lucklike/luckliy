package com.luckyframework.proxy.cache.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存添加
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 11:31
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CacheMetadata
@Repeatable(CachePuts.class)
public @interface CachePut {

    @AliasFor("cacheNames")
    String[] value() default {};

    @AliasFor("value")
    String[] cacheNames() default {};

    String key() default "";

    String keyGenerator() default "";

    String cacheType() default "";

    String cacheManager() default "";

    String condition() default "";

    String unless() default "";

    String[] spelImport() default {};
}
