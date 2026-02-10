package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 指定慢响应时间的处理器
 */
public abstract class AbstractSlowResponseHandler implements SlowResponseHandler {

    private long slowTime;

    public void setSlowTime(long slowTime) {
        this.slowTime = slowTime;
    }

    @Override
    public long getSlowTime(MethodContext context) {
        return this.slowTime;
    }
}
