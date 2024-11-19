package com.luckyframework.httpclient.proxy.spel.callback;


import com.luckyframework.httpclient.proxy.spel.function.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.httpclient.proxy.spel.var.VarType;

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

    /**
     * 是否存储回调函数的结果
     */
    boolean store() default true;

    /**
     * 存储名称，不配时结果将存储到变量"$+MethodName"中
     */
    String name() default "";

    /**
     * 存储类型，存为普通变量还是Root变量
     */
    VarType storeType() default VarType.NORMAL;


}
