package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpecialOperationFunction;
import com.luckyframework.httpclient.proxy.dynamic.LookUpSpecialAnnotationDynamicParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 特殊操作注解，用于指定参数设置前的最后处理
 * @see LookUpSpecialAnnotationDynamicParamResolver
 * @see URLEncoder
 * @see Base64Encoder
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/28 02:24
 */

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SpecialOperation {

    /**
     * 特殊操作接口Class
     */
    Class<? extends SpecialOperationFunction> value() default SpecialOperationFunction.class;

    /**
     * 用于创建特殊操作接口的额外信息
     */
    String funMsg() default "";

    /**
     * 是否开启功能
     */
    boolean enable() default true;
}
