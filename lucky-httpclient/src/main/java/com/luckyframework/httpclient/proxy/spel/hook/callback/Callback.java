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
 * 定义一个回调函数
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Hook(hookHandleClass = CallbackHookHandler.class)
public @interface Callback {

    /**
     * 回调执行时机
     */
    @AliasFor(annotation = Hook.class, attribute = "lifecycle")
    Lifecycle lifecycle();

    /**
     * 是否存储回调函数的结果
     */
    boolean store() default false;

    /**
     * 存储名称，store为true时此项不配时结果将存储到变量"_MethodName_"中
     */
    String storeName() default "";

    /**
     * 存储类型，存为普通变量还是Root变量
     */
    VarType storeType() default VarType.NORMAL;

    /**
     * 是否将结果展开
     */
    boolean unfold() default false;

    /**
     * 是否为字面量存储
     */
    boolean literal() default false;

}
