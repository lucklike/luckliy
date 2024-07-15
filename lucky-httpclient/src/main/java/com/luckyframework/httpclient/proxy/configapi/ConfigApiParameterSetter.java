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
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.setter.UrlParameterSetter;
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

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;

/**
 * Spring环境变量API参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:24
 */
public class ConfigApiParameterSetter implements ParameterSetter {

    private final UrlParameterSetter urlSetter = new UrlParameterSetter();

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        ConfigContextApi contextApi = (ConfigContextApi) paramInfo.getValue();
        ConfigApi api = contextApi.getApi();
        MethodContext context = contextApi.getContext();

        api.getSpELImport().importSpELRuntime(context);

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
            context.getGlobalVar().addRootVariable(ASYNC_TAG, true);
        }

        if (StringUtils.hasText(api.getAsyncExecutor())) {
            context.getGlobalVar().addRootVariable(ASYNC_EXECUTOR, api.getAsyncExecutor());
        }

        LazyValue<HttpExecutor> lazyHttpExecutor = api.getLazyHttpExecutor(context);
        if (lazyHttpExecutor != null) {
            context.getGlobalVar().addRootVariable(HTTP_EXECUTOR, lazyHttpExecutor);
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
            context.getGlobalVar().addRootVariable(LISTENER_VAR, eventListener);
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
                jsonBody = context.parseExpression((String) jsonBody, String.class);
                request.setBody(BodyObject.jsonBody(jsonBody));
            } else {
                try {
                    String json = EncoderUtils.json(jsonBody);
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

    private EventListener getEventListener(Context context, SseListenerConf listenerConf) {
        return (EventListener) context.getHttpProxyFactory().getObjectCreator().newObject(listenerConf.getClazz(), listenerConf.getBeanName(), context, listenerConf.getScope());
    }

}
