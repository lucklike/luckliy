package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.common.FlatBean;
import com.luckyframework.httpclient.core.meta.FlatBeanBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

/**
 * {@link FlatBean}对象的参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class JavaFlatBeanParameterSetter implements ParameterSetter {

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        FlatBeanBodyObjectFactory.forJavaRequest(request, (FlatBean<?>) paramInfo.getValue());
    }
}
