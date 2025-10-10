package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Semaphore;

import static com.luckyframework.httpclient.proxy.configapi.Source.RESOURCE;


/**
 * <b>如下所有配置均支持松散绑定</b><br/><br/>
 * 无注解化配置注解-提供从本地文件中获取请求配置的功能<br/>
 * 某个被@EnableConfigurationParser注解标注的Java接口<br/>
 * 顶层的key需要与@EnableConfigurationParser注解的prefix属性值一致，如果注解没有配置prefix，则key使用接口的全类名<br/>
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 * @see EnableConfigurationParser
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EnableConfigurationParser(sourceType = RESOURCE)
@Combination({EnableConfigurationParser.class})
public @interface EnableResourceParser {

    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String value() default "classpath:/api/#{$class$.getSimpleName()}.yml";

    /**
     * 定义配置前缀，用于唯一定位到一段配置
     */
    String prefix() default "";

    /**
     * 配置源信息，本地文件位置
     * <pre>
     *     classpath:/api/EnvApi.yml
     *     file:/usr/local/config/api/EnvApi.yml
     * </pre>
     */
    /**
     * 配置源信息
     */
    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "source")
    String source() default "classpath:/api/#{$class$.getSimpleName()}.yml";

    /**
     * 拦截器优先级，数值越高优先级越低
     */
    @AliasFor(annotation = EnableConfigurationParser.class, attribute = "priority")
    int priority() default PriorityConstant.CONFIG_API_PRIORITY;

}
