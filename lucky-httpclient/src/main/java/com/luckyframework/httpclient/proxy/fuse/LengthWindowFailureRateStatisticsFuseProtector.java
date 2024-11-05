package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class LengthWindowFailureRateStatisticsFuseProtector implements FuseProtector {

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
            fuseProtector = new WindowsFuseProtector(
                    () -> new LengthWindow<>(fuseStrategyAnn.maxReqSize()),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), Scope.SINGLETON),
                    fuseStrategyAnn.notNormalExceptionTypes(),
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.fuseTimeSeconds()
            ) {
                @Override
                protected boolean computingFuseOrNot(ResultEvaluateCounter counter) {
                    int total = counter.getTotal();
                    int success = counter.getSuccess();
                    int nonSuccess = total-success;
                    return ((double) nonSuccess / (double)total) > fuseStrategyAnn.maxFailRatio();
                }
            };
        }
        return fuseProtector;
    }
}
