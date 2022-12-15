package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * 禁用代理
 * @author fk-7075
 */
@Target({ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ProxyModel(ProxyMode.NO)
public @interface DisableProxy {

}
