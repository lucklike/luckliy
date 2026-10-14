package com.luckyframework.httpclient.generalapi.oauth2.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 禁止使用授权请求 URL 构建插件，标注该注解后，即使当前元素或者父级元素使用
 * {@link com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2AuthorizationRequest @OAuth2AuthorizationRequest}
 * 配置了授权请求插件也不会生效。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface AuthorizationRequestProhibition {
}
