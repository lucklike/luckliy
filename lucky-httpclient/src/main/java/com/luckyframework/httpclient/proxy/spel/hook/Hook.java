package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Pack;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Var;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.spel.hook.Lifecycle.NON;

/**
 * 生命周期钩子
 *
 * @see Callback
 * @see Var
 * @see Pack
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@FunctionFilter
public @interface Hook {

    /**
     * 是否启用该Hook
     */
    String enable() default "";

    /**
     * 发生异常时是否中断后续流程
     */
    boolean errorInterrupt() default true;

    /**
     * 作用的生命周期
     */
    Lifecycle lifecycle() default NON;

    /**
     * 钩子处理器生成器
     */
    ObjectGenerate hookHandle() default @ObjectGenerate(HookHandler.class);

    /**
     * 钩子处理器Class
     */
    Class<? extends HookHandler> hookHandleClass() default HookHandler.class;

}
