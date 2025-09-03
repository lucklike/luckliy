package com.luckyframework.httpclient.proxy.typeparser;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutorException;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

public class AsyncMethodPackTypeParser implements PackTypeParser {

    private static final Logger log = LoggerFactory.getLogger(AsyncMethodPackTypeParser.class);

    @Override
    public boolean canHandle(MethodContext mc) {
        return mc.isAsyncMethod();
    }

    @Override
    public ResolvableType getRealType(ResolvableType packType) {
        return packType;
    }

    @Override
    public Object wrap(MethodContext mc, ResultSupplier supplier) throws Throwable {
        mc.getAsyncTaskExecutor().execute(() -> {
            try {
                supplier.get();
            } catch (Throwable e) {
                throw new AsyncTaskExecutorException("async task executor exception.", e).error(log);
            }
        });
        return null;
    }
}
