package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.describe.ApiDescribe;
import com.luckyframework.httpclient.generalapi.describe.Describe;
import com.luckyframework.httpclient.generalapi.describe.DescribeFunction;

/**
 * 错误状态处理器拦截器
 */
public interface ErrStatusHandleInterceptor extends Interceptor {

    @Override
    default Response doAfterExecute(Response response, InterceptorContext context) {
        if (isErrStatus(response.getStatus())) {
            handleErrStatus(response, context);
        }
        else if (isErrRespCode(response, context)) {
            handleErrRespCode(response, context);
        }
        return response;
    }

    @Override
    default int priority() {
        return PriorityConstant.ERR_STATUS_HANDLE_PRIORITY;
    }

    /**
     * 获取Api描述信息
     * @param context 上下文对象
     * @return Api描述信息
     */
    default ApiDescribe getApiDescribe(InterceptorContext context) {
        return DescribeFunction.describe(context.getContext());
    }

    /**
     * 判断状态码是否异常
     *
     * @param status 状态码
     * @return 是否为错误状态码
     */
    boolean isErrStatus(int status);

    /**
     * 出现错误状态码时的处理逻辑
     *
     * @param response 响应对象
     * @param context  注解上下文对象
     */
    void handleErrStatus(Response response, InterceptorContext context);

    /**
     * 判断响应码码是否异常
     *
     * @param response 响应对象
     * @param context  注解上下文对象
     * @return 是否为错误的响应码
     */
    boolean isErrRespCode(Response response, InterceptorContext context);

    /**
     * 出现错误响应码时的处理逻辑
     *
     * @param response 响应对象
     * @param context  注解上下文对象
     */
    void handleErrRespCode(Response response, InterceptorContext context);
}
