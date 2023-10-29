package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.setter.ResourceParameterSetter;
import com.luckyframework.httpclient.proxy.impl.statics.ResourceStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态资源参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(paramSetter = ResourceParameterSetter.class, paramResolver = ResourceStaticParamResolver.class)
public @interface StaticResource {

    /**
     * 资源参数配置,格式为：key=value,支持SpEL表达式
     */
    String[] value();

}
