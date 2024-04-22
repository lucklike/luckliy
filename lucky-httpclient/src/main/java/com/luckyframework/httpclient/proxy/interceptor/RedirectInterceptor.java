package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;
import com.luckyframework.httpclient.proxy.annotations.RedirectProhibition;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


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
    private Integer[] redirectStatus;

    /**
     * 重定向条件
     */
    private String redirectCondition;

    /**
     * 重定向地址表达式
     */
    private String redirectLocationExp;

    private final AtomicBoolean statusIsOk = new AtomicBoolean(false);
    private final AtomicBoolean conditionIsOk = new AtomicBoolean(false);
    private final AtomicBoolean locationIsOk = new AtomicBoolean(false);


    {
        redirectStatus = new Integer[]{301, 302, 303, 304, 307, 308};
        redirectLocationExp = "#{$respHeader$.Location}";
        redirectCondition = "";
    }

    public void setRedirectStatus(Integer[] redirectStatus) {
        this.redirectStatus = redirectStatus;
    }

    public void setRedirectCondition(String redirectCondition) {
        this.redirectCondition = redirectCondition;
    }

    public void setRedirectLocationExp(String redirectLocationExp) {
        this.redirectLocationExp = redirectLocationExp;
    }

    public Integer[] getRedirectStatus(InterceptorContext context) {
        if (statusIsOk.compareAndSet(false, true)) {
            if (context.notNullAnnotated()) {
                int[] status = context.toAnnotation(AutoRedirect.class).status();
                if (status != null && status.length > 0) {
                    redirectStatus = new Integer[status.length];
                    for (int i = 0; i < status.length; i++) {
                        redirectStatus[i] = status[i];
                    }
                }
            }
        }
        return redirectStatus;
    }

    public String getRedirectCondition(InterceptorContext context) {
        if (conditionIsOk.compareAndSet(false, true)) {
            if (context.notNullAnnotated()) {
                String condition = context.toAnnotation(AutoRedirect.class).condition();
                if (StringUtils.hasText(condition)) {
                    redirectCondition = condition;
                }
            }
        }
        return redirectCondition;
    }

    public String getRedirectLocationExp(InterceptorContext context) {
        if (locationIsOk.compareAndSet(false, true)) {
            if (context.notNullAnnotated()) {
                String location = context.toAnnotation(AutoRedirect.class).location();
                if (StringUtils.hasText(location)) {
                    redirectLocationExp = location;
                }
            }
        }
        return redirectLocationExp;
    }

    @Override
    public VoidResponse doAfterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        if (isAllowRedirect(voidResponse.getStatus(), context, voidResponse)) {
            String redirectLocation = getRedirectLocation(context, voidResponse);
            DefaultRequest request = (DefaultRequest) voidResponse.getRequest();
            clearRepeatParams(request, redirectLocation);
            log.info("Redirecting {} to {}", request.getUrl(), redirectLocation);
            request.setUrlTemplate(redirectLocation);
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
    public Response doAfterExecute(Response response, InterceptorContext context) {
        if (isAllowRedirect(response.getStatus(), context, response)) {
            String redirectLocation = getRedirectLocation(context, response);
            DefaultRequest request = (DefaultRequest) response.getRequest();
            clearRepeatParams(request, redirectLocation);
            log.info("Redirecting {} to {}", request.getUrl(), redirectLocation);
            request.setUrlTemplate(redirectLocation);
            return afterExecute(context.getContext().getHttpExecutor().execute(request), context);
        }
        return response;
    }

    @Override
    public Class<? extends Annotation> prohibition() {
        return RedirectProhibition.class;
    }

    /**
     * 获取重定向地址
     *
     * @param context  注解上下文
     * @param response 响应对象
     * @return 重定向地址
     */
    private String getRedirectLocation(InterceptorContext context, Object response) {
        String redirectLocationExp = getRedirectLocationExp(context);
        String location = context.parseExpression(redirectLocationExp, String.class, getContextParamSetter(response));
        if (!StringUtils.hasText(location)) {
            throw new HttpExecutorException("Redirection failed, invalid redirect address, expression: '" + redirectLocationExp + "', value: '" + location + "'").printException(log);
        }
        return location;
    }

    /**
     * 是否允许重定向
     * <pre>
     *      1.满足指定的重定向状态码
     *      2.满足重定向表达式
     *      3.代理方法上不存在{@link RedirectProhibition @RedirectProhibition}注解
     * </pre>
     *
     * @param status  状态码
     * @param context 拦截器注解上下文
     * @return 是否允许重定向
     */
    private boolean isAllowRedirect(int status, InterceptorContext context, Object response) {
        Integer[] redirectStatus = getRedirectStatus(context);
        String redirectCondition = getRedirectCondition(context);
        boolean isRedirectStatus = ContainerUtils.isNotEmptyArray(redirectStatus) && ContainerUtils.inArrays(redirectStatus, status);
        boolean isRedirectCondition = StringUtils.hasText(redirectCondition) && context.parseExpression(redirectCondition, boolean.class, getContextParamSetter(response));
        return (isRedirectStatus || isRedirectCondition);
    }


    private Consumer<ContextParamWrapper> getContextParamSetter(Object response) {
        if (response instanceof Response) {
            return cpw -> cpw.extractResponse((Response) response).extractRequest(((Response) response).getRequest());
        } else if (response instanceof VoidResponse) {
            return cpw -> cpw.extractVoidResponse((VoidResponse) response).extractRequest(((VoidResponse) response).getRequest());
        } else {
            return cpw -> {
            };
        }
    }

    private void clearRepeatParams(Request request, String location) {
        int index = location.indexOf("?");
        if (index > 0) {
            for (String kv : location.substring(index + 1).split("&")) {
                int _index = kv.indexOf("=");
                if (_index > 0) {
                    String key = kv.substring(0, _index).trim();
                    request.removerQueryParameter(key);
                }
            }
        }
    }
}
