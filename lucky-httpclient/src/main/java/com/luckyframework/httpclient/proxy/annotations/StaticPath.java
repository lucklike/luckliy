package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.setter.PathParameterSetter;
import com.luckyframework.httpclient.proxy.statics.URLEncodeStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态路径参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@URLEncodeStaticParam
@Combination({StaticParam.class})
@StaticParam(paramSetter = PathParameterSetter.class, paramResolver = URLEncodeStaticParamResolver.class)
public @interface StaticPath {

    /**
     * <pre>
     * 路径配置
     * 格式为：key=value，
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *  root:{
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_EL_ENV}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#AN}
     *      {@value TAG#PN}
     *      {@value TAG#ARGS_N}
     *      {@value TAG#PARAM_NAME}
     *  }
     * </pre>
     */
    String[] value();

    /**
     * 是否进行URL编码
     */
    boolean urlEncode() default false;

    /**
     * 进行URL编码时采用的编码方式
     */
    String charset() default "UTF-8";
}
