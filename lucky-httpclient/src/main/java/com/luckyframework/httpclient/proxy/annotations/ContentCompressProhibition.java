package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内容解压
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 02:07
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(StaticHeader.class)
@StaticHeader("[-]Accept-Encoding= ")
public @interface ContentCompressProhibition {

}
