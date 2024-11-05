package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 基于窗口统计的熔断器
 */
public abstract class AbstractWindowsFuseProtector implements FuseProtector {

    /**
     * 窗口集合
     */
    private final Map<Object, Window<ResultEvaluate>> requestRecordMap = new ConcurrentHashMap<>(32);

    /**
     * 熔断时间集合
     */
    private final Map<Object, Long> fuseTimeMap = new ConcurrentHashMap<>(32);

    /**
     * 窗体生成器
     */
    private final Supplier<Window<ResultEvaluate>> windowSupplier;

    /**
     * ID生成器
     */
    private final IdGenerator idGenerator;

    /**
     * 非正常返回的异常类型
     */
    private final Class<? extends Throwable>[] notNormalExceptionTypes;

    /**
     * 允许的最大请求时间，超过该时间的请求将会标记为【超时请求】
     */
    private final long allowMaxReqTimeMillis;

    /**
     * 熔断时间（单位秒）
     */
    private final long fuseTime;


    public AbstractWindowsFuseProtector(Supplier<Window<ResultEvaluate>> windowSupplier, IdGenerator idGenerator, Class<? extends Throwable>[] notNormalExceptionTypes, long allowMaxReqTimeMillis, int fuseTimeSeconds) {
        this.windowSupplier = windowSupplier;
        this.idGenerator = idGenerator;
        this.notNormalExceptionTypes = notNormalExceptionTypes;
        this.allowMaxReqTimeMillis = allowMaxReqTimeMillis;
        this.fuseTime = fuseTimeSeconds * 1000L;
    }

    @Override
    public boolean fuseOrNot(MethodContext methodContext, Request request) {
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

        // 窗口还没满，还未达到计算条件 -> 放行
        if (!evaluateWindow.isFull()) {
            return false;
        }

        // 计算失败率并清空窗口，如果超过限制则熔断并记录/更新熔断时间
        boolean fuseOrNot = computingFuseOrNot(count(evaluateWindow));
        evaluateWindow.clear();
        if (fuseOrNot) {
            fuseTimeMap.put(requestId, System.currentTimeMillis() + fuseTime);
            return true;
        }
        return false;
    }


    @Override
    public void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.computeIfAbsent(id, _id -> this.windowSupplier.get());
        if ((throwable instanceof HttpExecutorException)) {
            evaluateWindow.addElement(ResultEvaluate.FAILURE);
        } else if (ExceptionUtils.isAssignableFrom(Arrays.asList(notNormalExceptionTypes), throwable.getClass())) {
            evaluateWindow.addElement(ResultEvaluate.ABNORMAL);
        }
    }

    @Override
    public void recordSuccess(MethodContext methodContext, Request request, long timeConsuming) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.computeIfAbsent(id, _id -> this.windowSupplier.get());
        if (timeConsuming >= allowMaxReqTimeMillis) {
            evaluateWindow.addElement(ResultEvaluate.TIME_OUT);
        } else {
            evaluateWindow.addElement(ResultEvaluate.SUCCESS);
        }
    }

    /**
     * 获取某个请求的ID值
     *
     * @param methodContext 当前方法上下文对象
     * @param request       当前请求对象
     * @return ID值
     */
    protected Object getRequestId(MethodContext methodContext, Request request) {
        return idGenerator.generateId(methodContext, request);
    }

    /**
     * 获取个请求的统计窗口
     *
     * @param requestId 请求ID
     * @return 统计窗口
     */
    protected Window<ResultEvaluate> getStatisticalWindow(Object requestId) {
        return requestRecordMap.get(requestId);
    }

    /**
     * 获取窗体中所有类型的请求结果数量
     * @param evaluateWindow 窗体
     * @return 统计结果
     */
    protected ResultEvaluateCounter count(Window<ResultEvaluate> evaluateWindow) {
        ResultEvaluateCounter counter = new ResultEvaluateCounter(evaluateWindow.size());
        for (ResultEvaluate element : evaluateWindow.getElements()) {
            switch (element){
                case ABNORMAL: counter.addAbnormal(); break;
                case TIME_OUT: counter.addTimeOut(); break;
                case FAILURE: counter.addFailure(); break;
                case SUCCESS: counter.addSuccess(); break;
            }
        }
        return counter;
    }


    /**
     * 计算是否需要进行熔断操作
     * @param counter 统计结果
     * @return 是否需要进行熔断操作
     */
    protected abstract boolean computingFuseOrNot(ResultEvaluateCounter counter);

    /**
     * 统计结果的包装对象
     */
    public static class ResultEvaluateCounter {

        private final int total;
        private int success = 0;
        private int failure = 0;
        private int abnormal = 0;
        private int timeOut = 0;

        ResultEvaluateCounter(int total) {
            this.total = total;
        }

        public void addSuccess() {
            this.success += 1;
        }

        public void addFailure() {
            this.failure += 1;
        }

        public void addAbnormal() {
            this.abnormal += 1;
        }

        public void addTimeOut() {
            this.timeOut += 1;
        }

        public int getTotal() {
            return total;
        }

        public int getSuccess() {
            return success;
        }

        public int getFailure() {
            return failure;
        }

        public int getAbnormal() {
            return abnormal;
        }

        public int getTimeOut() {
            return timeOut;
        }
    }
}
