package com.luckyframework.context.event;

import com.luckyframework.context.AbstractApplicationContext;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 使用注解的形式添加一个监听器，该监听器的的实例是一个{@link EventListenerMethodApplicationListener}对象
 * 生产该对象的方法是{@link AbstractApplicationContext#initApplicationEventMulticaster()}
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/18 下午2:35
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

    /**
     * Alias for {@link #classes}.
     */
    @AliasFor("classes")
    Class<?>[] value() default {};

    /**
     * The event classes that this listener handles.
     * <p>If this attribute is specified with a single value, the
     * annotated method may optionally accept a single parameter.
     * However, if this attribute is specified with multiple values,
     * the annotated method must <em>not</em> declare any parameters.
     */
    @AliasFor("value")
    Class<?>[] classes() default {};

    /**
     * Spring Expression Language (SpEL) expression used for making the event
     * handling conditional.
     * <p>The event will be handled if the expression evaluates to boolean
     * {@code true} or one of the following strings: {@code "true"}, {@code "on"},
     * {@code "yes"}, or {@code "1"}.
     * <p>The default expression is {@code ""}, meaning the event is always handled.
     * <p>The SpEL expression will be evaluated against a dedicated context that
     * provides the following metadata:
     * <ul>
     * <li>{@code #root.event} or {@code event} for references to the
     * {@link ApplicationEvent}</li>
     * <li>{@code #root.args} or {@code args} for references to the method
     * arguments array</li>
     * <li>Method arguments can be accessed by index. For example, the first
     * argument can be accessed via {@code #root.args[0]}, {@code args[0]},
     * {@code #a0}, or {@code #p0}.</li>
     * <li>Method arguments can be accessed by name (with a preceding hash tag)
     * if parameter names are available in the compiled byte code.</li>
     * </ul>
     */
    String condition() default "";
}