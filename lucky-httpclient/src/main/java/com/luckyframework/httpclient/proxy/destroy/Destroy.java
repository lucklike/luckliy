package com.luckyframework.httpclient.proxy.destroy;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注册销毁逻辑的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 01:27
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Destroys.class)
@Combination(DestroyMeta.class)
@DestroyMeta(destroyHandle = @ObjectGenerate(DefaultDestroyHandle.class))
public @interface Destroy {

    /**
     * 使用SpEL表达式来执行一段销毁逻辑
     */
    @SpelAttribute(stage = Lifecycle.DESTROY)
    String value() default "";

    /**
     * 使用启用该销毁逻辑的开关
     */
    @AliasFor(annotation = DestroyMeta.class, attribute = "enable")
    String enable() default "";

    /**
     * 指定一个用于执行销毁逻辑的SpEL函数
     */
    @SpelAttribute(kind = SpelKind.FUNCTION, stage = Lifecycle.DESTROY)
    String func() default "";

}
