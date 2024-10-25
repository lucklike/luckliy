package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 资源加载注解
 * @author fk
 * @version 1.0
 * @date 2021/3/25 0025 16:17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

    /** 忽略资源没有找到?*/
    boolean ignoreResourceNotFound() default false;

    /**
     * 资源描述
     * http开头为网络资源，其他为本地资源
     */
    String[] value();

    /** 加载时使用的编码格式*/
    String encoding() default "UTF-8";

}
