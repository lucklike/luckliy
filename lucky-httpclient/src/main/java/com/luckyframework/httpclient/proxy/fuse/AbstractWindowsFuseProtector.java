package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Arrays;
import java.util.List;
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
    protected final Map<Object, Window<ResultEvaluate>> requestRecordMap = new ConcurrentHashMap<>(32);

    /**
     * 熔断时间集合
     */
    protected final Map<Object, Long> fuseTimeMap = new ConcurrentHashMap<>(32);

    /**
     * 窗体生成器
     */
    protected final Supplier<Window<ResultEvaluate>> windowSupplier;

    /**
     * ID生成器
     */
    protected final IdGenerator idGenerator;

    /**
     * 非正常返回的异常类型
     */
    protected final List<Class<? extends Throwable>> notNormalExceptionTypes;

    /**
     * 允许的最大请求时间，超过该时间的请求将会标记为【超时请求】
     */
    protected final long allowMaxReqTimeMillis;

    /**
     * 熔断时间（单位毫秒）
     */
    protected final long fuseTime;


    public AbstractWindowsFuseProtector(Supplier<Window<ResultEvaluate>> windowSupplier, IdGenerator idGenerator, Class<? extends Throwable>[] notNormalExceptionTypes, long allowMaxReqTimeMillis, int fuseTime) {
        this.windowSupplier = windowSupplier;
        this.idGenerator = idGenerator;
        this.notNormalExceptionTypes = Arrays.asList(notNormalExceptionTypes);
        this.allowMaxReqTimeMillis = allowMaxReqTimeMillis;
        this.fuseTime = fuseTime;
    }


    @Override
    public void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.computeIfAbsent(id, _id -> this.windowSupplier.get());
        if ((throwable instanceof HttpExecutorException)) {
            evaluateWindow.addElement(ResultEvaluate.FAILURE);
        } else if (ExceptionUtils.isAssignableFrom(notNormalExceptionTypes, throwable.getClass())) {
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
     *
     * @param evaluateWindow 窗体
     * @return 统计结果
     */
    protected ResultEvaluateCounter count(Window<ResultEvaluate> evaluateWindow) {
        ResultEvaluateCounter counter = new ResultEvaluateCounter(evaluateWindow.size());
        for (ResultEvaluate element : evaluateWindow.getElements()) {
            switch (element) {
                case ABNORMAL:
                    counter.addAbnormal();
                    break;
                case TIME_OUT:
                    counter.addTimeOut();
                    break;
                case FAILURE:
                    counter.addFailure();
                    break;
                case SUCCESS:
                    counter.addSuccess();
                    break;
            }
        }
        return counter;
    }

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

        @Override
        public String toString() {
            return "ResultEvaluateCounter{" +
                    "total=" + total +
                    ", success=" + success +
                    ", failure=" + failure +
                    ", abnormal=" + abnormal +
                    ", timeOut=" + timeOut +
                    '}';
        }
    }
}
