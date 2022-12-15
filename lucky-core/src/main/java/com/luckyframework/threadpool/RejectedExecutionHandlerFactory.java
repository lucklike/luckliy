package com.luckyframework.threadpool;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝策略工厂，用于创建一个拒绝策略
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:10
 */
public interface RejectedExecutionHandlerFactory {

    RejectedExecutionHandlerFactory DEFAULT_INSTANCE = new DefaultRejectedExecutionHandlerFactory();

    RejectedExecutionHandler create();

    class DefaultRejectedExecutionHandlerFactory implements RejectedExecutionHandlerFactory{

        @Override
        public RejectedExecutionHandler create() {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    }
}
