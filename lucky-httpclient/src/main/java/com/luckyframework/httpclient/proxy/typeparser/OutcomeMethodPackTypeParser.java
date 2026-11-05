package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.common.Outcome;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 用于处理{@link Outcome}类型的包装类型解析器
 * <p>
 * 成功时把请求结果放入{@link Outcome#ok(Object)}，
 * 执行过程中抛出的异常（含HTTP异常、转换异常等）会被捕获并放入{@link Outcome#fail(Throwable)}，
 * 异常不会继续向外抛出，由调用方通过返回值自行决定如何处理失败信息。
 */
public class OutcomeMethodPackTypeParser extends SingleGenericPackTypeParser {

    @Override
    public boolean canHandle(MethodContext mc) {
        return Outcome.class.isAssignableFrom(mc.getReturnType());
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) {
        try {
            return Outcome.ok(supplier.get());
        } catch (Throwable e) {
            return Outcome.fail(e);
        }
    }
}
