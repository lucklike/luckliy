package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.common.FlatBean;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 用于处理{@link FlatBean}类型的包装类型解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/27 02:27
 */
public class FlatBeanMethodPackTypeParser extends SingleGenericPackTypeParser {

    @Override
    public boolean canHandle(MethodContext mc) {
        return FlatBean.class.isAssignableFrom(mc.getReturnType());
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        return FlatBean.of(supplier.get());
    }
}
