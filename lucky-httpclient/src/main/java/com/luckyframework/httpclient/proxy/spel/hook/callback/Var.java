package com.luckyframework.httpclient.proxy.spel.hook.callback;


import com.luckyframework.httpclient.proxy.spel.hook.Hook;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个回调变量
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Hook(hookHandleClass = VarHookHandler.class)
public @interface Var {

    /**
     * 是否启用该变量
     */
    String enable() default "";

    /**
     * 钩子函数是否设置为异步执行，默认false
     */
    @AliasFor(annotation = Hook.class, attribute = "async")
    boolean async() default false;

    /**
     * 存储的变量名
     */
    String name() default "";

    /**
     * 回调执行时机
     */
    @AliasFor(annotation = Hook.class, attribute = "lifecycle")
    Lifecycle lifecycle();

    /**
     * 存储类型，存为普通变量还是Root变量
     */
    VarType type() default VarType.ROOT;

    /**
     * 是否将结果展开
     */
    boolean unfold() default false;

    /**
     * 是否为字面量存储
     */
    boolean literal() default false;


}
