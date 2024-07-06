package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.convert.ThrowsExceptionResponseConvert;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于抛出异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/29 9:48
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(ResultConvert.class)
@ResultConvert(convert = @ObjectGenerate(ThrowsExceptionResponseConvert.class) )
public @interface Throws {

    /**
     * 异常描述注解
     */
    Ex[] value();

    /**
     * 结果表达式，用于返回最终结果
     * <pre>
     * SpEL表达式内置参数有
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
    String result() default "";

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "metaType")
    Class<?> metaType() default Object.class;

}
