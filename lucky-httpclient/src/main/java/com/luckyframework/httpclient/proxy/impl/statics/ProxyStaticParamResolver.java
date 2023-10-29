package com.luckyframework.httpclient.proxy.impl.statics;

import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.StaticParamResolver;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.net.Proxy;
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
    public List<ParamInfo> parser(MethodContext context, Annotation staticParamAnn) {
        String ip = context.getAnnotationAttribute(staticParamAnn, "ip", String.class);
        String port = context.getAnnotationAttribute(staticParamAnn, "port", String.class);
        Proxy.Type type = context.getAnnotationAttribute(staticParamAnn, "type", Proxy.Type.class);

        String spELIp = String.valueOf(parseExpression(ip, context, staticParamAnn));
        int spELPort = Integer.parseInt(String.valueOf(parseExpression(port, context, staticParamAnn)).trim());
        return Collections.singletonList(new ParamInfo("proxy", new Proxy(type, new InetSocketAddress(spELIp, spELPort))));
    }
}
