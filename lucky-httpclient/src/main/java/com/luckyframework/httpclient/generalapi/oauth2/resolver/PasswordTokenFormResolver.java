package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.PasswordToken;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码模式表单参数解析器（RFC 6749 §4.3.1）。
 * <pre>
 *     从 {@link PasswordToken} 注解中解析该模式的必要表单参数：
 *     grant_type=password、username、password
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class PasswordTokenFormResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        PasswordToken ann = context.getMergedAnnotationCheckParent(PasswordToken.class);
        if (ann == null) {
            ann = context.toAnnotation(PasswordToken.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", "password"));

        String username = context.parseExpression(ann.username(), String.class);
        if (StringUtils.hasText(username)) {
            params.add(new ParamInfo("username", username));
        }

        String password = context.parseExpression(ann.password(), String.class);
        if (StringUtils.hasText(password)) {
            params.add(new ParamInfo("password", password));
        }

        return params;
    }
}
