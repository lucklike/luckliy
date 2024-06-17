package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.httpclient.proxy.annotations.UseProxy;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 代理配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class ProxyStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        UseProxy useProxy = context.toAnnotation(UseProxy.class);

        String ip = context.parseExpression(useProxy.ip());
        int port = context.parseExpression(useProxy.port(), int.class);
        String username = context.parseExpression(useProxy.username());
        String password = context.parseExpression(useProxy.password());

        return Collections.singletonList(
                new ParamInfo(
                        "proxyInfo",
                        new ProxyInfo()
                                .setProxy(useProxy.type(), ip, port)
                                .setUsername(username)
                                .setPassword(password)
                )
        );
    }
}
