package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * 指定使用Cglib代理
 * @author fk-7075
 */
@Target({ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ProxyModel(ProxyMode.TARGET_CLASS)
public @interface CglibProxy {

}
