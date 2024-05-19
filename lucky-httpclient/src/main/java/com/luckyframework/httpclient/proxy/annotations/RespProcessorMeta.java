package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.ResponseProcessor;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应处理器元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RespProcessorMeta {

    /**
     * 是否开启功能
     */
    @AliasFor("enable")
    boolean value() default true;

    /**
     * 是否开启功能
     */
    @AliasFor("value")
    boolean enable() default true;

    /**
     * 构建{@link ResponseProcessor}的对象创建器
     */
    ObjectGenerate process();
}
