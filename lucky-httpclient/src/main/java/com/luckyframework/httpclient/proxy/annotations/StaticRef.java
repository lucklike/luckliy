package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.setter.RefParameterSetter;
import com.luckyframework.httpclient.proxy.statics.RefStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Basic Auth 参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(
        setter = @ObjectGenerate(RefParameterSetter.class),
        resolver = @ObjectGenerate(RefStaticParamResolver.class)
)
public @interface StaticRef {

    /**
     * 用户名,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     * <pre>
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
     *
     * </pre>
     */
    String value();

    RefType type() default RefType.ADD;
}
