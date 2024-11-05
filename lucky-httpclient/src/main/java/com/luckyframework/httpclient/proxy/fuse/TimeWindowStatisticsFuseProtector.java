package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class TimeWindowStatisticsFuseProtector implements FuseProtector {

    private FuseProtector fuseProtector;

    @Override
    public boolean fuseOrNot(MethodContext methodContext, Request request) {
        return getFuseProtector(methodContext).fuseOrNot(methodContext, request);
    }

    @Override
    public void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        getFuseProtector(methodContext).recordFailure(methodContext, request, throwable);
    }

    @Override
    public void recordSuccess(MethodContext methodContext, Request request, long timeConsuming) {
        getFuseProtector(methodContext).recordSuccess(methodContext, request, timeConsuming);
    }

    private FuseProtector getFuseProtector(MethodContext methodContext) {
        if (fuseProtector == null) {
            FixedTimeFuseStrategy fuseStrategyAnn = methodContext.getMergedAnnotationCheckParent(FixedTimeFuseStrategy.class);
            fuseProtector = new AbstractWindowsFuseProtector(
                    () -> new TimeWindow<>(fuseStrategyAnn.timeInterval()),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), Scope.SINGLETON),
                    fuseStrategyAnn.notNormalExceptionTypes(),
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.fuseTime()
            ) {
                @Override
                protected boolean computingFuseOrNot(ResultEvaluateCounter counter) {

                    // 请求数量不够 -> 放行
                    int total = counter.getTotal();
                    if (total < fuseStrategyAnn.minReqSize()) {
                        return false;
                    }

                    // 计算失败数量
                    int failure = counter.getFailure();
                    if (failure > fuseStrategyAnn.maxFailCount()) {
                        return true;
                    }

                    // 计算超时数量
                    int timeOut = counter.getTimeOut();
                    if (timeOut > fuseStrategyAnn.maxTimeoutCount()) {
                        return true;
                    }

                    // 计算失败比例
                    double failureRatio = (double) failure / (double) total;
                    if (failureRatio > fuseStrategyAnn.maxFailRatio()) {
                        return true;
                    }

                    // 计算超时比例
                    double timeoutRatio = (double) timeOut / (double) total;
                    return timeoutRatio > fuseStrategyAnn.maxTimeoutRatio();
                }
            };
        }
        return fuseProtector;
    }
}
