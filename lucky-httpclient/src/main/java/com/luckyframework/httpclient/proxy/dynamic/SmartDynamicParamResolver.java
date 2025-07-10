package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.context.ValueContext;

public interface SmartDynamicParamResolver extends DynamicParamResolver {

    boolean canResolve(ValueContext context);

}
