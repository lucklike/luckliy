package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.Request;

/**
 * UserInfo参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/7 21:44
 */
public class UserInfoParameterSetter extends ValueNameParameterSetter {
    @Override
    protected void doSet(Request request, String paramName, Object paramValue) {
        request.setUserInfo(String.valueOf(paramValue));
    }
}
