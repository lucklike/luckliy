package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.VoidResponseSelectConvert;
import com.luckyframework.reflect.Combination;
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
@Combination(VoidResultConvert.class)
@VoidResultConvert(convert = @ObjectGenerate(VoidResponseSelectConvert.class))
public @interface VoidResultSelect {

    /**
     * 取值表达式
     * <pre>
     * 响应状态码：           <b>$status$</b>，其中<b>$status$</b>表示响应状态码。
     * 响应体的长度：         <b>contentLength$</b>，其中<b>contentLength$</b>表示响应体长度。
     * 响应头取值表达式：      <b>$respHeader$.${key}</b>，其中<b>$respHeader$</b>为固定的前缀，表示响应头信息。
     * 响应头Cookie取值表达式：<b>$respCookie$.${key}</b>，其中<b>$respCookie$</b>为固定的前缀，表示响应中Cookie的信息。
     *
     * 请参照{@link ConfigurationMap#getProperty(String)}的用法，
     * 从数组中取值：$respHeader$.array[0].user或$respHeader$[1].user.password
     * 从对象中取值：$respHeader$.object.user或$respHeader$.user.password
     * </pre>
     */
    String value();

    /**
     * 当取值表达式取不到值时可以通过这个属性来设置默认值，
     * 这里允许使用SpEL表达式来生成一个默认值，SpEL表达式部分需要写在#{}中
     *
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
     *
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
    @AliasFor(annotation = VoidResultConvert.class, attribute = "exception")
    String exception() default "The '@VoidResultSelect' annotation response conversion failed, the value specified by the value expression '#{$ann$.value}' could not be retrieved from the response, and the default value was not configured. The current method is '#{$method$.toString()}'. the current http request message is [#{$reqMethod$.toString()}] #{$url$}";

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
