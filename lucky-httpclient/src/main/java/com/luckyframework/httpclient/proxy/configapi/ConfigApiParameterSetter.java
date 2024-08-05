package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.core.ssl.TrustAllHostnameVerifier;
import com.luckyframework.httpclient.proxy.CommonFunctions;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.setter.UrlParameterSetter;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.httpclient.proxy.ssl.SSLSocketFactoryBuilder;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;

/**
 * Spring环境变量API参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:24
 */
@SuppressWarnings("all")
public class ConfigApiParameterSetter implements ParameterSetter {


    private final UrlParameterSetter urlSetter = new UrlParameterSetter();

    @Override
    public void set(Request request, ParamInfo paramInfo) {

        // 获取API配置和方法上下文
        ConfigContextApi contextApi = (ConfigContextApi) paramInfo.getValue();
        ConfigApi api = contextApi.getApi();
        MethodContext context = contextApi.getContext();

        // 导入SpringEL变量、函数和包
        api.getSpringElImport().importSpELRuntime(context);
        // 设置URL和请求方法
        setUrlAndMethod(context, request, api);
        // 设置异步任务相关的配置
        asyncSetter(context, api);
        // SSL相关配置
        sslSetter(context, request, api);
        // 重试相关的配置
        retrySetter(context, api);
        // 超时相关配置
        timeOutSetter(context, request, api);
        // 代理相关配置
        proxySetter(context, request, api);
        // 参数相关配置
        parameterSetter(context, request, api);
        // 请求体相关配置
        bodySetter(context, request, api);
        // SSE相关配置
        sseSetter(context, request, api);
    }

    /**
     * 设置URL以及请求方法
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void setUrlAndMethod(MethodContext context, Request request, ConfigApi api) {

        TempPair<String, String> urlPair = api.getUrlPair();
        String cUrl = urlPair.getOne();
        String mUrl = urlPair.getTwo();

        cUrl = StringUtils.hasText(cUrl) ? context.parseExpression(cUrl, String.class) : "";
        mUrl = StringUtils.hasText(mUrl) ? context.parseExpression(mUrl, String.class) : "";
        String url = StringUtils.joinUrlPath(cUrl, mUrl);
        if (StringUtils.hasText(url)) {
            urlSetter.doSet(request, "url", url);
        }

        if (api.getMethod() != null) {
            request.setRequestMethod(api.getMethod());
        }
    }

    /**
     * 异步任务相关的设置
     *
     * @param context 方法上下文实例
     * @param api     当前API配置
     */
    private void asyncSetter(MethodContext context, ConfigApi api) {
        if (api.isAsync()) {
            context.getContextVar().addVariable(ASYNC_TAG, true);
        }

        if (StringUtils.hasText(api.getAsyncExecutor())) {
            context.getContextVar().addVariable(ASYNC_EXECUTOR, api.getAsyncExecutor());
        }

        LazyValue<HttpExecutor> lazyHttpExecutor = api.getLazyHttpExecutor(context);
        if (lazyHttpExecutor != null) {
            context.getContextVar().addVariable(HTTP_EXECUTOR, lazyHttpExecutor);
        }
    }

