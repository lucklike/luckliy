package com.luckyframework.httpclient.generalapi.token;

import com.luckyframework.httpclient.generalapi.describe.TokenApi;
import com.luckyframework.httpclient.proxy.plugin.Plugin;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用Token管理器功能
 * <pre>
 *     注：使用该插件的方法返回值类型必须要实现{@link TokenResult}接口
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@TokenApi
@Plugin(pluginClass = TokenCacheProxyPlugin.class)
public @interface UseTokenManager {

    /**
     * 同 path
     */
    @AliasFor("path")
    String value() default "";

    /**
     *
     * 用于缓存 Token 信息本地文件路径，支持 SpEL 表达式
     * 此属性可以不配置，不配置时默认使用内存进行缓存
     */
    @AliasFor("value")
    String path() default "";

}
