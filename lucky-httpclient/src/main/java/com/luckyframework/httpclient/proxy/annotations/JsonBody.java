package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.impl.BodyParameterProcessor.CHARSET;
import static com.luckyframework.httpclient.proxy.impl.BodyParameterProcessor.MIME_TYPE;
import static com.luckyframework.httpclient.proxy.impl.BodyParameterProcessor.SERIALIZATION_SCHEME_CLASS;

/**
 * JSON请求体参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BodyParam(extraConfig = {
        @KV(name = MIME_TYPE, value = "application/json"),
        @KV(name = CHARSET, value = "UTF-8"),
        @KV(name = SERIALIZATION_SCHEME_CLASS, value = "com.luckyframework.httpclient.core.JsonBodySerialization")
})
public @interface JsonBody {

}
