package com.luckyframework.annotations;

import com.luckyframework.proxy.scope.BeanScope;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(BeanScope.THREAD_LOCAL)
public @interface SimpleThreadScope {

    /**
     * Alias for {@link Scope#proxyMode}.
     * <p>Defaults to {@link ProxyMode#AUTO}.
     */
    @AliasFor(annotation = Scope.class)
    ProxyMode proxyMode() default ProxyMode.AUTO;

}