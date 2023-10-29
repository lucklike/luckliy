package com.luckyframework.httpclient.proxy.impl.statics;

import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.StaticParamResolver;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Basic Auth配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class BasicAuthStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(MethodContext context, Annotation staticParamAnn) {
        String username = context.getAnnotationAttribute(staticParamAnn, "username", String.class);
        String password = context.getAnnotationAttribute(staticParamAnn, "password", String.class);
        return Collections.singletonList(new ParamInfo(parseExpression(username, context, staticParamAnn), parseExpression(password, context, staticParamAnn)));
    }
}
