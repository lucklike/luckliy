package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.ConditionalSelectionException;
import com.luckyframework.httpclient.proxy.convert.ConditionalSelectionResponseConvert;
import com.luckyframework.reflect.Combination;
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
@Combination(ResultConvert.class)
@ResultConvert(convert = @ObjectGenerate(ConditionalSelectionResponseConvert.class))
public @interface ConditionalSelection {


    /**
     * 同branch
     */
    @AliasFor("branch")
    Branch[] value() default {};

    /**
     * 条件分支，执行逻辑如下：
     * <pre>
     *     1.循环所有{@link Branch @Branch}分支，挨个进行处理
     *     2.如果{@link Branch#assertion()}表达式返回<b>true</b>
     *          a.如果分支注解配置了{@link Branch#result()}，则返回此表达式得到的值
     *          b.如果分支注解配置了{@link Branch#exception()}，则会抛出表达式得到的异常
     *          c.都未配置时会抛出一个{@link ConditionalSelectionException}异常
     *     3.如果所有{@link Branch#assertion()}表达式均返回<b>false</b>
     *          a.如果配置了默认值{@link #defaultValue()},则返回此默认值
     *          b.如果配置了异常{@link #exception()},则抛出此异常
     *          c.都未配置时返回<b>null</b>
     * </pre>
     *
     * @see ConditionalSelectionResponseConvert
     */
    @AliasFor("value")
    Branch[] branch() default {};

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "defaultValue")
    String defaultValue() default "";

    /**
     * 异常信息，当从条件表达式中无法获取值时又没有设置默认值时
     * 配置了该属性则会抛出携带该异常信息的异常，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "exception")
    String exception() default "The '@ConditionalSelection' annotation response conversion failed, the assertion expression in all branches results in false: {#{$ann$.branch.!['<[❌] ' + assertion + '>']}}, no default value is configured, the current method is '#{$method$.toString()}', the current http request message is [#{$reqMethod$.toString()}] #{$url$}";

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "metaType")
    Class<?> metaType() default Object.class;
}
