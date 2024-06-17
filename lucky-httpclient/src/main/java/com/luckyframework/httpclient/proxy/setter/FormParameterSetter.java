package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;

/**
 * 表单参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class FormParameterSetter extends ValueNameParameterSetter {


    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.addFormParameter(paramName, paramValue);
    }
}
