package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.spel.LazyValue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 *工厂参数的参数拆包器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/11/30 03:50
 */
public class FactoryObjectContentValueUnpack implements ContextValueUnpack {

    @Override
    public Object getRealValue(ValueUnpackContext unpackContext, Object wrapperValue) throws ContextValueUnpackException {
        if (wrapperValue instanceof Factory) {
            return ((Factory<?>) wrapperValue).create();
        }
        if (wrapperValue instanceof LazyValue) {
            return ((LazyValue<?>) wrapperValue).getValue();
        }
        if (wrapperValue instanceof Future) {
            return getFutureValue((Future<?>) wrapperValue);
        }
        if (wrapperValue instanceof Supplier) {
            return ((Supplier<?>) wrapperValue).get();
        }
        if (wrapperValue instanceof Callable) {
            return getCallableValue(((Callable<?>) wrapperValue));
        }
        return wrapperValue;
    }

    private Object getFutureValue(Future<?> futureValue) {
        try {
            return futureValue.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ContextValueUnpackException("The future value unpack is running abnormally, could not get the result from future.", e);
        }
    }

    private Object getCallableValue(Callable<?> futureValue) {
        try {
            return futureValue.call();
        } catch (Exception e) {
            throw new ContextValueUnpackException("The callable value unpack is running abnormally, could not get the result from callable.", e);
        }
    }
}
