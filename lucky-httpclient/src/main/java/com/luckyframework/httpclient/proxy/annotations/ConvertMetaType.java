package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * 响应转换元类型
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ConvertMetaType {

    /**
     * 转换元类型
     */
    Class<?> value() default Object.class;

    /**
     * 转换元类型, 支持SpEL表达式
     * <pre>
     *     表达式的结果支持如下类型
     *     1.{@link Type}
     *     2.{@link Class}
     *     3.{@link ResolvableType}({@link ResolvableType#getType()})
     *     4.{@link SerializationTypeToken}({@link SerializationTypeToken#getType()})
     *     5.{@link String}({@link Class#forName(String)})
     * </pre>
     *
     * @see SpELVariableNote
     */
    String type() default "";


    /**
     * 指定一个用于获取转换元类型的函数
     * <pre>
     *     该函数的返回值类型必须如下类型：
     *     1.{@link Type}
     *     2.{@link Class}
     *     3.{@link ResolvableType}({@link ResolvableType#getType()})
     *     4.{@link SerializationTypeToken}({@link SerializationTypeToken#getType()})
     *     5.{@link String}({@link Class#forName(String)})
     * </pre>
     */
    String func() default "";

    /**
     * 强制指定响应体的 Content-Type
     */
    String contentType() default "";
}