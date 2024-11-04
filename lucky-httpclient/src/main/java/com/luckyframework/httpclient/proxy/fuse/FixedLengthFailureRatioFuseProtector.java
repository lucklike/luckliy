package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.common.DateUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 计算固定请求次数中异常响应次数的比率来决定是否进行熔断
 */
public class FixedLengthFailureRatioFuseProtector implements FuseProtector {


    private final Map<Object, Window<ResultEvaluate>> requestRecordMap = new ConcurrentHashMap<>(32);
    private final Map<Object, Date> fuseTimeMap = new ConcurrentHashMap<>(32);


    /**
     * 最大请求时间，超过该时间则视为超时
     */
    private final long maxRespTime;

    /**
     * 统计的最大请求数量
     */
    private final int maxReqSize;

    /**
     * 允许的最大失败率，失败率超过该值将会被熔断
     */
    private final double maxFailRatio;

    /**
     * 熔断时间（单位秒）
     */
    private final int fuseTimeSeconds;

    /**
     * 非正常返回的异常类型
     */
    private final Class<? extends Throwable> notNormalExceptionType;

    /**
     * ID生成器
     */
    private final IdGenerator idGenerator;

    public FixedLengthFailureRatioFuseProtector(long maxRespTime, int maxReqSize, double maxFailRatio, int fuseTimeSeconds, Class<? extends Throwable> notNormalExceptionType, IdGenerator idGenerator) {
        this.maxRespTime = maxRespTime;
        this.maxReqSize = maxReqSize;
        this.maxFailRatio = maxFailRatio;
        this.fuseTimeSeconds = fuseTimeSeconds;
        this.notNormalExceptionType = notNormalExceptionType;
        this.idGenerator = idGenerator;
    }

    @Override
    public boolean fuseOrNot(MethodContext methodContext, Request request) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.get(id);
        if (evaluateWindow == null) {
            return false;
        }

        Date fuseDate = fuseTimeMap.get(id);
        if (fuseDate != null && fuseDate.after(new Date())) {
            return true;
        }

        if (evaluateWindow.isFull()) {
            double failureRate = computingFailureRate(evaluateWindow);
            evaluateWindow.clear();
            if (failureRate >= maxFailRatio) {
                fuseTimeMap.put(id, DateUtils.currAddDate(Calendar.SECOND, fuseTimeSeconds));
                return true;
            }
        }
        return false;
    }

    @Override
    public void recordFailure(MethodContext methodContext, Request request, Throwable throwable) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.computeIfAbsent(id, _id -> new LengthWindow<>(maxReqSize));
        if ((throwable instanceof HttpExecutorException)) {
            evaluateWindow.addElement(ResultEvaluate.FAILURE);
        } else if (notNormalExceptionType.isAssignableFrom(throwable.getClass())) {
            evaluateWindow.addElement(ResultEvaluate.EXCEPTION_RESPONSE);
        }
    }

    @Override
    public void recordSuccess(MethodContext methodContext, Request request, long timeConsuming) {
        Object id = idGenerator.generateId(methodContext, request);
        Window<ResultEvaluate> evaluateWindow = requestRecordMap.computeIfAbsent(id, _id -> new LengthWindow<>(maxReqSize));
        if (timeConsuming >= maxRespTime) {
            evaluateWindow.addElement(ResultEvaluate.TIME_OUT);
        } else {
            evaluateWindow.addElement(ResultEvaluate.SUCCESS);
        }
    }

    private double computingFailureRate(Window<ResultEvaluate> evaluateWindow) {
        long total = 0L, fail = 0L;
        for (ResultEvaluate element : evaluateWindow.getElements()) {
            total++;
            if (element != ResultEvaluate.SUCCESS) {
                fail++;
            }
        }
        return (double) fail / (double) total;
    }

}
