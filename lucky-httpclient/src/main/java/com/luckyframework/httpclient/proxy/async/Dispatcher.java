package com.luckyframework.httpclient.proxy.async;

import java.util.concurrent.Executor;

/**
 * 调度器
 */
public class Dispatcher {

    /**
     * 并发模型
     */
    private Model model;

    /**
     * 异步执行器
     */
    private Executor executor;

    /**
     * 并发控制
     */
    private Integer concurrency;

    public Model getModel() {
        return model;
    }

    public Dispatcher setModel(Model model) {
        this.model = model;
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Dispatcher setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public Dispatcher setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
        return this;
    }
}
