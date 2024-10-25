package com.luckyframework.proxy.cache.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 11:31
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

    String[] cacheNames() default {};

    String keyGenerator() default "";

    String cacheType() default "";

    String cacheManager() default "";

    String[] spelImport() default {};
}
