package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.setter.BodyParameterSetter;
import com.luckyframework.httpclient.proxy.statics.BodyStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态Query参数配置注解
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
        setter = @ObjectGenerate(BodyParameterSetter.class),
        resolver = @ObjectGenerate(BodyStaticParamResolver.class)
)
public @interface StaticBody {


    /**
     * <pre>
     * Body配置，支持SpEL表达式，表达式部分需要写在#{}中
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
    String body() default "";

    String mimeType();

    /**
     * 进行URL编码时采用的编码方式
     */
    String charset() default "UTF-8";

    /**
     * 请求体处理器，必须是一个{@link BodyStaticParamResolver.BodyHandle}接口的子类
     */
    ObjectGenerate bodyHandle() default @ObjectGenerate(BodyStaticParamResolver.DefaultBodyHandle.class);

}
