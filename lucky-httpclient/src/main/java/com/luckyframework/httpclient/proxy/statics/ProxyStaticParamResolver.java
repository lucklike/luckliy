package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.Proxy;

import java.net.InetSocketAddress;
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
        Proxy proxy = context.toAnnotation(Proxy.class);
        String spELIp = String.valueOf(parseExpression(proxy.ip(), context));
        int spELPort = Integer.parseInt(String.valueOf(parseExpression(proxy.port(), context)).trim());
        return Collections.singletonList(new ParamInfo("proxy", new java.net.Proxy(proxy.type(), new InetSocketAddress(spELIp, spELPort))));
    }
}
