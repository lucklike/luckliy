package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.ProxyInfo;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestParameter;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.exception.NotFindRequestException;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.web.ContentTypeUtils;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.springframework.util.FileCopyUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于OkHttp旧版本的Http执行器
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 3:10 下午
 */
public class OkHttpExecutor implements HttpExecutor {

    /**
     * OkHttpClient构建器
     */
    private final OkHttpClient.Builder builder;

    public OkHttpExecutor(OkHttpClient.Builder builder) {
        this.builder = builder;
    }

    public OkHttpExecutor(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        this.builder = defaultOkHttpClientBuilder(maxIdleConnections, keepAliveDuration, timeUnit);
    }

    public OkHttpExecutor() {
        this(10, 5, TimeUnit.MINUTES);
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        okhttp3.Response okhttpResponse = null;
        Call call;
        OkHttpClient client;
        try {
            client = createOkHttpClient(request);
            okhttp3.Request okhttpRequest = changeToOkHttpRequest(request);
            call = client.newCall(okhttpRequest);
            okhttpResponse = call.execute();
            resultProcess(request, processor, okhttpResponse);
        } finally {
            if (okhttpResponse != null) {
                okhttpResponse.close();
            }
        }
    }

    protected OkHttpClient.Builder defaultOkHttpClientBuilder(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        return new OkHttpClient.Builder()
                .connectTimeout(Request.DEF_CONNECTION_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(Request.DEF_READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .writeTimeout(Request.DEF_WRITER_TIME_OUT, TimeUnit.MILLISECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, timeUnit));
    }


    /**
     * 创建OkHttp客户端
     *
     * @param request 请求实例
     * @return OkHttp客户端
     */
    private synchronized OkHttpClient createOkHttpClient(Request request) {

        OkHttpClient client = builder.build();
        OkHttpClient.Builder tempBuilder = client.newBuilder();
        ProxyInfo proxyInfo = request.getProxyInfo();
        if (proxyInfo != null) {
            tempBuilder.proxy(proxyInfo.getProxy());
        }

        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        Integer writerTimeout = request.getWriterTimeout();
        HostnameVerifier hostnameVerifier = request.getHostnameVerifier();
        SSLSocketFactory sslSocketFactory = request.getSSLSocketFactory();


        if (connectTimeout != null && connectTimeout > 0) {
            tempBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        }

        if (readTimeout != null && readTimeout > 0) {
            tempBuilder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        }

        if (writerTimeout != null && writerTimeout > 0) {
            tempBuilder.writeTimeout(writerTimeout, TimeUnit.MILLISECONDS);
        }

        if (hostnameVerifier != null) {
            tempBuilder.hostnameVerifier(hostnameVerifier);
        }

        if (sslSocketFactory != null) {
            FieldUtils.setValue(tempBuilder, "sslSocketFactory", sslSocketFactory);
        }
        return tempBuilder.build();
    }

    /**
     * 将Lucky规范中的Request转化为okHttp所需要的Request
     *
     * @param request 请求信息
     * @return okHttp所需要的Request
     * @throws IOException 请求转换期间出现错位会抛出该异常
     */
    private okhttp3.Request changeToOkHttpRequest(Request request) throws IOException {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.getUrl());
        headerSetting(request, builder);
        requestParameterSetting(request, builder);
        return builder.build();
    }

    /**
     * 设置请求头
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void headerSetting(Request request, okhttp3.Request.Builder builder) {
        Map<String, List<Header>> headerMap = request.getHeaderMap();
        for (Map.Entry<String, List<Header>> entry : headerMap.entrySet()) {
            String headerName = entry.getKey();
            List<Header> headerList = entry.getValue();
            if (!ContainerUtils.isEmptyCollection(headerList)) {
                for (Header header : headerList) {
                    Object headerValue = header.getValue();
                    if (headerValue != null) {
                        switch (header.getHeaderType()) {
                            case ADD:
                                builder.addHeader(headerName, headerValue.toString());
                                break;
                            case SET:
                                builder.header(headerName, headerValue.toString());
                                break;
                        }
                    }
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    //                   Create okHttp Client Request
    //--------------------------------------------------------------------------

    /**
     * 设置请求参数以及请求方法
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     * @throws IOException 处理响应结果出现异常时会抛出该异常
     */
    private void requestParameterSetting(Request request, okhttp3.Request.Builder builder) throws IOException {
        switch (request.getRequestMethod()) {
            case GET:
                getSetting(request, builder);
                break;
            case POST:
                postSetting(request, builder);
                break;
            case DELETE:
                deleteSetting(request, builder);
                break;
            case PUT:
                putSetting(request, builder);
                break;
            case HEAD:
                headSetting(request, builder);
                break;
            case PATCH:
                patchSetting(request, builder);
                break;
            default:
                throw new NotFindRequestException("okHttp does not support requests of type ['" + request.getRequestMethod() + "'].");
        }
    }

