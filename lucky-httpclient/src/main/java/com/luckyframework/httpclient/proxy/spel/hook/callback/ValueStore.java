package com.luckyframework.httpclient.proxy.spel.hook.callback;


import com.luckyframework.httpclient.proxy.spel.hook.Hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 值存储注解
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Hook(hookHandleClass = VarHookHandler.class)
public @interface ValueStore {

    /**
     * 是否存储结果
     */
    boolean storeOrNot() default false;

    /**
     * 存储的变量名
     */
    String name() default "";


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
