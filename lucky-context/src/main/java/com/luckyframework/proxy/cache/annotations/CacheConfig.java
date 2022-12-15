package com.luckyframework.proxy.cache.annotations;

import java.lang.annotation.*;

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
