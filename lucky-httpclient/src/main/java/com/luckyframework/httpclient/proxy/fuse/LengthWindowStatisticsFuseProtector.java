package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class LengthWindowStatisticsFuseProtector implements FuseProtector {

    private FuseProtector fuseProtector;

    @Override
    public synchronized boolean fuseOrNot(MethodContext methodContext, Request request) {
        return getFuseProtector(methodContext).fuseOrNot(methodContext, request);
    }

    @Override
    public synchronized void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        getFuseProtector(methodContext).recordFailure(methodContext, request, throwable);
    }

    @Override
    public synchronized void recordSuccess(MethodContext methodContext, Request request, long timeConsuming) {
        getFuseProtector(methodContext).recordSuccess(methodContext, request, timeConsuming);
    }

    private FuseProtector getFuseProtector(MethodContext methodContext) {
        if (fuseProtector == null) {
            LengthWindowFuseStrategy fuseStrategyAnn = methodContext.getMergedAnnotationCheckParent(LengthWindowFuseStrategy.class);
            fuseProtector = new SlidingWindowFuseProtector(
                    () -> new LengthWindow<>(fuseStrategyAnn.maxReqSize()),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), Scope.SINGLETON),
                    fuseStrategyAnn.notNormalExceptionTypes(),
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.fuseTime() * 1000,
                    fuseStrategyAnn.slideUnit())
            {
                @Override
                protected boolean computingFuseOrNotByFull(ResultEvaluateCounter counter) {
                    int total = counter.getTotal();
                    int failure = counter.getFailure();
                    int timeOut = counter.getTimeOut();
                    // 超时比例
                    double timeoutRatio = (double) timeOut / (double) total;
                    // 失败比例
                    double failureRatio = (double) failure / (double) total;
                    return failureRatio > fuseStrategyAnn.maxFailRatio() || timeoutRatio > fuseStrategyAnn.maxTimeoutRatio();
                }

                @Override
                protected boolean computingFuseOrNotByEveryOne(ResultEvaluateCounter counter) {
                    return false;
                }
            };
        }
        return fuseProtector;
    }
}
