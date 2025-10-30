package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件描述注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/05/27 09:30
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Conditions.class)
public @interface Condition {

    /**
     * 是否开启注解功能
     */
    String enable() default "";

    /**
     * <pre>
     * 断言SpEL表达式, <b>SpEL表达式部分需要写在#{}中</b>
     * 返回值必须是{@link Boolean}类型
     *
     * @see SpELVariableNote
     */
    String assertion();

    /**
     * 结果表达式，这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     *
     * @see SpELVariableNote
     */
    String result() default "";

    /**
     * 异常信息,如果此处的返回结果为{@link Throwable},则会直接抛出该异常，否则会抛出{@link ActivelyThrownException} ，<b>SpEL表达式部分需要写在#{}中</b>
     *
     * @see SpELVariableNote
     */
    String exception() default "";

    /**
     * 返回值类型
     */
    Class<?> returnType() default Object.class;
}
