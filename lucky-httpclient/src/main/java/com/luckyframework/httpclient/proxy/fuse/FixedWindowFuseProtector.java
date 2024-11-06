package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.function.Supplier;

/**
 * 基于固定窗口实现的统计熔断器
 */
public abstract class FixedWindowFuseProtector extends AbstractWindowsFuseProtector {

    public FixedWindowFuseProtector(Supplier<Window<ResultEvaluate>> windowSupplier, IdGenerator idGenerator, Class<? extends Throwable>[] notNormalExceptionTypes, long allowMaxReqTimeMillis, int fuseTime) {
        super(windowSupplier, idGenerator, notNormalExceptionTypes, allowMaxReqTimeMillis, fuseTime);
    }

    @Override
    public final boolean fuseOrNot(MethodContext methodContext, Request request) {
        // 获取窗口
        Object requestId = getRequestId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = getStatisticalWindow(requestId);

        // 窗口为空，说明是第一次请求 -> 放行
        if (evaluateWindow == null) {
            return false;
        }

        // 在熔断时间内 -> 熔断
        Long fuseDate = fuseTimeMap.get(requestId);
        if (fuseDate != null && fuseDate > System.currentTimeMillis()) {
            return true;
        }

        ResultEvaluateCounter counter = count(evaluateWindow);

        // 窗口还没满，使用实时计算结果来进行统计
        if (!evaluateWindow.isFull()) {
            boolean fuseOrNotByEveryOne = computingFuseOrNotByEveryOne(counter);
            if (fuseOrNotByEveryOne) {
                evaluateWindow.clear();
                fuseTimeMap.put(requestId, System.currentTimeMillis() + fuseTime);
                return true;
            }
            return false;
        }

        // 计算失败率并清空窗口，如果超过限制则熔断并记录/更新熔断时间
        boolean fuseOrNotByFull = computingFuseOrNotByFull(counter);
        evaluateWindow.clear();
        if (fuseOrNotByFull) {
            fuseTimeMap.put(requestId, System.currentTimeMillis() + fuseTime);
            return true;
        }
        return false;
    }

    /**
     * 计算是否需要进行熔断操作，窗口满的时候计算
     *
     * @param counter 统计结果
     * @return 是否需要进行熔断操作
     */
    protected abstract boolean computingFuseOrNotByFull(ResultEvaluateCounter counter);

    /**
     * 计算是否需要进行熔断操作，每次请求都计算
     *
     * @param counter 统计结果
     * @return 是否需要进行熔断操作
     */
    protected abstract boolean computingFuseOrNotByEveryOne(ResultEvaluateCounter counter);
}
