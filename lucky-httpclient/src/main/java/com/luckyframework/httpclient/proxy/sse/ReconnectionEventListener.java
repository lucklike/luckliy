package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.ContextAware;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 支持重连的事件监听器
 */
public abstract class ReconnectionEventListener implements EventListener, ContextAware {

    /**
     * 方法上下文
     */
    private MethodContext context;

    @Override
    public void setContext(Context context) {
        this.context = (MethodContext) context;
    }

    /**
     * 获取方法上下文
     *
     * @return 方法上下文
     */
    public MethodContext getContext() {
        return context;
    }

    /**
     * 执行重新连接
     */
    public void reconnection() {
        context.invokeCurrentMethod();
    }

    /**
     * 执行重新连接
     *
     * @param runBefore 运行之前需要执行的逻辑
     */
    public void reconnection(Runnable runBefore) {
        runBefore.run();
        reconnection();
    }


    /**
     * 异步重连
     */
    public void asyncReconnection() {
        context.getAsyncTaskExecutor().execute(this::reconnection);
    }

    /**
     * 异步重连
     *
     * @param runBefore 运行之前需要执行的逻辑
     */
    public void asyncReconnection(Runnable runBefore) {
        context.getAsyncTaskExecutor().execute(() -> reconnection(runBefore));
    }
}