    /**
     * SSL相关配置
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void sslSetter(MethodContext context, Request request, ConfigApi api) {
        SSLConf ssl = api.getSsl();
        if (Objects.equals(Boolean.TRUE, ssl.getEnable())) {

            // HostnameVerifier
            HostnameVerifier hostnameVerifier
                    = StringUtils.hasText(ssl.getHostnameVerifier())
                    ? context.parseExpression(ssl.getHostnameVerifier(), HostnameVerifier.class)
                    : TrustAllHostnameVerifier.DEFAULT_INSTANCE;

            // SSLSocketFactory
            SSLSocketFactory sslSocketFactory;
            if (StringUtils.hasText(ssl.getSslSocketFactory())) {
                sslSocketFactory = context.parseExpression(ssl.getSslSocketFactory(), SSLSocketFactory.class);
            } else {
                KeyStoreInfo keyStoreInfo = ssl.getKeyStoreInfo();
                KeyStoreInfo trustStoreInfo = ssl.getTrustStoreInfo();

                String keyStore = ssl.getKeyStore();
                String trustStore = ssl.getTrustStore();
                if (keyStoreInfo == null) {
                    keyStoreInfo = SSLSocketFactoryBuilder.getKeyStoreInfo(context, keyStore);
                }
                if (trustStoreInfo == null) {
                    trustStoreInfo = SSLSocketFactoryBuilder.getKeyStoreInfo(context, trustStore);
                }
                sslSocketFactory = SSLUtils.createSSLContext(ssl.getProtocol(), keyStoreInfo, trustStoreInfo).getSocketFactory();
            }
            request.setHostnameVerifier(hostnameVerifier);
            request.setSSLSocketFactory(sslSocketFactory);
        }
    }

    /**
     * 设置重试相关的配置
     *
     * @param context 方法上下文实例
     * @param api     当前API配置
     */
    private void retrySetter(MethodContext context, ConfigApi api) {
        RetryConf retry = api.getRetry();
        if (Objects.equals(Boolean.TRUE, retry.getEnable())) {
            MapRootParamWrapper contextVar = context.getContextVar();

            contextVar.addVariable(RETRY_SWITCH, true);

            String taskName = retry.getTaskName();
            if (StringUtils.hasText(taskName)) {
                contextVar.addVariable(RETRY_TASK_NAME, taskName);
            }

            Integer maxCount = retry.getMaxCount();
            if (maxCount != null) {
                contextVar.addVariable(RETRY_COUNT, maxCount);
            }

            Function<MethodContext, RunBeforeRetryContext> beforeRetryFunction = c -> c.getHttpProxyFactory().getObjectCreator().newObject(ConfigApiBackoffWaitingBeforeRetryContext.class, "", c, Scope.METHOD_CONTEXT, bwbrc -> {
                if (retry.getWaitMillis() != null) {
                    bwbrc.setWaitMillis(retry.getWaitMillis());
                }
                if (retry.getMaxWaitMillis() != null) {
                    bwbrc.setMaxWaitMillis(retry.getMaxWaitMillis());
                }
                if (retry.getMinWaitMillis() != null) {
                    bwbrc.setMinWaitMillis(retry.getMinWaitMillis());
                }
                if (retry.getMultiplier() != null) {
                    bwbrc.setMultiplier(retry.getMultiplier());
                }
            });

            Function<MethodContext, RetryDeciderContext> deciderFunction = c -> c.getHttpProxyFactory().getObjectCreator().newObject(ConfigApiHttpExceptionRetryDeciderContext.class, "", c, Scope.METHOD_CONTEXT, herdc -> {
                herdc.setRetryFor(retry.getException().toArray(new Class[0]));
                herdc.setExclude(retry.getExclude().toArray(new Class[0]));
                herdc.setExceptionStatus(ConversionUtils.conversion(retry.getExceptionStatus(), int[].class));
                herdc.setNormalStatus(ConversionUtils.conversion(retry.getNormalStatus(), int[].class));
                herdc.setRetryExpression(retry.getExpression());
            });

            contextVar.addVariable(RETRY_RUN_BEFORE_RETRY_FUNCTION, beforeRetryFunction);
            contextVar.addVariable(RETRY_DECIDER_FUNCTION, deciderFunction);
        }
    }

