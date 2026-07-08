package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.core.ssl.SSLSocketFactoryWrap;
import com.luckyframework.httpclient.core.ssl.SSLUtils;
import com.luckyframework.httpclient.core.ssl.TrustAllHostnameVerifier;
import com.luckyframework.httpclient.proxy.configapi.SSLConf;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.ssl.SSLSocketFactoryBuilder;
import org.springframework.core.io.InputStreamSource;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_COUNT$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_DECIDER_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_SWITCH$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$RETRY_TASK_NAME$__;

/**
 * 请求参数工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/7/9 00:40
 */
public class RequestParameterUtils {

    public static final Consumer<RequestParamInfo> HEADER_PARAMS_SETTING = reqInfo -> reqInfo.getRequest().setHeader(reqInfo.getName(), reqInfo.getValue());
    public static final Consumer<RequestParamInfo> QUERY_PARAMS_SETTING = reqInfo -> reqInfo.getRequest().addQueryParameter(reqInfo.getName(), reqInfo.getValue());
    public static final Consumer<RequestParamInfo> PATH_PARAMS_SETTING = reqInfo -> reqInfo.getRequest().addPathParameter(reqInfo.getName(), reqInfo.getValue());
    public static final Consumer<RequestParamInfo> FORM_PARAMS_SETTING = reqInfo -> reqInfo.getRequest().addFormParameter(reqInfo.getName(), reqInfo.getValue());
    public static final Consumer<RequestParamInfo> MULTIPART_FORM_DATA_PARAMS_SETTING = reqInfo -> reqInfo.getRequest().addMultipartFormParameter(reqInfo.getName(), reqInfo.getValue());

    /**
     * 设置 Header 参数
     *
     * @param mc           上下文对象
     * @param request      请求示例
     * @param headerParams Header 参数
     */
    public static void setHeaderParams(Context mc, Request request, Map<String, Object> headerParams) {
        setParameter(mc, request, headerParams, HEADER_PARAMS_SETTING);
    }

    /**
     * 设置 Query 参数
     *
     * @param mc          上下文对象
     * @param request     请求示例
     * @param queryParams Query 参数
     */
    public static void setQueryParams(Context mc, Request request, Map<String, Object> queryParams) {
        setParameter(mc, request, queryParams, QUERY_PARAMS_SETTING);
    }

    /**
     * 设置 Path 参数
     *
     * @param mc         上下文对象
     * @param request    请求示例
     * @param pathParams Path 参数
     */
    public static void setPathParams(Context mc, Request request, Map<String, Object> pathParams) {
        setParameter(mc, request, pathParams, PATH_PARAMS_SETTING);
    }

    /**
     * 设置 Form 参数
     *
     * @param mc         上下文对象
     * @param request    请求示例
     * @param formParams Form 参数
     */
    public static void setFormParams(Context mc, Request request, Map<String, Object> formParams) {
        setParameter(mc, request, formParams, FORM_PARAMS_SETTING);
    }

    /**
     * 设置 multipart/form-data参数
     *
     * @param mc                  上下文对象
     * @param request             请求示例
     * @param multipartFormParams multipart/form-data 参数
     */
    public static void setMultipartFormData(Context mc, Request request, Map<String, Object> multipartFormParams) {
        setParameter(mc, request, multipartFormParams, MULTIPART_FORM_DATA_PARAMS_SETTING);
    }

    /**
     * 设置带条件的 Header 参数
     *
     * @param mc                    上下文对象
     * @param request               请求示例
     * @param conditionHeaderParams 带条件的 Header 参数
     */
    public static void setConditionHeaderParams(Context mc, Request request, List<ConditionConfig> conditionHeaderParams) {
        setConditionParameter(mc, request, conditionHeaderParams, HEADER_PARAMS_SETTING);
    }

