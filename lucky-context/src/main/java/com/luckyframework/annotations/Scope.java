package com.luckyframework.annotations;

import com.luckyframework.proxy.scope.BeanScope;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/19 0019 15:37
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    @AliasFor("scopeName")
    String value() default BeanScope.SINGLETON;

    @AliasFor("value")
    String scopeName() default BeanScope.SINGLETON;

    ProxyMode proxyMode() default ProxyMode.NO;

}
