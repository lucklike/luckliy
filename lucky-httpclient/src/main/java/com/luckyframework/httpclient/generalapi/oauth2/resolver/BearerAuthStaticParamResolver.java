package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.BearerAuth;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Bearer Token 认证参数解析器（RFC 6750 §2.1）。
 * <pre>
 *     按照规范生成 {@code Authorization: Bearer <token>} 请求头：
 *     Authorization = prefix + token
 *
 *     token未配置时返回空参数列表。
 *     prefix为空时记录警告日志（裸 token 不符合 RFC 6750 规范，但仍可继续运行）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class BearerAuthStaticParamResolver implements StaticParamResolver {

    private static final Logger log = LoggerFactory.getLogger(BearerAuthStaticParamResolver.class);

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {

        // 优先从上下文链中获取合并注解，兼容用户自定义组合注解中 @AliasFor 透传场景
        // （与 OAuth2ClientAuthStaticParamResolver 保持一致风格）
        BearerAuth bearerAuth = context.getMergedAnnotationCheckParent(BearerAuth.class);
        if (bearerAuth == null) {
            bearerAuth = context.toAnnotation(BearerAuth.class);
        }

        String token = context.parseExpression(bearerAuth.token(), String.class);

        // 访问令牌未配置时不设置认证头
        if (!StringUtils.hasText(token)) {
            return Collections.emptyList();
        }

        String header = context.parseExpression(bearerAuth.header(), String.class);
        String prefix = context.parseExpression(bearerAuth.prefix(), String.class);

        // prefix 为空时给出提示（裸 token 不符合 RFC 6750，但不阻断调用）
        if (!StringUtils.hasText(prefix)) {
            log.warn("[OAuth2] BearerAuth prefix is empty, the Authorization header will contain " +
                    "a bare token without 'Bearer ' prefix which does not conform to RFC 6750 §2.1");
        }

        return Collections.singletonList(new ParamInfo(header, prefix + token));
    }
}
