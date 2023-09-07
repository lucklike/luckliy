package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.core.XmlBodySerialization;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Xml请求体参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BodyParam
@Inherited
public @interface XmlBody {


    @AliasFor(annotation = BodyParam.class, attribute = "mimeType")
    String mimeType() default "application/xml";

    @AliasFor(annotation = BodyParam.class, attribute = "charset")
    String charset() default "UTF-8";

    @AliasFor(annotation = BodyParam.class, attribute = "serializationClass")
    Class<? extends BodySerialization> serializationClass() default XmlBodySerialization.class;

}
