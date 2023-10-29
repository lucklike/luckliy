package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.convert.ConditionalSelectionResponseConvert;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件选择器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ResultConvert(convert = ConditionalSelectionResponseConvert.class)
public @interface ConditionalSelection {


    /**
     * 条件分支，循环所有分支，如果分支的{@link Branch#assertion() assertion}表达式返回{@code true}
     * 则立即执行分支的{@link Branch#result()} () result}表达式获取结果返回，如果所有分支的条件均不满足
     * 则会检查是否配置了默认值，如果配置了默认值则返回默认值，否则会检查是否配置了exMsg，如果exMsg不为空则抛异常
     * 否则将返回null
     * @see ConditionalSelectionResponseConvert
     */
    @AliasFor("branch")
    Branch[] value() default {};

    /**
     * 同value
     */
    @AliasFor("value")
    Branch[] branch() default {};

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     */
    String defaultValue() default "";

    /**
     * 异常信息，当从条件表达式中无法获取值时又没有设置默认值时
     * 配置了该属性则会抛出携带该异常信息的异常，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     */
    String exMsg() default "The '@ConditionalSelection' annotation response conversion failed, the assertion expression in all branches results in false: {#{#$ann$.branch.!['<[❌] ' + assertion + '>']}}, no default value is configured, the current method is '#{#$method$.toString()}', and the current request instance is #{#$req$.toString()}";
}