package com.luckyframework.threadpool;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * {@link Runnable}阻塞队列工厂，用于创建一个{@link Runnable}阻塞队列
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:06
 */
@FunctionalInterface
public interface RunnableBlockingQueueFactory {

    RunnableBlockingQueueFactory DEFAULT_INSTANCE = new DefaultRunnableBlockingQueueFactory();

    BlockingDeque<Runnable> create(int blockingQueueSize);

    class DefaultRunnableBlockingQueueFactory implements RunnableBlockingQueueFactory{

        @Override
        public BlockingDeque<Runnable> create(int blockingQueueSize) {
            return new LinkedBlockingDeque<>(blockingQueueSize);
        }
    }
}
