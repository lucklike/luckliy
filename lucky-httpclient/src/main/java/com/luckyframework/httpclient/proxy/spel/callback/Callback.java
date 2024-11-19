package com.luckyframework.httpclient.proxy.spel.callback;


import com.luckyframework.httpclient.proxy.spel.function.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个回调函数
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@FunctionFilter
public @interface Callback {

    /**
     * 回调执行作用域
     */
    VarScope value();
}