    /**
     * GET 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void getSetting(Request request, okhttp3.Request.Builder builder) {
        builder.get();
    }

    /**
     * POST 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void postSetting(Request request, okhttp3.Request.Builder builder) throws IOException {
        RequestBody requestBody = getRequestBody(request);
        builder.post(requestBody);
    }

    /**
     * DELETE 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void deleteSetting(Request request, okhttp3.Request.Builder builder) throws IOException {
        RequestBody requestBody = getRequestBody(request);
        builder.delete(requestBody);
    }

    /**
     * PUT 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void putSetting(Request request, okhttp3.Request.Builder builder) throws IOException {
        RequestBody requestBody = getRequestBody(request);
        builder.put(requestBody);
    }

    /**
     * HEAD 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void headSetting(Request request, okhttp3.Request.Builder builder) {
        builder.head();
    }

    /**
     * PATCH 请求的参数设置
     *
     * @param request 请求信息
     * @param builder okHttp的请求构建器
     */
    private void patchSetting(Request request, okhttp3.Request.Builder builder) throws IOException {
        RequestBody requestBody = getRequestBody(request);
        builder.patch(requestBody);
    }

    //-----设置请求体-----//
    private RequestBody getRequestBody(Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();
        BodyObject body = requestParameter.getBody();
        Map<String, Object> fromParameters = requestParameter.getFormParameters();
        Map<String, Object> multipartFromParameters = requestParameter.getMultipartFormParameters();

        //如果设置了Body参数，则优先使用Body参数
        if (body != null) {
            return RequestBody.Companion.create(body.getBody(), MediaType.parse(body.getContentType().toString()));
        }

        // multipart/form-data表单参数优先级其次
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            return getFileBody(multipartFromParameters);
        }

        // form表单优先级最低
        if (ContainerUtils.isNotEmptyMap(fromParameters)) {
            return getFormBody(fromParameters);
        }

        return new FormBody.Builder().build();
    }

    /**
     * 设置表单类型的参数
     *
     * @param nameValuesMap 参数列表
     * @return {@link RequestBody}
     */
    private RequestBody getFormBody(Map<String, Object> nameValuesMap) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, Object> entry : nameValuesMap.entrySet()) {
            builder.add(entry.getKey(), entry.getValue().toString());
        }
        return builder.build();
    }

    /**
     * 设置文件类型的参数
     *
     * @param nameValuesMap 参数列表
     * @return {@link RequestBody}
     */
    private RequestBody getFileBody(Map<String, Object> nameValuesMap) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (Map.Entry<String, Object> paramEntry : nameValuesMap.entrySet()) {
            String paramName = paramEntry.getKey();
            Object paramValue = paramEntry.getValue();

            // 资源类型参数处理
            if (HttpExecutor.isResourceParam(paramValue)) {
                HttpFile[] httpFiles = HttpExecutor.toHttpFiles(paramValue);
                for (HttpFile httpFile : httpFiles) {
                    InputStream in = httpFile.getInputStream();
                    String fileName = httpFile.getFileName();
                    MediaType mediaType = MediaType.parse(ContentTypeUtils.getMimeTypeOrDefault(fileName, "text/plain"));
                    builder.addFormDataPart(paramName, httpFile.getFileName(), RequestBody.create(mediaType, FileCopyUtils.copyToByteArray(in)));
                }
            }
            //其他类型将会被当做String类型的参数
            else {
                builder.addFormDataPart(paramName, String.valueOf(paramValue));
            }
        }
        return builder.build();
    }

    /**
     * 响应结果处理
     *
     * @param request        请求实例
     * @param processor      响应处理器
     * @param okhttpResponse OkHttp的{@link okhttp3.Response}
     */
    private void resultProcess(Request request, ResponseProcessor processor, okhttp3.Response okhttpResponse) throws Exception {
        int code = okhttpResponse.code();

        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        headerChanger(httpHeaderManager, okhttpResponse.headers());
        processor.process(new ResponseMetaData(
                request,
                code,
                httpHeaderManager,
                () -> Objects.requireNonNull(okhttpResponse.body()).byteStream()
        ));
    }

    /**
     * 将okhttp3的响应头转化为lucky-httpclient的响应头
     *
     * @param httpHeaderManager 响应头管理器
     * @param headers           okhttp3的响应头
     */
    private void headerChanger(HttpHeaderManager httpHeaderManager, Headers headers) {
        for (Map.Entry<String, List<String>> entry : headers.toMultimap().entrySet()) {
            String name = entry.getKey();
            List<String> valueList = entry.getValue();
            for (String value : valueList) {
                httpHeaderManager.putHeader(name, value);
            }
        }
    }
}
