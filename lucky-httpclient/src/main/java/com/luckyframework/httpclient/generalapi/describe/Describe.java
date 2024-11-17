package com.luckyframework.httpclient.generalapi.describe;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于定义接口的描述信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:14
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Describe {

    /**
     * 接口唯一ID
     */
    String id() default "";

    /**
     * 接口名称
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 接口名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 接口版本号
     */
    String version() default "";

    /**
     * 接口作者
     */
    String author() default "";

    /**
     * 修改时间
     */
    String updateTime() default "";

    /**
     * 维护人员联系方式
     */
    String contactWay() default "";

}
