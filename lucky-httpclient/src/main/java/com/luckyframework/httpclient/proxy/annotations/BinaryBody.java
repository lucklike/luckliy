package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.serialization.BinaryBodySerialization;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 二进制请求体参数注解
 * <pre>
 *   支持的参数类型如下：
 *      {@link byte[]}
 *      {@link Byte[]}
 *      {@link InputStream}
 *      {@link File}
 *      {@link Resource}
 *      {@link MultipartFile}
 *      {@link HttpFile}
 *    如果参数不是以上类型，则会尝试使用{@code  ConversionUtils.conversion(object, Resource.class)}
 *    方法将参数转化为{@link Resource}类型
 *
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 *
 * @see BinaryBodySerialization
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BodyParam
public @interface BinaryBody {

    @AliasFor(annotation = BodyParam.class, attribute = "mimeType")
    String mimeType() default "application/octet-stream";

    @AliasFor(annotation = BodyParam.class, attribute = "charset")
    String charset() default "";

    @AliasFor(annotation = BodyParam.class, attribute = "serialization")
    ObjectGenerate serialization() default @ObjectGenerate(BinaryBodySerialization.class);


}
