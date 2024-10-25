package com.luckyframework.annotations;

import com.luckyframework.proxy.scope.BeanScope;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/29 16:16
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(BeanScope.REFRESH)
public @interface RefreshScope {

    /**
     * Alias for {@link Scope#proxyMode}.
     * <p>Defaults to {@link ProxyMode#TARGET_CLASS}.
     */
    @AliasFor(annotation = Scope.class)
    ProxyMode proxyMode() default ProxyMode.TARGET_CLASS;
}
