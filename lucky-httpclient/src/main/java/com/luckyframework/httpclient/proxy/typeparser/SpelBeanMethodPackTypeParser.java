package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpelBean;

/**
 * 用于处理{@link SpelBean}类型的包装类型解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/30 17:52
 */
public class SpelBeanMethodPackTypeParser extends SingleGenericPackTypeParser {

    @Override
    public boolean canHandle(MethodContext mc) {
        return SpelBean.class.isAssignableFrom(mc.getReturnType());
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        return SpelBean.of(mc, supplier.get());
    }
}
