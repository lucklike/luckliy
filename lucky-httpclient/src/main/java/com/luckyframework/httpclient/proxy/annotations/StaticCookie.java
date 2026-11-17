package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.setter.CookieParameterSetter;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSeparator;
import com.luckyframework.httpclient.proxy.statics.SpELValueFieldSeparationStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态Cookie参数配置注解
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
        setter = @ObjectGenerate(CookieParameterSetter.class),
        resolver = @ObjectGenerate(SpELValueFieldSeparationStaticParamResolver.class)
)
@Repeatable(StaticCookies.class)
public @interface StaticCookie {

    /**
     * Cookie配置
     * 格式为：key=value,
     * key和value部分均支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    @SpelAttribute(kind = SpelKind.KEY_VALUE, result = String.class)
    String[] value();

    /**
     * 属性名与属性值之间的分隔符
     * <p>同时标记为同注解内{@code KEY_VALUE}属性的分隔符来源。
     */
    @SpelSeparator
    String separator() default "=";

    /**
     * 条件表达式，只有该表达式为true时，才会进行参数的设置
     */
    @SpelAttribute(kind = SpelKind.CONDITION, result = boolean.class, stage = Lifecycle.REQUEST_INIT)
    String condition() default "";
}
