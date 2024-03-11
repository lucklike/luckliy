package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.BinaryBodySerialization;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.core.JsonBodySerialization;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 二进制请求体参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
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

    @AliasFor(annotation = BodyParam.class, attribute = "serializationClass")
    Class<? extends BodySerialization> serializationClass() default BinaryBodySerialization.class;


}
