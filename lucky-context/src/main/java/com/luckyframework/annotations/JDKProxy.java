package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * 指定使用JDK代理
 * @author fk-7075
 */
@Target({ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ProxyModel(ProxyMode.INTERFACES)
public @interface JDKProxy {

}
