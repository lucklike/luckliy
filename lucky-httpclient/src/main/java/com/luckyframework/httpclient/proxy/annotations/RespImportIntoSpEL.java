package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.Response;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * 决定是否需要将响应结果导入SpEL环境的注解
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RespImportIntoSpEL {

    /**
     * 是否导入响应实例{@link Response}
     */
    boolean importRespInstance() default true;

    /**
     * 是否导入响应体{@link Response#getEntity(Type)}
     */
    boolean importBody() default true;

    /**
     * 是否导入响应头{@link Response#getSimpleHeaders()}
     */
    boolean importHeader() default true;
}