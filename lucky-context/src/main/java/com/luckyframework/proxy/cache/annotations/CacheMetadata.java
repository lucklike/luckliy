package com.luckyframework.proxy.cache.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存元注解
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 11:30
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheMetadata {

}
