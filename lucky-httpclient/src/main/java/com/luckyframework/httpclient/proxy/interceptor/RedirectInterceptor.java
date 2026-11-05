package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;
import com.luckyframework.httpclient.proxy.annotations.RedirectProhibition;
import com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent;
import com.luckyframework.httpclient.proxy.spel.AddTempRespAndThrowVarSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_REDIRECT_URL_CHAIN_$;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RESPONSE_TIME_SPENT$_;


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
     * 重定向地址缓存的最大条目数；超出时整体清空。
     * 原始地址可能高基数（带一次性query/token的URL、路径参数），不封顶会导致内存无界增长
     */
    private static final int MAX_CACHED_LOCATIONS = 1024;

    /**
     * 重定向地址缓存，key为触发重定向的原始请求地址，value为重定向链解析后的最终地址。
     * 拦截器为Scope.METHOD共享实例，必须以原始地址为key区分不同请求，否则会串号
     */
    private final Map<String, String> locationCache = new ConcurrentHashMap<>();

    private final AtomicBoolean statusIsOk = new AtomicBoolean(false);
    private final AtomicBoolean conditionIsOk = new AtomicBoolean(false);
    private final AtomicBoolean locationIsOk = new AtomicBoolean(false);
    private final AtomicBoolean maxCountIsOk = new AtomicBoolean(false);
    private final AtomicBoolean cacheLocationIsOk = new AtomicBoolean(false);


    {
        redirectStatus = new Integer[]{301, 302, 303, 307, 308};
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

    /**
     * 获取缓存的重定向地址
     *
     * @param originalUrl 发生重定向的原始请求地址
     * @return 缓存的最终地址，不存在时返回null
     */
    public String getCachedLocation(String originalUrl) {
        return locationCache.get(originalUrl);
    }

    /**
     * 以原始请求地址为key缓存重定向后的最终地址，下次请求同一地址时可直接命中，跳过中间的3xx往返
     *
     * @param originalUrl 发生重定向的原始请求地址
     * @param location    重定向链解析后的最终地址
     */
    public void putCachedLocation(String originalUrl, String location) {
        // 容量封顶：原始地址高基数时避免缓存无界增长，达到上限则整体清空
        if (locationCache.size() >= MAX_CACHED_LOCATIONS) {
            locationCache.clear();
        }
        locationCache.put(originalUrl, location);
    }

    /**
     * 清空重定向地址缓存
     */
    public void clearLocationCache() {
        locationCache.clear();
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
        if (isEnable(context) && isCacheLocation(context)) {
            DefaultRequest defaultRequest = (DefaultRequest) request;
            String currentUrl = defaultRequest.getUrl();
            // 以原始请求地址为key查缓存，只有同一地址曾重定向过才复用其最终地址
            String location = getCachedLocation(currentUrl);
            if (StringUtils.hasText(location)) {
                // 命中缓存与真实重定向要走同一套「替换前处理」，否则两条入口行为发散：
                //   1.以缓存地址自带的查询参数为准，清掉请求里的同名旧值，否则getCompleteUrl会把它重复拼上（?t=2&p=1&p=1）
                //   2.缓存地址若与本次请求不同主机，同样要剥离Cookie/Authorization，保证跨主机凭据剥离一致
                clearRepeatParams(defaultRequest, location);
                stripSensitiveHeadersOnHostChange(defaultRequest, currentUrl, location);
                log.info("Replace the request address with the cached redirect address: {} -> {}", currentUrl, location);
                replaceRequestUrl(defaultRequest, location);
            }
        }
    }

    @Override
    public Response doAfterExecute(Response response, InterceptorContext context) {
        String originalUrl = ((DefaultRequest) response.getRequest()).getUrl();
        return followRedirect(response, context, 1, originalUrl);
    }

    public Response doAfterExecuteCalculateCount(Response response, InterceptorContext context, int count) {
        String originalUrl = ((DefaultRequest) response.getRequest()).getUrl();
        return followRedirect(response, context, count, originalUrl);
    }

    private Response followRedirect(Response response, InterceptorContext context, int count, String originalUrl) {
        // maxCount<=0 视为关闭重定向，避免无上限跳转导致死循环
        if (isEnable(context) && getMaxRedirectCount(context) > 0 && isAllowRedirect(response, context)) {
            try {
                checkRedirectCount(context, count);
                DefaultRequest request = (DefaultRequest) response.getRequest();
                if (count == 1) {
                    recordRedirectUrl(context, originalUrl);
                }

                String fromUrl = request.getUrl();
                // Location可能是相对地址，先换成绝对地址，再用于替换请求地址与记录重定向链
                String redirectLocation = toAbsoluteLocation(fromUrl, getRedirectLocation(context, response));

                clearRepeatParams(request, redirectLocation);
                // 303响应的后续请求必须改为GET并丢弃请求体
                changeMethodIfNeeded(request, response.getStatus());
                // 跨主机重定向时剥离敏感头，避免Cookie/Authorization泄露给新主机
                stripSensitiveHeadersOnHostChange(request, fromUrl, redirectLocation);

                log.info("Redirecting [{}] {} to {}", response.getStatus(), fromUrl, redirectLocation);
                recordRedirectUrl(context, redirectLocation);
                replaceRequestUrl(request, redirectLocation);

                // 当前重定向响应已不再使用，先释放其占用的连接，避免中间响应长期未关闭
                response.close();

                // 执行请求，并记录执行时间
                long startTime = System.currentTimeMillis();
                Response redirectResponse = context.getContext().getHttpExecutor().execute(request);
                context.getContextVar().addRootVariable(_$RESPONSE_TIME_SPENT$_, new ResponseTimeSpent(startTime, System.currentTimeMillis()));

                // 继续尝试重定向
                redirectResponse = followRedirect(redirectResponse, context, count + 1, originalUrl);
                // request已随重定向链被改写，count==1返回时其当前URL即最终地址；以最初触发重定向的地址为key缓存
                if (count == 1 && isCacheLocation(context)) {
                    putCachedLocation(originalUrl, request.getUrl());
                }
                return redirectResponse;
            } catch (RuntimeException | Error e) {
                // 重定向过程中出现异常时，当前响应会随异常一起丢失，必须在此释放
                response.close();
                throw e;
            }
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

    /**
     * 303 See Other 的后续请求必须改为GET并丢弃请求体（RFC 7231）。
     * <pre>
     *     即使当前方法已经是GET也必须清空请求体：JDK执行器固定setDoOutput(true)，
     *     只要还有body就会调用getOutputStream()，底层HttpURLConnection会把GET隐式改成POST，
     *     导致303后依然以POST重发；清空body后才能真正发出GET
     * </pre>
     *
     * @param request 待修改的请求
     * @param status  当前响应状态码
     */
    private void changeMethodIfNeeded(DefaultRequest request, int status) {
        if (status == 303 && request.getRequestMethod() != RequestMethod.HEAD) {
            request.setRequestMethod(RequestMethod.GET);
            // 直接操作RequestParameter清除body：DefaultRequest.setBody(null)会因body.getContentType()抛NPE，
            // 且@BinaryBody以bodyFactory形式保存，只清body会被getBody()重新造出来，必须一并清掉factory
            RequestParameter parameter = request.getRequestParameter();
            parameter.setBody(null);
            parameter.setBodyFactory(null);
        }
    }

    /**
     * 当重定向目标与当前请求不在同一主机时，剥离Cookie与Authorization头，
     * 防止认证信息随3xx跳转泄露给第三方主机（与主流HTTP客户端行为一致）
     *
     * @param request  待修改的请求
     * @param fromUrl  重定向前的请求地址
     * @param location 重定向后的绝对地址
     */
    private void stripSensitiveHeadersOnHostChange(DefaultRequest request, String fromUrl, String location) {
        if (isSameHost(fromUrl, location)) {
            return;
        }
        request.removerHeader("Cookie");
        request.removerHeader("Authorization");
    }

    /**
     * 判断两个URL是否属于同一主机，任一解析失败或缺失host时保守地视为同一主机（不剥离）
     */
    private boolean isSameHost(String urlA, String urlB) {
        try {
            String hostA = URI.create(urlA).getHost();
            String hostB = URI.create(urlB).getHost();
            if (hostA == null || hostB == null) {
                return true;
            }
            return hostA.equalsIgnoreCase(hostB);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 把重定向地址换成绝对地址
     * <pre>
     *     服务端返回的Location可能是相对地址（如{@code /new/path}），
     *     此时需要基于当前请求地址解析出完整地址，否则会丢失域名部分
     * </pre>
     *
     * @param currentUrl 当前的请求地址
     * @param location   重定向地址
     * @return 绝对形式的重定向地址
     */
    private String toAbsoluteLocation(String currentUrl, String location) {
        if (StringUtils.isProtocol(location)) {
            return location;
        }
        try {
            return URI.create(currentUrl).resolve(location).toString();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to resolve the relative redirect address [{}] against the current request address [{}]", location, currentUrl);
            return location;
        }
    }

    /**
     * 用重定向地址整体替换请求地址
     * <pre>
     *     1.绝对地址整段交给path，因为path含协议头时{@link StringUtils#joinUrlPath(String, String)}
     *       会直接返回path，只改baseUrl无法覆盖已经带协议的path（URL写在@Get注解上的场景）
     *     2.baseUrl必须清空，否则它会作为path的前缀被再次拼接
     *     3.setPath虽然是追加语义，但待追加的地址带协议头时会被整体覆盖，正好完成替换
     * </pre>
     *
     * @param request  待修改的请求
     * @param location 绝对形式的重定向地址
     */
    private void replaceRequestUrl(DefaultRequest request, String location) {
        request.setBaseUrl("");
        request.setPath(location);
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
            throw new RedirectException("Redirection failed, invalid redirect address, expression: '" + redirectLocationExp + "', value: '" + location + "'").error(log);
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
