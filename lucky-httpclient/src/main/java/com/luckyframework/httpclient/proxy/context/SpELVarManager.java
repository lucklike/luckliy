package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import com.luckyframework.spel.ParamWrapper;

/**
 * SpEL变量管理
 */
public interface SpELVarManager {

    ParamWrapper getAnnotationParamWrapper(ContextParamWrapper cpw);
}
