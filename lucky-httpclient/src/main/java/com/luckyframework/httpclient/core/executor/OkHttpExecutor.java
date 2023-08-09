package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestParameter;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.exception.NotFindRequestException;
import com.luckyframework.reflect.FieldUtils;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于OkHttp的Http执行器
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

    public OkHttpExecutor() {
        this.builder = defaultOkHttpClientBuilder();
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        okhttp3.Response okhttpResponse = null;
        Call call = null;
        OkHttpClient client;
        try {
            client = createOkHttpClient(request);
            okhttp3.Request okhttpRequest = changeToOkHttpRequest(request);
            call = client.newCall(okhttpRequest);
            okhttpResponse = call.execute();
            resultProcess(processor, okhttpResponse);
        } finally {
            if (okhttpResponse != null) {
                okhttpResponse.close();
            }
        }
    }

    protected OkHttpClient.Builder defaultOkHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(Request.DEF_CONNECTION_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(Request.DEF_READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .writeTimeout(Request.DEF_WRITER_TIME_OUT, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES));
    }


    /**
     * 创建OkHttp客户端
     *
     * @param request 请求实例
     * @return OkHttp客户端
     */
    private OkHttpClient createOkHttpClient(Request request) {

        OkHttpClient client = builder.build();
        FieldUtils.setValue(client, "proxy", request.getProxy());

        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        Integer writerTimeout = request.getWriterTimeout();

        if (connectTimeout != null && connectTimeout > 0) {
            FieldUtils.setValue(client, "connectTimeoutMillis", connectTimeout);
        }

        if (readTimeout != null && readTimeout > 0) {
            FieldUtils.setValue(client, "readTimeoutMillis", readTimeout);
        }

        if (writerTimeout != null && writerTimeout > 0) {
            FieldUtils.setValue(client, "writeTimeoutMillis", writerTimeout);
        }

        return client;
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
        Map<String, Object> nameValuesMap = requestParameter.getRequestParameters();
        if (body != null) {
            return RequestBody.Companion.create(body.getBody(), MediaType.parse(body.getContentType().toString()));
        }

        if (ContainerUtils.isEmptyMap(nameValuesMap)) {
            return new FormBody.Builder().build();
        }

        return HttpExecutor.isFileRequest(nameValuesMap) ? getFileBody(nameValuesMap) : getFormBody(nameValuesMap);
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
        MediaType mediaType = MultipartBody.FORM;
        builder.setType(mediaType);
        for (Map.Entry<String, Object> paramEntry : nameValuesMap.entrySet()) {
            String paramName = paramEntry.getKey();
            Object paramValue = paramEntry.getValue();

            // 资源类型参数处理
            if (HttpExecutor.isResourceParam(paramValue)) {
                HttpFile[] httpFiles = HttpExecutor.toHttpFiles(paramValue);
                for (HttpFile httpFile : httpFiles) {
                    InputStream in = httpFile.getInputStream();
                    builder.addFormDataPart(paramName, httpFile.getFileName(), RequestBody.Companion.create(FileCopyUtils.copyToByteArray(in), mediaType));
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
     * 将OkHttp的响应对象转化为Lucky规范中的Response对象
     *
     * @param processor      响应处理器
     * @param okhttpResponse OkHttp的响应对象
     */
    private void resultProcess(ResponseProcessor processor, okhttp3.Response okhttpResponse) {
        int code = okhttpResponse.code();
        Headers headers = okhttpResponse.headers();
        Map<String, List<String>> headerMap = headers.toMultimap();
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            String name = entry.getKey();
            List<String> valueList = entry.getValue();
            for (String value : valueList) {
                httpHeaderManager.putHeader(name, value);
            }
        }
        processor.process(code, httpHeaderManager, () -> Objects.requireNonNull(okhttpResponse.body()).byteStream());
    }
}
