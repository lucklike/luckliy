package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.httpclient.proxy.context.MethodMetaContext;

/**
 * Mock插件实现类
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class MockProxyPlugin implements ProxyPlugin {

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();
        MethodMetaContext metaContext = meta.getMetaContext();



        return null;
    }



    private boolean isApply(ExecuteMeta meta) {
        return false;
    }


}
