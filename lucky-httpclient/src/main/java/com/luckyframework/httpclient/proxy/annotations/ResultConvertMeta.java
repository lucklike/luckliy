package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.sse.SseResultConvert;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * 响应结果转换器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 * @see SseResultConvert
 * @see RespConvert
 * @see ConvertProhibition
 * @see DownloadToLocal
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConvertMetaType
public @interface ResultConvertMeta {

    /**
     * 响应结果转换器生成器，用于生成{@link ResponseConvert}对象的生成器
     */
    ObjectGenerate convert();

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ConvertMetaType.class, attribute = "value")
    Class<?> metaType() default Object.class;


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
    @AliasFor(annotation = ConvertMetaType.class, attribute = "type")
    String metaTypeExpr() default "";


    /**
     * 指定一个用于获取转换元类型的函数
     * <pre>
     *     该函数的返回值类型必须为{@link Type}类型
     * </pre>
     */
    @AliasFor(annotation = ConvertMetaType.class, attribute = "func")
    String metaTypeFunc() default "";



}
