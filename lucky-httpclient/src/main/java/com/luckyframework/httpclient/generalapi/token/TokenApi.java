package com.luckyframework.httpclient.generalapi.token;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为获取Token的API
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:14
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TokenApi {

}
