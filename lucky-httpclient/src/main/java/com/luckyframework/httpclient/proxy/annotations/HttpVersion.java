package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.Version;
import com.luckyframework.httpclient.proxy.setter.HttpVersionParameterSetter;
import com.luckyframework.httpclient.proxy.statics.HttpVersionStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 超时时间参数配置注解
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
        setter = @ObjectGenerate(HttpVersionParameterSetter.class),
        resolver = @ObjectGenerate(HttpVersionStaticParamResolver.class)
)
public @interface HttpVersion {

    /**
     * HTTP版本
     */
    Version value();

}
