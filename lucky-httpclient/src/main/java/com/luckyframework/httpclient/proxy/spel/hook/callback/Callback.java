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
     * 是否启用该回调
     */
    @AliasFor(annotation = Hook.class, attribute = "enable")
    String enable() default "";

    /**
     * 回调执行时机
     */
    @AliasFor(annotation = Hook.class, attribute = "lifecycle")
    Lifecycle lifecycle();

    /**
     * 是否存储回调函数的结果
     */
    boolean storeOrNot() default false;

    /**
     * 存储名称，storeOrNot为true时此项不进行配置时，结果将存储到变量"$MethodName"中，
     * 例如 String test()方法 --> 方法的运行结果将存储到$test变量中
     */
    String storeName() default "";

    /**
     * 存储类型，存为普通变量还是Root变量，默认存储为ROOT变量
     */
    VarType storeType() default VarType.ROOT;

    /**
     * 是否将结果展开
     */
    boolean unfold() default false;

    /**
     * 是否为字面量存储
     */
    boolean literal() default false;

}
