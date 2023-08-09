package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.impl.FormParameterSetter;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.httpclient.proxy.impl.SpELValueFieldEqualSeparationStaticParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态Form表单参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@StaticParam
public @interface StaticForm {

    /**
     * Form表单参数配置,格式为：key=value,支持SpEL表达式
     */
    String[] value();

    //----------------------------------------------------------------
    //                   @StaticParam注解规范必要参数
    //----------------------------------------------------------------

    Class<? extends ParameterSetter> paramSetter() default FormParameterSetter.class;

    String paramSetterMsg() default "";

    Class<? extends StaticParamResolver> paramResolver() default SpELValueFieldEqualSeparationStaticParamResolver.class;

    String paramResolverMsg() default "";
}
