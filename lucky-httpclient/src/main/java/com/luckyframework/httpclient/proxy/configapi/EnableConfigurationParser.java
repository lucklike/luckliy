package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@InterceptorRegister(
        intercept = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS),
        priority = 9000
)
@ResultConvert(
        convert = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS))
@StaticParam(
        resolver = @ObjectGenerate(clazz = ConfigurationApiFunctionalSupport.class, scope = Scope.CLASS),
        setter = @ObjectGenerate(ConfigApiParameterSetter.class)
)
@HttpRequest
@SpELImport(fun = {EncoderUtils.class})
@Combination({StaticParam.class, InterceptorRegister.class})
public @interface EnableConfigurationParser {

    /**
     * 定义配置前缀
     */
    String prefix() default "";

    /**
     * 配置源信息
     */
    String source() default "";

    /**
     * 配置源类型
     */
    String sourceType() default "";
}
