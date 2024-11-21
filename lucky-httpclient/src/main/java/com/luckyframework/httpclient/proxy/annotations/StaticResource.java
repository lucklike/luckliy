package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.setter.ResourceParameterSetter;
import com.luckyframework.httpclient.proxy.statics.ResourceStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
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
@StaticParam(
        setter = @ObjectGenerate(ResourceParameterSetter.class),
        resolver = @ObjectGenerate(ResourceStaticParamResolver.class)
)
@Repeatable(StaticResources.class)
public @interface StaticResource {

    /**
     * <pre>
     * 静态资源参数配置
     * 格式为：key=value，
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *  root:{
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *  }
     * </pre>
     */
    String[] value();

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";

    /**
     * 条件表达式，只有该表达式为true时，才会进行参数的设置
     */
    String condition() default "";

}
