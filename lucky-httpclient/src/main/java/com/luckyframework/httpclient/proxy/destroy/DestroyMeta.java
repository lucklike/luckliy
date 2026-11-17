package com.luckyframework.httpclient.proxy.destroy;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注册销毁逻辑的元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 00:53
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(DestroyMetas.class)
public @interface DestroyMeta {

    /**
     * 使用启用该销毁逻辑的开关
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.DESTROY)
    String enable() default "";

    /**
     * 用于生成{@link DestroyHandle}的对象生成器
     */
    ObjectGenerate destroyHandle() default @ObjectGenerate(DestroyHandle.class);

    /**
     * 注册{@link DestroyHandle}Class
     */
    Class<? extends DestroyHandle> destroyClass() default DestroyHandle.class;
}
