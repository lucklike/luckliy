package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@HttpRequest
@SpELImport(fun = {EncoderUtils.class})
@EnableConfigurationParser(sourceType = "file")
@Combination({EnableConfigurationParser.class})
public @interface EnableLocalConfigParser {

    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String value() default "";

    /**
     * 定义配置前缀
     */
    String prefix() default "";

    /**
     * 配置源信息
     */
    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String source() default "";

}
