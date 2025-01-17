package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.destroy.DestroyContext;
import com.luckyframework.httpclient.proxy.destroy.DestroyHandle;
import com.luckyframework.io.StorageMediumStream;

import java.io.Closeable;

/**
 * 可重复读取流销毁处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 01:58
 */
public class RepeatableReadStreamDestroyHandle implements DestroyHandle {

    /**
     * 释放参数列中的资源
     * <pre>
     *     1.检测参数列表中是否存在{@link StorageMediumStream}类型的参数，如果有则尝试释放资源
     *     2.检测参数列表中是否存在{@link Closeable}类型的参数，如果有则尝试释放资源
     * </pre>
     */
    @Override
    public void destroy(DestroyContext context) {
        MethodContext methodContext = context.getContext();
        for (ParameterContext parameterContext : methodContext.getParameterContexts()) {
            Object value = parameterContext.getValue();
            RepeatableReadStreamFunction.releaseByObject(value);
        }
    }


}
