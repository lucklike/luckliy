package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;

public class FuseProtectorWrapper implements FuseProtector {

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
            fuseProtector = new FixedLengthFailureRatioFuseProtector(
                    fuseStrategyAnn.maxRespTime(),
                    fuseStrategyAnn.maxReqSize(),
                    fuseStrategyAnn.maxFailRatio(),
                    fuseStrategyAnn.fuseTimeSeconds(),
                    fuseStrategyAnn.notNormalExceptionType(),
                    methodContext.generateObject(fuseStrategyAnn.idGenerator(), "", Scope.SINGLETON)
            );
        }
        return fuseProtector;
    }
}
