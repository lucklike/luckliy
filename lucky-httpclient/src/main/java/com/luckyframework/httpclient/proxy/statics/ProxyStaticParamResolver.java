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
        String ip = context.getAnnotationAttribute(Proxy.ATTRIBUTE_IP, String.class);
        String port = context.getAnnotationAttribute(Proxy.ATTRIBUTE_PORT, String.class);
        java.net.Proxy.Type type = context.getAnnotationAttribute(Proxy.ATTRIBUTE_TYPE, java.net.Proxy.Type.class);

        String spELIp = String.valueOf(parseExpression(ip, context));
        int spELPort = Integer.parseInt(String.valueOf(parseExpression(port, context)).trim());
        return Collections.singletonList(new ParamInfo("proxy", new java.net.Proxy(type, new InetSocketAddress(spELIp, spELPort))));
    }
}
