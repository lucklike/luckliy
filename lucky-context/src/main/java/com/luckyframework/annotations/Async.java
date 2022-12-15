package com.luckyframework.annotations;

import com.luckyframework.async.AsyncResult;

import java.lang.annotation.*;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 * Can also be used at the type level, in which case all of the type's methods are
 * considered as asynchronous. Note, however, that {@code @Async} is not supported
 * on methods declared within a
 * {@link Configuration @Configuration} class.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link org.springframework.util.concurrent.ListenableFuture} or
 * {@link java.util.concurrent.CompletableFuture} types which allow for richer
 * interaction with the asynchronous task and for immediate composition with
 * further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code Future} that can be used to track the result of the asynchronous method
 * execution. However, since the target method needs to implement the same signature,
 * it will have to return a temporary {@code Future} handle that just passes a value
 * through: e.g. Lucky's {@link AsyncResult}, EJB 3.1's {@link javax.ejb.AsyncResult},
 * or {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {

    /**
     * A qualifier value for the specified asynchronous operation(s).
     * <p>May be used to determine the target executor to be used when executing
     * the asynchronous operation(s), matching the qualifier value (or the bean
     * name) of a specific {@link java.util.concurrent.Executor Executor} or
     * {@link org.springframework.core.task.TaskExecutor TaskExecutor}
     * bean definition.
     * <p>When specified on a class-level {@code @Async} annotation, indicates that the
     * given executor should be used for all methods within the class. Method-level use
     * of {@code Async#value} always overrides any value set at the class level.
     * @since 3.1.2
     */
    String value() default "";

}
