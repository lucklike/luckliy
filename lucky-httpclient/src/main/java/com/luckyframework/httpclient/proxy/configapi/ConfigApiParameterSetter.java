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
import com.luckyframework.httpclient.proxy.mock.DefaultMockResponseFactory;
import com.luckyframework.httpclient.proxy.mock.MockResponseFactory;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.setter.HeaderParameterSetter;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.setter.UrlParameterSetter;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.httpclient.proxy.ssl.SSLSocketFactoryBuilder;
import com.luckyframework.httpclient.proxy.url.AnnotationRequest;
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$ASYNC_EXECUTOR$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$ASYNC_TAG$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$HTTP_EXECUTOR$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$LISTENER_VAR$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$MOCK_RESPONSE_FACTORY$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$REQ_SSE$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$RETRY_COUNT$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$RETRY_DECIDER_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$RETRY_SWITCH$__;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.__$RETRY_TASK_NAME$__;

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
    private final HeaderParameterSetter headerSetter = new HeaderParameterSetter();
    private final DefaultMockResponseFactory mockResponseFactory = new DefaultMockResponseFactory();

    @Override
    public void set(Request request, ParamInfo paramInfo) {

        // 获取API配置和方法上下文
        ConfigContextApi contextApi = (ConfigContextApi) paramInfo.getValue();
        ConfigApi api = contextApi.getApi();
        MethodContext context = contextApi.getContext();

        // 向SpEL运行时环境导入变量、函数和包
        api.getSpringElImport().importSpELRuntime(context, VarScope.METHOD_CONTEXT);
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
        // Mock相关配置
        mockSetter(context, request, api);
        // 参数相关配置
        parameterSetter(context, request, api);
        // 请求体相关配置
        bodySetter(context, request, api);
        // SSE相关配置
        sseSetter(context, request, api);
        // 扩展配置
        extendSetter(context, request, api);
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
        String rcUrl = ((AnnotationRequest) request).getDomain();
        String rmUrl = ((AnnotationRequest) request).getPath();

        cUrl = StringUtils.hasText(cUrl) ? context.parseExpression(cUrl, String.class) : rcUrl;
        mUrl = StringUtils.hasText(mUrl) ? context.parseExpression(mUrl, String.class) : rmUrl;

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
            context.getContextVar().addVariable(__$ASYNC_TAG$__, true);
        }

        if (StringUtils.hasText(api.getAsyncExecutor())) {
            context.getContextVar().addVariable(__$ASYNC_EXECUTOR$__, api.getAsyncExecutor());
        }

        LazyValue<HttpExecutor> lazyHttpExecutor = api.getLazyHttpExecutor(context);
        if (lazyHttpExecutor != null) {
            context.getContextVar().addVariable(__$HTTP_EXECUTOR$__, lazyHttpExecutor);
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
            HostnameVerifier hostnameVerifier = StringUtils.hasText(ssl.getHostnameVerifier()) ? context.parseExpression(ssl.getHostnameVerifier(), HostnameVerifier.class) : TrustAllHostnameVerifier.DEFAULT_INSTANCE;

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
            SpELVariate contextVar = context.getContextVar();

            contextVar.addVariable(__$RETRY_SWITCH$__, true);

            String taskName = retry.getTaskName();
            if (StringUtils.hasText(taskName)) {
                contextVar.addVariable(__$RETRY_TASK_NAME$__, taskName);
            }

            Integer maxCount = retry.getMaxCount();
            if (maxCount != null) {
                contextVar.addVariable(__$RETRY_COUNT$__, maxCount);
            }

            Function<MethodContext, RunBeforeRetryContext> beforeRetryFunction = c -> c.generateObject(ConfigApiBackoffWaitingBeforeRetryContext.class, "", Scope.METHOD_CONTEXT, bwbrc -> {
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

            Function<MethodContext, RetryDeciderContext> deciderFunction = c -> c.generateObject(ConfigApiHttpExceptionRetryDeciderContext.class, "", Scope.METHOD_CONTEXT, herdc -> {
                herdc.setRetryFor(retry.getException().toArray(new Class[0]));
                herdc.setExclude(retry.getExclude().toArray(new Class[0]));
                herdc.setExceptionStatus(ConversionUtils.conversion(retry.getExceptionStatus(), int[].class));
                herdc.setNormalStatus(ConversionUtils.conversion(retry.getNormalStatus(), int[].class));
                herdc.setRetryExpression(retry.getExpression());
            });

            contextVar.addVariable(__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__, beforeRetryFunction);
            contextVar.addVariable(__$RETRY_DECIDER_FUNCTION$__, deciderFunction);
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
        /****************************** Header Setter ********************************************/
        // Header
        api.getHeader().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            v.forEach(e -> {
                trueIsRun(context, e, rv -> headerSetter.doSet(request, key, rv));
            });
        });

        // Condition Header
        for (ConditionMapList conditionHeader : api.getConditionHeader()) {
            if (conditionHeader == null || conditionHeader.getData() == null) {
                continue;
            }
            String condition = conditionHeader.getCondition();
            if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
                continue;
            }

            Map<String, List<Object>> dataMap = conditionHeader.getData();
            if (ContainerUtils.isNotEmptyMap(dataMap)) {
                dataMap.forEach((k, v) -> {
                    String key = context.parseExpression(k, String.class);
                    v.forEach(e -> {
                        trueIsRun(context, e, rv -> headerSetter.doSet(request, key, rv));
                    });
                });
            }
        }

        /****************************** Qquery Setter ********************************************/
        // Query
        api.getQuery().forEach((k, v) -> {
            String key = context.parseExpression(k, String.class);
            v.forEach(e -> {
                trueIsRun(context, e, rv -> request.addQueryParameter(key, rv));
            });
        });

        // Condition Query
        for (ConditionMapList conditionQuery : api.getConditionQuery()) {
            if (conditionQuery == null || conditionQuery.getData() == null) {
                continue;
            }

            String condition = conditionQuery.getCondition();
            if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
                continue;
            }

            Map<String, List<Object>> dataMap = conditionQuery.getData();
            if (ContainerUtils.isNotEmptyMap(dataMap)) {
                dataMap.forEach((k, v) -> {
                    String key = context.parseExpression(k, String.class);
                    v.forEach(e -> {
                        trueIsRun(context, e, rv -> request.addQueryParameter(key, rv));
                    });
                });
            }
        }

        /****************************** Path Setter ********************************************/
        // Path
        api.getPath().forEach((k, v) -> {
            trueIsRun(context, v, rv -> request.addPathParameter(context.parseExpression(k, String.class), rv));
        });

        // Condition Path
        for (ConditionMap conditionPath : api.getConditionPath()) {
            if (conditionPath == null || conditionPath.getData() == null) {
                continue;
            }

            String condition = conditionPath.getCondition();
            if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
                continue;
            }

            Map<String, Object> dataMap = conditionPath.getData();
            if (ContainerUtils.isNotEmptyMap(dataMap)) {
                dataMap.forEach((k, v) -> {
                    trueIsRun(context, v, rv -> request.addPathParameter(context.parseExpression(k, String.class), rv));
                });
            }
        }

        /****************************** Form Setter ********************************************/
        // Form
        api.getForm().forEach((k, v) -> {
            trueIsRun(context, v, rv -> request.addFormParameter(context.parseExpression(k, String.class), rv));
        });

        // Condition Form
        for (ConditionMap conditionForm : api.getConditionForm()) {
            if (conditionForm == null || conditionForm.getData() == null) {
                continue;
            }

            String condition = conditionForm.getCondition();
            if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
                continue;
            }

            Map<String, Object> dataMap = conditionForm.getData();
            if (ContainerUtils.isNotEmptyMap(dataMap)) {
                dataMap.forEach((k, v) -> {
                    trueIsRun(context, v, rv -> request.addFormParameter(context.parseExpression(k, String.class), rv));
                });
            }
        }

        /****************************** Multipart Form Data Setter ********************************************/
        // Multipart Form Data
        api.getMultipartFormData().getTxt().forEach((k, v) -> {
            trueIsRun(context, v, rv -> request.addMultipartFormParameter(context.parseExpression(k, String.class), rv));
        });

        api.getMultipartFormData().getFile().forEach((k, v) -> {
            String _v = context.ifExpressionEvaluation(String.valueOf(v));
            if (!StringUtils.hasText(_v)) {
                return;
            }

            String key = context.parseExpression(k, String.class);
            Object value = context.parseExpression(_v);
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

        // Condition Multipart Form Data
        List<ConditionMultipartFormData> conditionMultipartFormDataList = api.getConditionMultipartFormData();

        for (ConditionMultipartFormData conditionMultipartFormData : conditionMultipartFormDataList) {
            if (conditionMultipartFormData == null || conditionMultipartFormData.getData() == null) {
                continue;
            }

            String condition = conditionMultipartFormData.getCondition();
            if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
                continue;
            }

            MultipartFormData conditionConfigData = conditionMultipartFormData.getData();
            conditionConfigData.getTxt().forEach((k, v) -> {
                trueIsRun(context, v, rv -> request.addMultipartFormParameter(context.parseExpression(k, String.class), rv));
            });

            conditionConfigData.getFile().forEach((k, v) -> {
                String _v = context.ifExpressionEvaluation(String.valueOf(v));
                if (!StringUtils.hasText(_v)) {
                    return;
                }

                String key = context.parseExpression(k, String.class);
                Object value = context.parseExpression(_v);
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
    }

    private void trueIsRun(Context context, Object value, Consumer<Object> consumer) {
        if (value instanceof String) {
            String expression = context.ifExpressionEvaluation((String) value);
            if (StringUtils.hasText(expression)) {
                consumer.accept(context.parseExpression(expression));
            }
        } else {
            consumer.accept(value);
        }
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
            request.setProxyInfo(new ProxyInfo().setProxy(proxy.getType(), ip, Integer.parseInt(port)).setUsername(proxy.getUsername()).setPassword(proxy.getPassword()));
        }
    }

    /**
     * Mock相关配置
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void mockSetter(MethodContext context, Request request, ConfigApi api) {
        MockConf mock = api.getMock();
        if (mock != null && (!StringUtils.hasText(mock.getEnable()) || context.parseExpression(mock.getEnable(), boolean.class))) {
            MockResponseFactory mockFactory = (r, c) -> {
                return mockResponseFactory.doGetMockResponseByCache(
                        r,
                        c.getContext(),
                        mock.getResponse(),
                        mock.getStatus(),
                        mockHeaderToArray(mock),
                        mock.getBody(),
                        mock.getCache()
                );
            };
            context.getContextVar().addVariable(__$MOCK_RESPONSE_FACTORY$__, mockFactory);
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
        else if (body.getXml() != null) {
            Object xmlBody = body.getXml();
            if (xmlBody instanceof String) {
                xmlBody = context.parseExpression((String) xmlBody, String.class);
                request.setBody(BodyObject.xmlBody((String) jsonBody));
            } else {
                try {
                    String xml = CommonFunctions.xml(xmlBody);
                    xml = context.parseExpression(xml, String.class);
                    request.setBody(BodyObject.xmlBody(xml));
                } catch (Exception e) {
                    throw new SerializationException(e);
                }
            }
        }
        // FORM
        else if (body.getForm() != null) {
            String mimeType = "application/x-www-form-urlencoded";
            String charset = context.parseExpression(body.getCharset(), String.class);
            Object formBody = body.getForm();
            if (formBody instanceof String) {
                request.setBody(BodyObject.builder(mimeType, charset, context.parseExpression((String) formBody, String.class)));
            } else {
                try {
                    String form = CommonFunctions.form(formBody);
                    form = context.parseExpression(form, String.class);
                    request.setBody(BodyObject.builder(mimeType, charset, form));
                } catch (Exception e) {
                    throw new SerializationException(e);
                }
            }
        }
        // Google Protobuf
        else if (body.getProtobuf() != null) {
            String mimeType = "application/x-protobuf";
            String charset = context.parseExpression(body.getCharset(), String.class);
            Object protobufBody = body.getProtobuf();
            if (protobufBody instanceof String) {
                request.setBody(BodyObject.builder(mimeType, charset, context.parseExpression((String) body.getProtobuf(), byte[].class)));
            } else {
                request.setBody(BodyObject.builder(mimeType, charset, CommonFunctions.protobuf(protobufBody)));
            }
        }
        // JDK Serializable
        else if (body.getJava() != null) {
            String charset = context.parseExpression(body.getCharset(), String.class);
            String mimeType = "application/x-java-serialized-object";
            Object javaBody = body.getJava();
            if (javaBody instanceof String) {
                request.setBody(BodyObject.builder(mimeType, charset, context.parseExpression((String) javaBody, String.class)));
            } else {
                try {
                    request.setBody(BodyObject.builder(mimeType, charset, CommonFunctions.java(javaBody)));
                } catch (IOException e) {
                    throw new SerializationException(e);
                }
            }
        }
        // 二进制格式
        else if (StringUtils.hasText(body.getFile())) {
            try {
                String file = body.getFile();
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
            Object data = context.parseExpression(body.getData());
            if (data instanceof String) {
                request.setBody(BodyObject.builder(mimeType, charset, (String) data));
            } else if (data instanceof byte[]) {
                request.setBody(BodyObject.builder(mimeType, charset, (byte[]) data));
            } else {
                throw new SerializationException("Unsupported responder type! Expression: {}, Value: {}", body.getData(), data);
            }
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
        if (__$REQ_SSE$__.equals(api.getType())) {
            if (api.getReadTimeout() == null) {
                request.setReadTimeout(600000);
            }
            EventListener eventListener = getEventListener(context, api.getSseListener());
            context.getContextVar().addVariable(__$LISTENER_VAR$__, eventListener);
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
        return (EventListener) context.generateObject(listenerConf.getClassName(), listenerConf.getBeanName(), listenerConf.getScope());
    }


    /**
     * 执行请求扩展的处理逻辑
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param api     当前API配置
     */
    private void extendSetter(MethodContext context, Request request, ConfigApi api) {
        List<Extension<RequestExtendHandle>> requestExtensionList = api.getRequestExtension();
        if (ContainerUtils.isNotEmptyCollection(requestExtensionList)) {
            for (Extension<RequestExtendHandle> requestExtendHandleExtension : requestExtensionList) {
                ExtendHandleConfig<RequestExtendHandle> handleConfig = requestExtendHandleExtension.getHandle();
                Object config = requestExtendHandleExtension.getConfig();
                Class<RequestExtendHandle> handleClass = handleConfig.getClassName();
                handleClass = handleClass == null ? RequestExtendHandle.class : handleClass;
                RequestExtendHandle extendHandle = context.generateObject(handleClass, handleConfig.getBeanName(), handleConfig.getScope());
                extendHandle.handle(context, request, ConversionUtils.looseBind(extendHandle.getType(), config));
            }
        }
    }

    /**
     * 将Mock配置中Map结构的Header转为字符串形式的Hader
     *
     * @param mockConf MockConf配置
     * @return 字符串形式的Header
     */
    private String[] mockHeaderToArray(MockConf mockConf) {
        List<String> stringHeaderList = new ArrayList<>();
        mockConf.getHeader().forEach((k, headerList) -> {
            for (Object hv : headerList) {
                stringHeaderList.add(k + ": " + hv);
            }
        });
        return stringHeaderList.toArray(new String[0]);
    }
}
