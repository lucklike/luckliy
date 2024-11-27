package com.luckyframework.httpclient.proxy.spel;

@FunctionalInterface
public interface HookHandler {

    /**
     * 钩子处理逻辑
     *
     * @param context       hook上下文对象
     * @param namespaceWrap 命名空间包装类
     */
    void handle(HookContext context, NamespaceWrap namespaceWrap);
}
