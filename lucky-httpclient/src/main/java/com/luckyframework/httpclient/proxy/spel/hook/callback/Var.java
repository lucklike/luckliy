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
@ValueStore(storeOrNot = true)
@Hook(hookHandleClass = VarHookHandler.class)
public @interface Var {

    /**
     * 是否启用该变量
     */
    @AliasFor(annotation = Hook.class, attribute = "enable")
    String enable() default "";

    /**
     * 发生异常时是否中断后续流程
     */
    @AliasFor(annotation = Hook.class, attribute = "errorInterrupt")
    boolean errorInterrupt() default true;

    /**
     * 回调执行时机
     */
    @AliasFor(annotation = Hook.class, attribute = "lifecycle")
    Lifecycle lifecycle();

    /**
     * 存储的变量名
     */
    @AliasFor(annotation = ValueStore.class, attribute = "name")
    String name() default "";

    /**
     * 存储类型，存为普通变量还是Root变量
     */
    @AliasFor(annotation = ValueStore.class, attribute = "type")
    VarType type() default VarType.ROOT;

    /**
     * 是否将结果展开
     */
    @AliasFor(annotation = ValueStore.class, attribute = "unfold")
    boolean unfold() default false;

    /**
     * 是否为字面量存储
     */
    @AliasFor(annotation = ValueStore.class, attribute = "literal")
    boolean literal() default false;

    /**
     * 配置是否来自于文件，如果此项为true时，属性值会被当作资源描述符来解析
     * 目前支持的资源类型有：
     * yml、json、properties，另外配置的最外层必须为Map结构
     */
    boolean fromFile() default false;
}
