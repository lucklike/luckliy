package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.ResponseSelectConvert;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于{@link ConfigurationMap}和{@code SpEL表达式}实现的响应结果转换器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ResultConvert(convert = ResponseSelectConvert.class)
public @interface ResultSelect {

    String ATTRIBUTE_SELECT = "select";
    String ATTRIBUTE_DEFAULT_VALUE = "defaultValue";
    String ATTRIBUTE_EX_MSG = "exMsg";
    /**
     * 取值表达式
     * <pre>
     * 响应体取值表达式：      <b>$body$.${key}</b>，其中<b>$body$</b>为固定的前缀，表示响应体信息。
     * 响应头取值表达式：      <b>$header$.${key}</b>，其中<b>$header$</b>为固定的前缀，表示响应头信息。
     * 响应头Cookie取值表达式：<b>$cookie$.${key}</b>，其中<b>$cookie$</b>为固定的前缀，表示响应中Cookie的信息。
     *
     * 请参照{@link ConfigurationMap#getProperty(String)}的用法，
     * 从数组中取值：$body$.array[0].user或$body$[1].user.password
     * 从对象中取值：$body$.object.user或$body$.user.password
     * </pre>
     */
    @AliasFor("select")
    String value() default "";

    /**
     * 同value
     */
    @AliasFor("value")
    String select() default "";

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     *
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_EL_ENV}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#AN}
     *      {@value TAG#PN}
     *      {@value TAG#ARGS_N}
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
     *
     * </pre>
     */
    String defaultValue() default "";

    /**
     * 异常信息，当从条件表达式中无法获取值时又没有设置默认值时
     * 配置了该属性则会抛出携带该异常信息的异常，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_EL_ENV}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#AN}
     *      {@value TAG#PN}
     *      {@value TAG#ARGS_N}
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
    String exMsg() default "The '@ResultSelect' annotation response conversion failed, the value specified by the value expression '#{$ann$.select}' could not be retrieved from the response, and the default value was not configured. The current method is '#{$method$.toString()}'. the current http request message is [#{$reqMethod$.toString()}] #{$url$}";
}
