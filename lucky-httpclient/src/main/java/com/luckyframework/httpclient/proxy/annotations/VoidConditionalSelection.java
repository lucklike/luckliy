package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.VoidConditionalSelectionResponseConvert;
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
@Combination(VoidResultConvert.class)
@VoidResultConvert(convert = @ObjectGenerate(VoidConditionalSelectionResponseConvert.class) )
public @interface VoidConditionalSelection {


    /**
     * 条件分支。
     * <pre>
     *    运行时会循环所有分支，如果分支的{@link Branch#assertion() assertion}表达式返回{@code true}则立即执行分支的{@link Branch#result()}表达式获取结果返回，
     *    如果所有分支的条件均不满足则会检查是否配置了默认值, 如果配置了默认值则返回默认值，否则会检查是否配置了exMsg，
     *    如果exMsg不为空则抛异常否则将返回null
     * </pre>
     * @see VoidConditionalSelectionResponseConvert
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
     *      <b>Void Response : </b>
     *      {@value TAG#VOID_RESPONSE}
     *      {@value TAG#VOID_RESPONSE_REQUEST}
     *      {@value TAG#VOID_RESPONSE_CONTENT_TYPE}
     *      {@value TAG#VOID_RESPONSE_CONTENT_LENGTH}
     *      {@value TAG#VOID_RESPONSE_STATUS}
     *      {@value TAG#VOID_RESPONSE_HEADER}
     *      {@value TAG#VOID_RESPONSE_COOKIE}
     * }
     * </pre>
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "defaultValue")
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
     *      <b>Void Response : </b>
     *      {@value TAG#VOID_RESPONSE}
     *      {@value TAG#VOID_RESPONSE_REQUEST}
     *      {@value TAG#VOID_RESPONSE_CONTENT_TYPE}
     *      {@value TAG#VOID_RESPONSE_CONTENT_LENGTH}
     *      {@value TAG#VOID_RESPONSE_STATUS}
     *      {@value TAG#VOID_RESPONSE_HEADER}
     *      {@value TAG#VOID_RESPONSE_COOKIE}
     * }
     * </pre>
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "exMsg")
    String exMsg() default "The '@VoidConditionalSelection' annotation response conversion failed, the assertion expression in all branches results in false: {#{$ann$.branch.!['<[❌] ' + assertion + '>']}}, no default value is configured, the current method is '#{$method$.toString()}', the current http request message is [#{$reqMethod$.toString()}] #{$url$}";

    /**
     * 是否导入响应实例{@link VoidResponse}
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importVoidRespInstance")
    boolean importVoidRespInstance() default true;

    /**
     * 是否导入响应体
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importBody")
    boolean importBody() default true;

    /**
     * 是否导入响应头
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importHeader")
    boolean importHeader() default true;
}