    /**
     * 设置超时相关的配置
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void timeOutSetter(MethodContext context, Request request, ConfigApi api) {
        if (api.getConnectTimeout() != null) {
            request.setConnectTimeout(context.parseExpression(api.getConnectTimeout(), Integer.class));
        }

        if (api.getReadTimeout() != null) {
            request.setReadTimeout(context.parseExpression(api.getReadTimeout(), Integer.class));
        }

        if (api.getWriteTimeout() != null) {
            request.setWriterTimeout(context.parseExpression(api.getWriteTimeout(), Integer.class));
        }
    }

    /**
     * 设置请求参数
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void parameterSetter(MethodContext context, Request request, ConfigApi api) {

        api.getHeader().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(String.valueOf(v));
            request.setHeader(key, value);
        });

        api.getQuery().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            v.forEach(e -> {
                Object value = context.parseExpression(String.valueOf(e));
                request.addQueryParameter(key, value);
            });
        });

        api.getPath().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(String.valueOf(v));
            request.addPathParameter(key, value);
        });

        api.getForm().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(String.valueOf(v));
            request.addFormParameter(key, value);
        });

        api.getMultiData().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(String.valueOf(v));
            request.addMultipartFormParameter(key, value);
        });

        api.getMultiFile().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(String.valueOf(v));
            HttpFile[] httpFiles = null;

            // 是资源类型
            if (HttpExecutor.isResourceParam(value)) {
                httpFiles = HttpExecutor.toHttpFiles(value);
            }
            // 字符串类型或者是字符串数组、集合类型
            else if (ContainerUtils.getElementType(value) == String.class) {
                if (ContainerUtils.isIterable(value)) {
                    List<Resource> resourceList = new ArrayList<>();
                    Iterator<Object> iterator = ContainerUtils.getIterator(value);
                    while (iterator.hasNext()) {
                        resourceList.addAll(Arrays.asList(ConversionUtils.conversion(iterator.next(), Resource[].class)));
                    }
                    httpFiles = HttpExecutor.toHttpFiles(resourceList);
                } else {
                    httpFiles = HttpExecutor.toHttpFiles(ConversionUtils.conversion(value, Resource[].class));
                }
            }

            if (ContainerUtils.isNotEmptyArray(httpFiles)) {
                request.addHttpFiles(key, httpFiles);
            }
        });

    }

    /**
     * 设置代理
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void proxySetter(MethodContext context, Request request, ConfigApi api) {
        ProxyConf proxy = api.getProxy();
        String ip = context.parseExpression(proxy.getIp(), String.class);
        String port = context.parseExpression(proxy.getPort(), String.class);
        if (StringUtils.hasText(ip) && StringUtils.hasText(port)) {
            request.setProxyInfo(
                    new ProxyInfo()
                            .setProxy(proxy.getType(), ip, Integer.parseInt(port))
                            .setUsername(proxy.getUsername())
                            .setPassword(proxy.getPassword())
            );
        }
    }

    /**
     * 设置请求体
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void bodySetter(MethodContext context, Request request, ConfigApi api) {
        Body body = api.getBody();

        // JSON
        Object jsonBody = body.getJson();
        if (jsonBody != null) {
            if (jsonBody instanceof String) {
                jsonBody = context.parseExpression((String) jsonBody, String.class);
                request.setBody(BodyObject.jsonBody((String) jsonBody));
            } else {
                try {
                    String json = CommonFunctions.json(jsonBody);
                    json = context.parseExpression(json, String.class);
                    request.setBody(BodyObject.jsonBody(json));
                } catch (Exception e) {
                    throw new SerializationException(e);
                }
            }

        }
        // XML
        else if (StringUtils.hasText(body.getXml())) {
            String xmlBody = context.parseExpression(body.getXml(), String.class);
            request.setBody(BodyObject.xmlBody(xmlBody));
        }
        // FORM
        else if (StringUtils.hasText(body.getForm())) {
            String formBody = context.parseExpression(body.getForm(), String.class);
            String charset = context.parseExpression(body.getCharset(), String.class);
            request.setBody(BodyObject.builder("application/x-www-form-urlencoded", charset, formBody));
        }
        // Google Protobuf
        else if (StringUtils.hasText(body.getProtobuf())) {
            byte[] protobufBody = context.parseExpression(body.getProtobuf(), byte[].class);
            String charset = context.parseExpression(body.getCharset(), String.class);
            request.setBody(BodyObject.builder("application/x-protobuf", charset, protobufBody));
        }
        // JDK Serializable
        else if (StringUtils.hasText(body.getJava())) {
            String javaBody = context.parseExpression(body.getJava(), String.class);
            String charset = context.parseExpression(body.getCharset(), String.class);
            request.setBody(BodyObject.builder("application/x-java-serialized-object", charset, javaBody));
        }
        // 二进制格式
        else if (body.getFile() != null) {
            try {
                Resource resource = context.parseExpression(body.getFile(), Resource.class);
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                request.setBody(BodyObject.builder("application/octet-stream", (Charset) null, bytes));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // 自定义格式
        else if (body.getData() != null) {
            String mimeType = context.parseExpression(body.getMimeType(), String.class);
            String charset = context.parseExpression(body.getCharset(), String.class);
            String data = context.parseExpression(body.getData(), String.class);
            request.setBody(BodyObject.builder(mimeType, charset, data));
        }
    }

    /**
     * 设置SSE相关的配置
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void sseSetter(MethodContext context, Request request, ConfigApi api) {
        if (REQ_SSE.equals(api.getType())) {
            if (api.getReadTimeout() == null) {
                request.setReadTimeout(600000);
            }
            EventListener eventListener = getEventListener(context, api.getSseListener());
            context.getContextVar().addVariable(LISTENER_VAR, eventListener);
        }
    }

    /**
     * 获取SSE事件监听器
     *
     * @param context      方法上下文实例
     * @param listenerConf SSE事件监听器配置
     * @return SSE事件监听器
     */
    private EventListener getEventListener(Context context, SseListenerConf listenerConf) {
        return (EventListener) context.getHttpProxyFactory().getObjectCreator().newObject(listenerConf.getClassName(), listenerConf.getBeanName(), context, listenerConf.getScope());
    }


}
