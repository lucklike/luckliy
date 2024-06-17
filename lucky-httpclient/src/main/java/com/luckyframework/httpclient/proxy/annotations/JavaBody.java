package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.serialization.BinaryBodySerialization;
import com.luckyframework.httpclient.core.serialization.JavaObjectBodySerialization;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * java对象请求体参数注解
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
public @interface JavaBody {

    @AliasFor(annotation = BodyParam.class, attribute = "mimeType")
    String mimeType() default "application/x-java-serialized-object";

    @AliasFor(annotation = BodyParam.class, attribute = "charset")
    String charset() default "";

    @AliasFor(annotation = BodyParam.class, attribute = "serialization")
    ObjectGenerate serialization() default @ObjectGenerate(JavaObjectBodySerialization.class);


}
