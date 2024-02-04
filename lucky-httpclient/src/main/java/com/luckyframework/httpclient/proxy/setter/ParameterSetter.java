package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.ParamInfo;

/**
 * 参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:21
 */
@FunctionalInterface
public interface ParameterSetter {

    /**
     * 请求参数设置
     *
     * @param request    请求体
     * @param paramInfo  参数信息
     */
    void set(Request request, ParamInfo paramInfo);
}
