package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.BasicAuth;

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
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        BasicAuth basicAuth = context.toAnnotation(BasicAuth.class);
        String username = basicAuth.username();
        String password = basicAuth.password();
        return Collections.singletonList(new ParamInfo(context.parseExpression(username), context.parseExpression(password)));
    }
}
