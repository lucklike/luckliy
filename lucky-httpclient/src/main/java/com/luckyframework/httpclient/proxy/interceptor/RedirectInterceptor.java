package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.HttpHeaders;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.proxy.annotations.RedirectProhibition;
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

    /**
     * 需要重定向的状态码
     */
    private int[] redirectStatus;

    /**
     * 重定向条件
     */
    private String redirectCondition;

    /**
     * 重定向地址表达式
     */
    private String redirectLocationExp;

    {
        redirectStatus = new int[0];
        redirectLocationExp = "#{$respHeader$.Location}";
    }

    @Override
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        if (isAllowRedirect(voidResponse.getStatus(), context)) {
            String newUrl = getRedirectLocation(voidResponse.getSimpleHeaders().get(HttpHeaders.LOCATION));
            DefaultRequest request = (DefaultRequest) voidResponse.getRequest();
            log.info("Redirecting {} to {}", request.getUrl(), newUrl);
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
        if (isAllowRedirect(response.getStatus(), context)) {
            String newUrl = getRedirectLocation(response.getSimpleHeaders().get(HttpHeaders.LOCATION));
            DefaultRequest request = (DefaultRequest) response.getRequest();
            log.info("Redirecting {} to {}", request.getUrl(), newUrl);
            request.setUrlTemplate(newUrl);
            return afterExecute(context.getContext().getHttpExecutor().execute(request), context);
        }
        return response;
    }

    /**
     * 获取重定向地址，获取不到时会抛{@link HttpExecutorException}异常
     *
     * @param location 重定向地址
     * @return 字符串类型的重定向地址
     */
    private String getRedirectLocation(Object location) {
        if (location == null) {
            throw new HttpExecutorException("The redirect failed. No 'Location' attribute was found in the response header").printException(log);
        }
        return String.valueOf(location);
    }

    /**
     * 是否允许重定向
     * <pre>
     *      1.状态码为<b>301<b/>、<b>302<b/>;
     *      2.代理方法上不存在{@link RedirectProhibition @RedirectProhibition}注解
     * </pre>
     *
     * @param status  状态码
     * @param context 拦截器注解上下文
     * @return 是否允许重定向
     */
    private boolean isAllowRedirect(int status, InterceptorContext context) {
        return (status == 302 || status == 301) && (!context.isAnnotated(RedirectProhibition.class));
    }
}
