package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class TimeWindowStatisticsFuseProtector implements FuseProtector {

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
            TimeWindowFuseStrategy fuseStrategyAnn = methodContext.getMergedAnnotationCheckParent(TimeWindowFuseStrategy.class);
            fuseProtector = new SlidingWindowFuseProtector(
                    () -> new TimeWindow<>(fuseStrategyAnn.timeInterval() * 1000L),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), Scope.SINGLETON),
                    fuseStrategyAnn.notNormalExceptionTypes(),
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.fuseTime() * 1000,
                    fuseStrategyAnn.slideUnit() * 1000L) {
                @Override
                protected boolean computingFuseOrNotByFull(ResultEvaluateCounter counter) {

                    // 请求数量不够 -> 放行
                    int total = counter.getTotal();
                    if (total < fuseStrategyAnn.minReqSize()) {
                        return false;
                    }

                    // 计算失败比例
                    int failure = counter.getFailure();
                    double failureRatio = (double) failure / (double) total;
                    if (failureRatio > fuseStrategyAnn.maxFailRatio()) {
                        return true;
                    }

                    // 计算超时比例
                    int timeOut = counter.getTimeOut();
                    double timeoutRatio = (double) timeOut / (double) total;
                    return timeoutRatio > fuseStrategyAnn.maxTimeoutRatio();
                }

                @Override
                protected boolean computingFuseOrNotByEveryOne(ResultEvaluateCounter counter) {
                    int failure = counter.getFailure();
                    if (failure > fuseStrategyAnn.maxFailCount()) {
                        return true;
                    }
                    int timeOut = counter.getTimeOut();
                    return timeOut > fuseStrategyAnn.maxTimeoutCount();
                }

                @Override
                protected long afterFuseSlideForwardOffset() {
                    return fuseTime;
                }
            };
        }
        return fuseProtector;
    }
}
