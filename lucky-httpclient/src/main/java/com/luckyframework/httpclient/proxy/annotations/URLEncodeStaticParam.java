package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 05:52
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface URLEncodeStaticParam {

    /**
     * 是否进行URL编码
     */
    boolean urlEncode() default false;

    /**
     * 进行URL编码时采用的编码方式
     */
    String charset() default "UTF-8";
}
