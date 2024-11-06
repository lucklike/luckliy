package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 永不熔断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 02:46
 */
public class NeverFuse implements FuseProtector {

    public static final FuseProtector INSTANCE = new NeverFuse();

    private NeverFuse() {
    }

    @Override
    public boolean fuseOrNot(MethodContext methodContext, Request request) {
        return false;
    }

    @Override
    public void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        // 不记录
    }

    @Override
    public void recordSuccess(MethodContext methodContext, Request request, long timeConsuming) {
        // 不记录
    }


}
