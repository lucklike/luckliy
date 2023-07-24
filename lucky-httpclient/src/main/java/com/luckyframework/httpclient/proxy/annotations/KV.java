package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.*;

/**
 * ØØ
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 03:59
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KV {

    String name();

    String value();

}
