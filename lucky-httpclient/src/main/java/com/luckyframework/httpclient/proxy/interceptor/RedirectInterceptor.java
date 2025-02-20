package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;
import com.luckyframework.httpclient.proxy.annotations.RedirectProhibition;
import com.luckyframework.httpclient.proxy.spel.AddTempRespAndThrowVarSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;


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
     * 最大重定向次数
     */
    private int maxRedirectCount;

    /**
     * 重定向地址表达式
     */
    private String redirectLocationExp;

    /**
     * 是否缓存最终的重定向地址
     */
    private boolean cacheLocation = false;

    /**
     * 重定向地址
     */
    private String location;

    private final AtomicBoolean statusIsOk = new AtomicBoolean(false);
    private final AtomicBoolean conditionIsOk = new AtomicBoolean(false);
    private final AtomicBoolean locationIsOk = new AtomicBoolean(false);
    private final AtomicBoolean maxCountIsOk = new AtomicBoolean(false);
    private final AtomicBoolean cacheLocationIsOk = new AtomicBoolean(false);


    {
        redirectStatus = new Integer[]{301, 302, 303, 304, 307, 308};
        redirectLocationExp = "#{$respHeader$.Location}";
        maxRedirectCount = 5;
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

    public void setMaxRedirectCount(int maxRedirectCount) {
        this.maxRedirectCount = maxRedirectCount;
    }

    public void setCacheLocation(boolean cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

    public synchronized void setLocation(String location) {
        this.location = location;
    }

    public synchronized String getLocation() {
        return location;
    }

    public Integer[] getRedirectStatus(InterceptorContext context) {
        if (statusIsOk.compareAndSet(false, true)) {
            if (hasAutoRedirectAnnotation(context)) {
                int[] status = context.getMergedAnnotationCheckParent(AutoRedirect.class).status();
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
            if (hasAutoRedirectAnnotation(context)) {
                String condition = context.getMergedAnnotationCheckParent(AutoRedirect.class).condition();
                if (StringUtils.hasText(condition)) {
                    redirectCondition = condition;
                }
            }
        }
        return redirectCondition;
    }

    public String getRedirectLocationExp(InterceptorContext context) {
        if (locationIsOk.compareAndSet(false, true)) {
            if (hasAutoRedirectAnnotation(context)) {
                String location = context.getMergedAnnotationCheckParent(AutoRedirect.class).location();
                if (StringUtils.hasText(location)) {
                    redirectLocationExp = location;
                }
            }
        }
        return redirectLocationExp;
    }

    public boolean isCacheLocation(InterceptorContext context) {
        if (cacheLocationIsOk.compareAndSet(false, true)) {
            if (hasAutoRedirectAnnotation(context)) {
                this.cacheLocation = context.getMergedAnnotationCheckParent(AutoRedirect.class).cacheLocation();
            }
        }
        return cacheLocation;
    }

    public int getMaxRedirectCount(InterceptorContext context) {
        if (maxCountIsOk.compareAndSet(false, true)) {
            if (hasAutoRedirectAnnotation(context)) {
                maxRedirectCount = context.getMergedAnnotationCheckParent(AutoRedirect.class).maxCount();
            }
        }
        return maxRedirectCount;
    }

    @Override
    public void doBeforeExecute(Request request, InterceptorContext context) {
        String location = getLocation();
        if (isEnable(context) && isCacheLocation(context) && StringUtils.hasText(location)) {
            clearRepeatParams(request, location);
            log.info("Replace the request address with the cached redirect address: {} -> {}" , request.getUrl(), location);;
            ((DefaultRequest) request).setUrlTemplate(location);

        }
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        return doAfterExecuteCalculateCount(response, context, 1);
    }

    public Response doAfterExecuteCalculateCount(Response response, InterceptorContext context, int count) {
        if (isEnable(context) && isAllowRedirect(response, context)) {
            checkRedirectCount(context, count);
            String redirectLocation = getRedirectLocation(context, response);
            DefaultRequest request = (DefaultRequest) response.getRequest();
            if (count == 1) {
                recordRedirectUrl(context, request.getUrl());
            }

            clearRepeatParams(request, redirectLocation);
            log.info("Redirecting [{}] {} to {}", response.getStatus(), request.getUrl(), redirectLocation);
            recordRedirectUrl(context, redirectLocation);

            request.setUrlTemplate(redirectLocation);
            Response redirectResponse = doAfterExecuteCalculateCount(context.getContext().getHttpExecutor().execute(request), context, count + 1);
            setLocation(redirectLocation);
            return redirectResponse;
        }
        return response;
    }

    @SuppressWarnings("all")
    public void recordRedirectUrl(InterceptorContext context, String url) {
        List urlChain = (List) context.getContextVar().getRoot().computeIfAbsent($_REQUEST_REDIRECT_URL_CHAIN_$, _k -> new ArrayList<>());
        urlChain.add(url);
    }

    private void checkRedirectCount(InterceptorContext context, int count) {
        int maxCount = getMaxRedirectCount(context);
        if (maxCount > 0 && count > maxCount) {
            throw new RedirectException("The redirect is abnormal, and the number of redirects exceeds the maximum limit of {}.", maxRedirectCount);
        }
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
    private String getRedirectLocation(InterceptorContext context, Response response) {
        String redirectLocationExp = getRedirectLocationExp(context);
        String location = context.parseExpression(redirectLocationExp, String.class, new AddTempRespAndThrowVarSetter(response, context.getContext(), null));
        if (!StringUtils.hasText(location)) {
            throw new RedirectException("Redirection failed, invalid redirect address, expression: '" + redirectLocationExp + "', value: '" + location + "'").printException(log);
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
     * @param response 响应
     * @param context  拦截器注解上下文
     * @return 是否允许重定向
     */
    private boolean isAllowRedirect(Response response, InterceptorContext context) {
        Integer[] redirectStatus = getRedirectStatus(context);
        String redirectCondition = getRedirectCondition(context);
        boolean isRedirectStatus = ContainerUtils.isNotEmptyArray(redirectStatus) && ContainerUtils.inArrays(redirectStatus, response.getStatus());
        boolean isRedirectCondition = StringUtils.hasText(redirectCondition) && context.parseExpression(redirectCondition, boolean.class, new AddTempRespAndThrowVarSetter(response, context.getContext(), null));
        return (isRedirectStatus || isRedirectCondition);
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

    private boolean hasAutoRedirectAnnotation(InterceptorContext context) {
        return context.isAnnotatedCheckParent(AutoRedirect.class);
    }

    private boolean isEnable(InterceptorContext context) {
        if (!hasAutoRedirectAnnotation(context)) {
            return true;
        }
        return context.getMergedAnnotationCheckParent(AutoRedirect.class).enable();
    }
}
