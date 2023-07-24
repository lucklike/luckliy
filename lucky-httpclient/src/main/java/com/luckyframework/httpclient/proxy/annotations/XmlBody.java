package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.XmlBodySerialization;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 04:16
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BodyParam(mimeType = "application/xml", charset = "UTF-8", serializationScheme = XmlBodySerialization.class)
public @interface XmlBody {

    @AliasFor(annotation = BodyParam.class, attribute = "charset")
    String charset() default "UTF-8";

}
