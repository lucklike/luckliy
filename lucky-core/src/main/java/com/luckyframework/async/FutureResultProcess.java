package com.luckyframework.async;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 12:39
 */
@FunctionalInterface
public interface FutureResultProcess<T> {

    void resultProcess(FutureResultSupplier<T> supplier);

}
