package com.luckyframework.httpclient.proxy.destroy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注册销毁逻辑的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 01:27
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Destroys {
    Destroy[] value();
}
