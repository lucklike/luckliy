package com.luckyframework.threadpool;

import org.springframework.lang.NonNull;
import org.springframework.util.CustomizableThreadCreator;

import java.util.concurrent.ThreadFactory;

/**
 * 可以自定义线程名称的线程工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/20 07:16
 */
public class NamedThreadFactory extends CustomizableThreadCreator implements ThreadFactory {
    public NamedThreadFactory() {
    }

    public NamedThreadFactory(String threadNamePrefix) {
        super(threadNamePrefix);
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        return createThread(runnable);
    }
}
