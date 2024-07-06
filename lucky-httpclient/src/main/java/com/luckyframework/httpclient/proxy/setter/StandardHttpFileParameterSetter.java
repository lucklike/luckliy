package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.Request;

/**
 * 标准的HTTP文件参数
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:51
 */
public class StandardHttpFileParameterSetter extends ValueNameParameterSetter {
    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.addHttpFiles(paramName, ((HttpFile[]) paramValue));
    }
}
