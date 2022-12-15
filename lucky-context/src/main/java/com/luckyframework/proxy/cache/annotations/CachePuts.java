package com.luckyframework.proxy.cache.annotations;

import java.lang.annotation.*;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 15:38
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CacheMetadata
public @interface CachePuts {

    CachePut[] value();
}
