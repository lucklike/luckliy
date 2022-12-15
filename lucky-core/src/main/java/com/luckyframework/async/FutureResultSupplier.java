package com.luckyframework.async;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 12:58
 */
@FunctionalInterface
public interface FutureResultSupplier<T> {

    T getResult() throws Exception;
}
