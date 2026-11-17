package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.annotation.*;
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
    @SpelAttribute(result = {Type.class, Class.class, ResolvableType.class,
            SerializationTypeToken.class, String.class}, stage = Lifecycle.METHOD)
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
    @SpelAttribute(kind = SpelKind.FUNCTION, result = {Type.class, Class.class, ResolvableType.class,
            SerializationTypeToken.class, String.class}, stage = Lifecycle.METHOD)
    String typeFunc() default "";

    /**
     * 强制指定响应体的 Content-Type
     */
    @SpelAttribute(result = String.class, stage = Lifecycle.METHOD)
    String respContentType() default "";

    /**
     * 指定一个用于强制指定响应体的 Content-Type 的函数
     */
    @SpelAttribute(kind = SpelKind.FUNCTION, result = String.class, stage = Lifecycle.METHOD)
    String respContentTypeFunc() default "";
}