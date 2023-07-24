package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.BytesResultConvert;
import com.luckyframework.httpclient.core.StringResultConvert;
import com.luckyframework.httpclient.core.impl.SaveResultResponseProcessor;
import com.luckyframework.httpclient.proxy.RequestProcessor;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpConfiguration {

    @AliasFor("path")
    String value() default "";

    @AliasFor("value")
    String path() default "";

    int connectTimeout() default 60 * 1000;

    int readTimeout() default 20 * 1000;

    int writeTimeout() default 20 * 1000;

    KV[] commonHeaders() default {};

    KV[] commonQueryParams() default {};

    KV[] commonPathParams() default {};

    Class<? extends RequestProcessor> requestProcessor() default RequestProcessor.class;

    Class<? extends SaveResultResponseProcessor> responseProcessor() default SaveResultResponseProcessor.class;

    Class<? extends StringResultConvert> stringResultConvert() default StringResultConvert.class;

    Class<? extends BytesResultConvert> bytesResultConvert() default BytesResultConvert.class;

}
