package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.RequestMethod;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpRequest(method = RequestMethod.POST)
public @interface Post {

    @AliasFor(annotation = HttpRequest.class, value = "url")
    String value() default "";

    @AliasFor(annotation = HttpRequest.class, value = "ignoreClassConvert")
    boolean ignoreClassConvert() default false;

}
