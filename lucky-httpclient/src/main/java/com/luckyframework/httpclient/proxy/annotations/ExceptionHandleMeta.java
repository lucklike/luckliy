package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.handle.ExceptionFallback;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

import java.lang.annotation.*;

/**
 * 异常处理注解
 * @see ExceptionHandle
 * @see ExceptionReturn
 * @see ExceptionFallback
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ExceptionHandleMeta {

    /**
     * 用于生成{@link HttpExceptionHandle}异常处理器的对象生成器
     */
    ObjectGenerate handle() default @ObjectGenerate(HttpExceptionHandle.class);

    /**
     * 进行异常处理的Class
     */
    Class<? extends HttpExceptionHandle> clazz() default HttpExceptionHandle.class;

    /**
     * 条件表达式，当条件表达式成立时使用该处理器
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.THROWABLE)
    String condition() default "";

    /**
     * 指定一个用于获取条件表达式，当条件表达式成立时使用该处理器的 SpEL 函数
     */
    @SpelAttribute(kind = SpelKind.FUNCTION, result = boolean.class, stage = Lifecycle.THROWABLE)
    String conditionFunc() default "";

    /**
     * 需要该处理器处理的异常
     */
    Class<? extends Throwable>[] exceptions() default {Exception.class};

}