    /**
     * 设置带条件的 Query 参数
     *
     * @param mc                   上下文对象
     * @param request              请求示例
     * @param conditionQueryParams 带条件的 Query 参数
     */
    public static void setConditionQueryParams(Context mc, Request request, List<ConditionConfig> conditionQueryParams) {
        setConditionParameter(mc, request, conditionQueryParams, QUERY_PARAMS_SETTING);
    }

    /**
     * 带条件的设置 Path 参数
     *
     * @param mc                  上下文对象
     * @param request             请求示例
     * @param conditionPathParams 带条件的 Path 参数
     */
    public static void setConditionPathParams(Context mc, Request request, List<ConditionConfig> conditionPathParams) {
        setConditionParameter(mc, request, conditionPathParams, PATH_PARAMS_SETTING);
    }

    /**
     * 带条件的 设置 Form 参数
     *
     * @param mc                  上下文对象
     * @param request             请求示例
     * @param conditionFormParams 带条件的 Form 参数
     */
    public static void setConditionFormParams(Context mc, Request request, List<ConditionConfig> conditionFormParams) {
        setConditionParameter(mc, request, conditionFormParams, FORM_PARAMS_SETTING);
    }

    /**
     * 设置带条件的 multipart/form-data参数
     *
     * @param mc                           上下文对象
     * @param request                      请求示例
     * @param conditionMultipartFormParams 带条件的 multipart/form-data 参数
     */
    public static void setConditionMultipartFormData(Context mc, Request request, List<ConditionConfig> conditionMultipartFormParams) {
        setConditionParameter(mc, request, conditionMultipartFormParams, MULTIPART_FORM_DATA_PARAMS_SETTING);
    }

    /**
     * 设置带条件的请求体
     *
     * @param mc                上下文对象
     * @param request           请求对象
     * @param conditionBodyList 条件请求体配置
     */
    public static void setConditionRequestBody(Context mc, Request request, List<ConditionBody> conditionBodyList, String defBody) {
        if (ContainerUtils.isNotEmptyCollection(conditionBodyList)) {
            for (ConditionBody conditionBody : conditionBodyList) {
                String condition = conditionBody.getCondition();
                if (StringUtils.hasText(condition) && mc.parseExpression(condition, boolean.class)) {
                    setRequestBody(mc, request, conditionBody.getBody());
                    return;
                }
            }
        }
        setRequestBody(mc, request, defBody);
    }

    /**
     * 设置带条件的请求体
     *
     * @param mc                上下文对象
     * @param request           请求对象
     * @param conditionBodyList 条件请求体配置
     */
    public static void setConditionRequestBody(Context mc, Request request, List<ConditionBody> conditionBodyList) {
        if (ContainerUtils.isNotEmptyCollection(conditionBodyList)) {
            for (ConditionBody conditionBody : conditionBodyList) {
                String condition = conditionBody.getCondition();
                if (StringUtils.hasText(condition) && mc.parseExpression(condition, boolean.class)) {
                    setRequestBody(mc, request, conditionBody.getBody());
                    return;
                }
            }
        }
    }

    /**
     * 设置请求体
     *
     * @param mc      上下文对象
     * @param request 请求对象
     * @param body    请求体配置
     */
    public static void setRequestBody(Context mc, Request request, String body) {
        if (!StringUtils.hasText(body)) {
            return;
        }
        Object bodyResult = mc.parseExpression(body);
        if (bodyResult instanceof BodyObject) {
            request.setBody((BodyObject) bodyResult);
        } else if (bodyResult instanceof InputStreamSource) {
            request.setBody(BodyObject.binaryBody((InputStreamSource) bodyResult));
        } else if (bodyResult instanceof File) {
            request.setBody(BodyObject.binaryBody((File) bodyResult));
        } else if (bodyResult instanceof byte[]) {
            request.setBody(BodyObject.binaryBody((byte[]) bodyResult));
        } else {
            String strBody = String.valueOf(bodyResult);
            ContentType contentType = request.getContentType();
            contentType = contentType == ContentType.NON ? ContentType.TEXT_PLAIN : contentType;
            request.setBody(BodyObject.builder(contentType, strBody));
        }
    }

