package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 代理模式
 * 1.ScopedProxyMode.AUTO          ->  自动选择
 * 2.ScopedProxyMode.NO            ->  禁止代理
 * 3.ScopedProxyMode.INTERFACES    ->  JDK代理
 * 4.ScopedProxyMode.TARGET_CLASS  ->  Cglib代理
 * @author fk-7075
 */
@Target({ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProxyModel {

    ProxyMode value() default ProxyMode.AUTO;

}
