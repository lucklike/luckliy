package com.luckyframework.httpclient.proxy.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Masker} 的容器注解，用于支持在同一位置重复标注 {@code @Masker}
 *
 * @see Masker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface Maskers {

    /**
     * 脱敏配置列表
     */
    Masker[] value();
}