    /**
     * SSL相关配置
     *
     * @param context 方法上下文实例
     * @param request 当前请求实例
     * @param ssl     SSL相关配置
     */
    public static void sslSetter(Context context, Request request, SSLConf ssl) {
        if (ssl != null && Objects.equals(Boolean.TRUE, ssl.getEnable())) {

            // HostnameVerifier
            HostnameVerifier hostnameVerifier = StringUtils.hasText(ssl.getHostnameVerifier())
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
                sslSocketFactory = new SSLSocketFactoryWrap(SSLUtils.createSSLContext(ssl.getProtocol(), keyStoreInfo, trustStoreInfo));
            }
            request.setHostnameVerifier(hostnameVerifier);
            request.setSSLSocketFactory(sslSocketFactory);
        }
    }


    /**
     * 设置重试相关的配置
     *
     * @param context     方法上下文实例
     * @param retryConfig 重试相关配置
     */
    public static void retrySetter(Context context, RetryConfiguration retryConfig) {
        if (retryConfig != null && Objects.equals(Boolean.TRUE, retryConfig.isEnable())) {
            SpELVariate contextVar = context.getContextVar();

            contextVar.addVariable(__$RETRY_SWITCH$__, true);

            String taskName = retryConfig.getTaskNameFormat();
            if (StringUtils.hasText(taskName)) {
                contextVar.addVariable(__$RETRY_TASK_NAME$__, taskName);
            }

            contextVar.addVariable(__$RETRY_COUNT$__, retryConfig.getCount());
            Function<MethodContext, RunBeforeRetryContext<?>> beforeRetryFunction = c -> new ConfigurationBackoffWaitingBeforeRetryContext(retryConfig);
            Function<MethodContext, RetryDeciderContext<?>> deciderFunction = c -> new ConfigurationRetryDeciderContext(retryConfig);

            contextVar.addVariable(__$RETRY_RUN_BEFORE_RETRY_FUNCTION$__, beforeRetryFunction);
            contextVar.addVariable(__$RETRY_DECIDER_FUNCTION$__, deciderFunction);
        }
    }

    /**
     * 设置条件参数
     *
     * @param mc               上下文对象
     * @param request          请求对象
     * @param conditionConfigs 条件配置
     * @param requestConsumer  请求消费者
     */
    private static void setConditionParameter(Context mc, Request request, List<ConditionConfig> conditionConfigs, Consumer<RequestParamInfo> requestConsumer) {
        if (ContainerUtils.isEmptyCollection(conditionConfigs)) {
            return;
        }
        for (ConditionConfig conditionConfig : conditionConfigs) {
            String condition = conditionConfig.getCondition();
            if (StringUtils.hasText(condition) && mc.parseExpression(condition, boolean.class)) {
                setParameter(mc, request, conditionConfig.getConfigs(), requestConsumer);
            }
        }
    }

    /**
     * 设置参数
     *
     * @param mc              上下文对象
     * @param request         请求对象
     * @param configMap       配置 Map
     * @param requestConsumer 请求消费者
     */
    private static void setParameter(Context mc, Request request, Map<String, Object> configMap, Consumer<RequestParamInfo> requestConsumer) {
        if (ContainerUtils.isEmptyMap(configMap)) {
            return;
        }
        configMap.forEach((name, value) -> {
            String pName = mc.parseExpression(name, String.class);
            if (ContainerUtils.isIterable(value)) {
                ContainerUtils.getIterable(value).forEach(e -> {
                    requestConsumer.accept(RequestParamInfo.of(pName, mc.parseExpression(String.valueOf(e)), request));
                });
            } else {
                requestConsumer.accept(RequestParamInfo.of(pName, mc.parseExpression(String.valueOf(value)), request));
            }
        });
    }

}
