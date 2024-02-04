package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.HttpHeaders;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;


/**
 * 重定向拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:14
 */
public class RedirectInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(RedirectInterceptor.class);

    @Override
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        int status = voidResponse.getStatus();
        if (status == 302 || status == 301) {
            Object location = voidResponse.getSimpleHeaders().get(HttpHeaders.LOCATION);
            if (location == null) {
                throw new HttpExecutorException("The redirect failed. No 'Location' attribute was found in the response header").printException(log);
            }
            String newUrl = String.valueOf(location);
            log.info("Redirecting to {}", newUrl);
            DefaultRequest request = (DefaultRequest) voidResponse.getRequest();
            request.setUrlTemplate(newUrl);
            final AtomicReference<ResponseMetaData> meta = new AtomicReference<>();
            context.getContext().getHttpExecutor().execute(request, md -> {
                meta.set(md);
                responseProcessor.process(md);
            });
            return afterExecute(new VoidResponse(meta.get()), responseProcessor, context);
        }
        return voidResponse;
    }

    @Override
    public Response afterExecute(Response response, InterceptorContext context) {
        int status = response.getStatus();
        if (status == 302 || status == 301) {
            Object location = response.getSimpleHeaders().get(HttpHeaders.LOCATION);
            if (location == null) {
                throw new HttpExecutorException("The redirect failed. No 'Location' attribute was found in the response header").printException(log);
            }
            String newUrl = String.valueOf(location);
            log.info("Redirecting to {}", newUrl);
            DefaultRequest request = (DefaultRequest) response.getRequest();
            request.setUrlTemplate(newUrl);
            return afterExecute(context.getContext().getHttpExecutor().execute(request), context);
        }
        return response;
    }
}
