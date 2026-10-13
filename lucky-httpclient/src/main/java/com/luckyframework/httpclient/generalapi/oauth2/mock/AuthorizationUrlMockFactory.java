package com.luckyframework.httpclient.generalapi.oauth2.mock;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.oauth2.AuthorizationUrlBuilder;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.AuthorizationRequest;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.mock.MockContext;
import com.luckyframework.httpclient.proxy.mock.MockResponse;
import com.luckyframework.httpclient.proxy.mock.MockResponseFactory;

/**
 * 授权请求 URL 构建的 Mock 响应工厂（RFC 6749 §4.1.1）。
 * <pre>
 *     需要由使用方通过 {@code @MockMeta(mock = @ObjectGenerate(AuthorizationUrlMockFactory.class))}
 *     挂载到框架的 Mock 机制上：目标方法声明为一个普通的 {@code @Get("/oauth2/authorize")} 请求方法，
 *     框架会先完成服务端基础URL解析与路径拼装、构建出完整的 {@link Request}，
 *     随后在真正发起网络请求之前交由 Mock 拦截。
 *
 *     这里直接复用框架已经解析好的 {@code request.getBaseUrl()} 与 {@code request.getPath()}
 *     作为授权端点前缀（无需再手动解析 ServerAddress/域名），仅按 OAuth2 协议补齐授权所需的
 *     query 参数（response_type、client_id、redirect_uri、scope、state 及 PKCE 参数），
 *     并把拼好的完整授权 URL 作为响应体返回。方法返回类型为 String，即得到最终授权重定向 URL，
 *     整个过程不产生任何真实网络调用。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 * @see AuthorizationRequest
 * @see AuthorizationUrlBuilder
 */
public class AuthorizationUrlMockFactory implements MockResponseFactory {

    @Override
    public Response createMockResponse(Request request, MockContext context) {
        MethodContext mc = context.getContext();
        AuthorizationRequest ann = mc.getMergedAnnotation(AuthorizationRequest.class);
        if (ann == null) {
            return MockResponse.create()
                    .request(request)
                    .status(200)
                    .contentType("text/plain")
                    .body(request.getUrl());
        }

        // 授权端点前缀直接取自框架解析好的 Request：base = 配置的服务地址，path = 方法上 @Get 声明的路径
        String endpoint = join(request.getBaseUrl(), request.getPath());

        // 解析注解属性中的 SpEL 表达式（引用方法入参），未写 SpEL 时按字面量返回
        String responseType = mc.parseExpression(ann.responseType(), String.class);
        String clientId = mc.parseExpression(ann.clientId(), String.class);
        String redirectUri = mc.parseExpression(ann.redirectUri(), String.class);
        String scope = mc.parseExpression(ann.scope(), String.class);
        String state = mc.parseExpression(ann.state(), String.class);
        String codeChallenge = mc.parseExpression(ann.codeChallenge(), String.class);
        String codeChallengeMethod = mc.parseExpression(ann.codeChallengeMethod(), String.class);

        String authorizeUrl = AuthorizationUrlBuilder.of(endpoint)
                .responseType(responseType)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .state(state)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .build();

        return MockResponse.create()
                .request(request)
                .status(200)
                .contentType("text/plain")
                .body(authorizeUrl);
    }

    /**
     * 拼接框架解析出的基础URL与方法声明的路径，保证中间恰好一个 '/' 分隔。
     */
    private static String join(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return path == null ? "" : path;
        }
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        boolean baseEnds = baseUrl.endsWith("/");
        boolean pathStarts = path.startsWith("/");
        if (baseEnds && pathStarts) {
            return baseUrl + path.substring(1);
        }
        if (!baseEnds && !pathStarts) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }
}
