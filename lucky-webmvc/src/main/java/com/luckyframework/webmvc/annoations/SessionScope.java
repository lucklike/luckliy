package com.luckyframework.webmvc.annoations;

import com.luckyframework.annotations.Scope;
import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.webmvc.WebBeanScope;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebBeanScope.SESSION)
public @interface SessionScope {

    /**
     * Alias for {@link Scope#proxyMode}.
     * <p>Defaults to {@link ProxyMode#AUTO}.
     */
    @AliasFor(annotation = Scope.class)
    ProxyMode proxyMode() default ProxyMode.AUTO;

}
