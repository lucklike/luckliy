package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.Request;

/**
 * InputStream参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class InputStreamParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        HttpFile[] httpFiles = (HttpFile[]) paramValue;
        request.addHttpFiles(paramName, httpFiles);
    }
}
