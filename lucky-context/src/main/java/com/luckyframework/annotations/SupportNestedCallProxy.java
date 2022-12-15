package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * 指定使用支持嵌套调用的代理模型
 * @author fk-7075
 */
@Target({ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ProxyModel(ProxyMode.SUPPORT_NESTED)
public @interface SupportNestedCallProxy {

}
