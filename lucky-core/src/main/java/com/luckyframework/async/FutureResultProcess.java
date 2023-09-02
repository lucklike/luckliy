package com.luckyframework.async;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:18
 */
@FunctionalInterface
public interface FutureResultProcess<T> {

    void resultProcess(T result) throws Exception;

}
