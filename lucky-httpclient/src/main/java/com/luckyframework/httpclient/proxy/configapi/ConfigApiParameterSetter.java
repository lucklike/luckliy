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
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        ConfigContextApi contextApi = (ConfigContextApi) paramInfo.getValue();
        ConfigApi api = contextApi.getApi();
        MethodContext context = contextApi.getContext();

        api.getSpringElImport().importSpELRuntime(context);

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

        if (api.isAsync()) {
            context.getContextVar().addRootVariable(ASYNC_TAG, true);
        }

        if (StringUtils.hasText(api.getAsyncExecutor())) {
            context.getContextVar().addRootVariable(ASYNC_EXECUTOR, api.getAsyncExecutor());
        }

        LazyValue<HttpExecutor> lazyHttpExecutor = api.getLazyHttpExecutor(context);
        if (lazyHttpExecutor != null) {
            context.getContextVar().addRootVariable(HTTP_EXECUTOR, lazyHttpExecutor);
        }

        // 重试相关的配置
        RetryConf retry = api.getRetry();
        if (Objects.equals(Boolean.TRUE, retry.getEnable())) {
            MapRootParamWrapper contextVar = context.getContextVar();

            contextVar.addRootVariable(RETRY_SWITCH, true);

            String taskName = retry.getTaskName();
            if (StringUtils.hasText(taskName)) {
                contextVar.addRootVariable(RETRY_TASK_NAME, taskName);
            }

            Integer maxCount = retry.getMaxCount();
            if (maxCount != null) {
                contextVar.addRootVariable(RETRY_COUNT, maxCount);
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

            contextVar.addRootVariable(RETRY_RUN_BEFORE_RETRY_FUNCTION, beforeRetryFunction);
            contextVar.addRootVariable(RETRY_DECIDER_FUNCTION, deciderFunction);
        }

        if (api.getConnectTimeout() != null) {
            request.setConnectTimeout(context.parseExpression(api.getConnectTimeout(), Integer.class));
        }

        if (api.getReadTimeout() != null) {
            request.setReadTimeout(context.parseExpression(api.getReadTimeout(), Integer.class));
        }

        if (api.getWriteTimeout() != null) {
            request.setWriterTimeout(context.parseExpression(api.getWriteTimeout(), Integer.class));
        }

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

        if (REQ_SSE.equals(api.getType())) {
            if (api.getReadTimeout() == null) {
                request.setReadTimeout(600000);
            }
            EventListener eventListener = getEventListener(context, api.getSseListener());
            context.getContextVar().addRootVariable(LISTENER_VAR, eventListener);
        }

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

        Body body = api.getBody();

        // JSON
        Object jsonBody = body.getJson();
        if (jsonBody != null) {
            if (jsonBody instanceof String) {
                jsonBody = context.nestParseExpression((String) jsonBody, String.class);
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
            String xmlBody = context.nestParseExpression(body.getXml(), String.class);
            request.setBody(BodyObject.xmlBody(xmlBody));
        }
        // FORM
        else if (StringUtils.hasText(body.getForm())) {
            String formBody = context.nestParseExpression(body.getForm(), String.class);
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

    private EventListener getEventListener(Context context, SseListenerConf listenerConf) {
        return (EventListener) context.getHttpProxyFactory().getObjectCreator().newObject(listenerConf.getClassName(), listenerConf.getBeanName(), context, listenerConf.getScope());
    }

}
