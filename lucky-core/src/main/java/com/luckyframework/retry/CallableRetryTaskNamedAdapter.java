package com.luckyframework.retry;

import java.util.concurrent.Callable;

/**
 * 整合Callable接口和RetryTaskNamed的适配器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 00:35
 */
public interface CallableRetryTaskNamedAdapter<T> extends Callable<T>, RetryTaskNamed {

    static <T> CallableRetryTaskNamedAdapter<T> create(String taskName, Callable<T> callable){
        return new CallableRetryTaskNamedAdapter<T>() {
            @Override
            public String getTaskName() {
                return taskName;
            }

            @Override
            public T call() throws Exception {
                return callable.call();
            }
        };
    }
}
