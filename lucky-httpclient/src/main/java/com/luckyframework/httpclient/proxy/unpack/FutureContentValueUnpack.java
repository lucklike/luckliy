package com.luckyframework.httpclient.proxy.unpack;

import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Future参数的参数拆包器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/11/30 03:50
 */
public class FutureContentValueUnpack implements ContextValueUnpack {

    @Override
    public Object getRealValue(Object wrapperValue, Annotation unpackAnn) throws ContextValueUnpackException {
        if (wrapperValue instanceof Future) {
            return getFutureValue((Future<?>) wrapperValue);
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
}
