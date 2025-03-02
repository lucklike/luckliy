package com.luckyframework.httpclient.generalapi.chunk;

import java.util.function.Consumer;

/**
 * 结果集分类
 */
public class ResultClassify<R> {

    /**
     * 成功结果集
     */
    private final HandleResults<R> success;

    /**
     * 失败结果集
     */
    private final HandleResults<R> failure;

    ResultClassify(HandleResults<R> success, HandleResults<R> failure) {
        this.success = success;
        this.failure = failure;
    }

    public void successForEach(Consumer<HandleResult<R>> consumer) {
        getSuccess().forEach(consumer);
    }

    public void failureForEach(Consumer<HandleResult<R>> consumer) {
        getFailure().forEach(consumer);
    }

    public boolean isAllSuccess() {
        return failure.isEmpty() && !success.isEmpty();
    }

    public HandleResults<R> getSuccess() {
        return success;
    }

    public HandleResults<R> getFailure() {
        return failure;
    }
}
