package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持降级处理的异常处理器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/01 16:38
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExceptionHandleMeta(handle = @ObjectGenerate(ExceptionFallbackHandle.class))
public @interface ExceptionFallback {

    /**
     * 优先级：2 <br/>
     * 设置一个用于降级处理的实现类的Class
     */
    @AliasFor("impl")
    Class<?> value() default Void.class;

    /**
     * 优先级：2 <br/>
     * 设置一个用于降级处理的实现类的Class
     */
    @AliasFor("value")
    Class<?> impl() default Void.class;

    /**
     * 优先级：3 <br/>
     * 使用SpEL表达式来获取一个降级处理实现类的对象
     */
    String implExp() default "";

    /**
     * 优先级：1 <br/>
     * 设置一个用于降级处理的实现类的生成器
     */
    ObjectGenerate implGenerate() default @ObjectGenerate;

    /**
     * 条件表达式，当条件表达式成立时使用该处理器
     */
    @AliasFor(annotation = ExceptionHandleMeta.class, attribute = "condition")
    String condition() default "";

    /**
     * 需要该处理器处理的异常
     */
    @AliasFor(annotation = ExceptionHandleMeta.class, attribute = "exceptions")
    Class<? extends Throwable>[] exceptions() default {HttpExecutorException.class};

}
