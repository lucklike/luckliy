package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于配置熔断器的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 02:56
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FuseMeta {

    ObjectGenerate fuse();

}
