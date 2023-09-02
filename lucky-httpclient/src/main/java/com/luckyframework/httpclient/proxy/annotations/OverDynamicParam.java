package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.httpclient.proxy.ParameterSetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 覆盖动态参数注解属性的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OverDynamicParam {


    /**
     * 指定参数设置器，用于覆盖{@link DynamicParam @DynamicParam}注解的{@link DynamicParam#paramSetter}属性
     */
    Class<? extends ParameterSetter> overParamSetter() default ParameterSetter.class;

    /**
     * 指定参数设置器额外信息，用于覆盖{@link DynamicParam @DynamicParam}注解的{@link DynamicParam#paramSetterMsg()}属性
     */
    String overParamSetterMsg() default "";

    /**
     * 指定参数处理器，用于覆盖{@link DynamicParam @DynamicParam}注解的{@link DynamicParam#paramProcessor}属性
     */
    Class<? extends ParameterProcessor> overParamProcessor() default ParameterProcessor.class;

    /**
     * 指定参数处理器额外信息，用于覆盖{@link DynamicParam @DynamicParam}注解的{@link DynamicParam#paramProcessorMsg()}属性
     */
    String overParamProcessorMsg() default "";



}
