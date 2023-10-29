package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.httpclient.proxy.impl.SpELURLGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 域名配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DomainName {

    /**
     * 请求的域名配置，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    String value() default "";

    /**
     * URL路径获取器
     */
    Class<? extends URLGetter> getter() default SpELURLGetter.class;

    /**
     * 用于创建URL路径获取器的额外信息
     */
    String getterMsg() default "";

}
