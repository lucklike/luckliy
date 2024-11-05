package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class LengthWindowStatisticsFuseProtector implements FuseProtector {

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
            FixedQuantityFuseStrategy fuseStrategyAnn = methodContext.getMergedAnnotationCheckParent(FixedQuantityFuseStrategy.class);
            fuseProtector = new AbstractWindowsFuseProtector(
                    () -> new LengthWindow<>(fuseStrategyAnn.maxReqSize()),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), Scope.SINGLETON),
                    fuseStrategyAnn.notNormalExceptionTypes(),
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.fuseTime()
            ) {
                @Override
                protected boolean computingFuseOrNot(ResultEvaluateCounter counter) {
                    int total = counter.getTotal();
                    int failure = counter.getFailure();
                    int timeOut = counter.getTimeOut();
                    // 超时比例
                    double timeoutRatio = (double) timeOut / (double) total;
                    // 失败比例
                    double failureRatio = (double) failure / (double) total;
                    return failureRatio > fuseStrategyAnn.maxFailRatio() || timeoutRatio > fuseStrategyAnn.maxTimeoutRatio();
                }
            };
        }
        return fuseProtector;
    }
}
