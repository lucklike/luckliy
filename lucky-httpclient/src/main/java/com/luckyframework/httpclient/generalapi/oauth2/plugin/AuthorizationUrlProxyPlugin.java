package com.luckyframework.httpclient.generalapi.oauth2.plugin;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.AuthorizationUrlBuilder;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2AuthorizationRequest;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ServerAddressMeta;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;
import com.luckyframework.httpclient.proxy.url.BaseURLGetter;
import com.luckyframework.httpclient.proxy.url.DomainNameContext;
import com.luckyframework.httpclient.proxy.url.HttpRequestContext;
import com.luckyframework.httpclient.proxy.url.PathGetter;

/**
 * 授权请求 URL 构建插件（RFC 6749 §4.1.1）。
 * <pre>
 *     拦截标注了 {@link OAuth2AuthorizationRequest @OAuth2AuthorizationRequest} 的代理方法，
 *     沿用框架内置流程解析出授权端点地址，并按注解配置补齐 OAuth2 授权请求的 query 参数，
 *     最终将构建好的完整授权 URL 作为方法返回值直接返回，不会发起真实的 HTTP 请求。
 *
 *     端点地址的解析完全复用框架逻辑：
 *     1.域名部分由类上的 {@link ServerAddressMeta @ServerAddress} 配置（或 yml 中的服务器地址）解析得到；
 *     2.路径部分由方法上的 {@link HttpRequest @Get/@Post...} 配置解析得到；
 *     两者通过 {@link StringUtils#joinUrlPath} 拼接为授权端点，因此不再有 authorizeEndpoint 属性。
 *
 *     用法示例：
 *     {@code
 *     @Get("/oauth2/authorize")
 *     @OAuth2AuthorizationRequest(clientId = "#{clientId}", redirectUri = "#{redirectUri}", scope = "#{scope}")
 *     String buildAuthorizationUrl(String clientId, String redirectUri, String scope);
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 * @see OAuth2AuthorizationRequest
 */
public class AuthorizationUrlProxyPlugin implements ProxyPlugin {

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();
        MethodContext mc = meta.getMethodContext();

        // 未标注授权请求注解时，执行原方法（正常走 HTTP 流程）
        OAuth2AuthorizationRequest ann = mc.getMergedAnnotationCheckParent(OAuth2AuthorizationRequest.class);
        if (ann == null) {
            return decorator.proceed();
        }

        // 沿用框架内置流程解析授权端点：ServerAddress 配置的域名 + HttpRequest 配置的路径
        String endpoint = StringUtils.joinUrlPath(resolveBaseUrl(mc), resolvePath(mc));

        // 解析注解属性中的 SpEL 表达式，补齐授权请求的 query 参数
        AuthorizationUrlBuilder builder = AuthorizationUrlBuilder.of(endpoint)
                .responseType(mc.parseExpression(ann.responseType(), String.class))
                .clientId(mc.parseExpression(ann.clientId(), String.class))
                .redirectUri(mc.parseExpression(ann.redirectUri(), String.class))
                .scope(mc.parseExpression(ann.scope(), String.class))
                .state(mc.parseExpression(ann.state(), String.class))
                .codeChallenge(mc.parseExpression(ann.codeChallenge(), String.class))
                .codeChallengeMethod(mc.parseExpression(ann.codeChallengeMethod(), String.class));

        // 直接返回构建好的授权 URL，不发起真实 HTTP 请求
        return builder.build();
    }

    /**
     * 复用框架内置流程解析类上 {@link ServerAddressMeta @ServerAddress} 配置的域名地址。
     *
     * @param mc 方法上下文
     * @return 域名地址，未配置时返回空字符串
     * @throws Exception 域名解析过程中可能抛出的异常
     */
    private String resolveBaseUrl(MethodContext mc) throws Exception {
        ServerAddressMeta domainMetaAnn = mc.getMergedAnnotationCheckParent(ServerAddressMeta.class);
        if (domainMetaAnn == null) {
            return ServerAddressMeta.EMPTY;
        }
        DomainNameContext domainNameContext = new DomainNameContext(mc, domainMetaAnn);
        BaseURLGetter baseURLGetter = mc.generateObject(domainMetaAnn.getter());
        return baseURLGetter.getBaseUrl(domainNameContext);
    }

    /**
     * 复用框架内置流程解析方法上 {@link HttpRequest @Get/@Post...} 配置的路径。
     *
     * @param mc 方法上下文
     * @return 请求路径，未配置时返回空字符串
     * @throws Exception 路径解析过程中可能抛出的异常
     */
    private String resolvePath(MethodContext mc) throws Exception {
        HttpRequest httpReqAnn = mc.getMergedAnnotationCheckParent(HttpRequest.class);
        if (httpReqAnn == null) {
            return "";
        }
        HttpRequestContext httpRequestContext = new HttpRequestContext(mc, httpReqAnn);
        PathGetter pathGetter = mc.generateObject(httpReqAnn.urlGetter());
        return pathGetter.getUrl(httpRequestContext, false);
    }
}
