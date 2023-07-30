package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.httpclient.proxy.ParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Http参数注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpParam {

    /**
     * Http参数名称
     */
    @AliasFor("name")
    String value() default "";

    /**
     * Http参数名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 用于定义文件上传时的文件名
     */
    String fileName() default "";

    /**
     * 指定参数设置器，用于将参数设置到Http请求实例中
     */
    Class<? extends ParameterSetter> paramSetter();

    /**
     * 指定参数处理器，用于将原始参数转化为目标参数
     */
    Class<? extends ParameterProcessor> paramProcessor();

    /**
     * 额外的配置信息
     */
    KV[] extraConfig() default {};
}
