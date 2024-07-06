package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态参数配置注解，为程序提供个性化静态参数配置扩展的能力：
 *
 * @see BasicAuth
 * @see StaticResource
 * @see StaticHeader
 * @see StaticQuery
 * @see StaticForm
 * @see StaticPath
 * @see StaticCookie
 * @see StaticXmlBody
 * @see StaticJsonBody
 * @see PropertiesJsonObject
 * @see StaticBinaryBody
 * @see StaticBody
 * @see StaticFormBody
 * @see UseProxy
 * @see Timeout
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface StaticParam {

    /**
     * 用于生成{@link ParameterSetter}参数设置器的对象生成器
     */
    ObjectGenerate setter();

    /**
     * 用于生成{@link StaticParamResolver}静态参数解析器的对象生成器
     */
    ObjectGenerate resolver();

}
