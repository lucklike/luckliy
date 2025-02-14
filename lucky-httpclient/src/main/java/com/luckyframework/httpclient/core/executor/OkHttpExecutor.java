package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.ResponseInputStream;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.httpclient.core.ssl.SSLSocketFactoryWrap;
import com.luckyframework.web.ContentTypeUtils;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.luckyframework.httpclient.core.ssl.SSLUtils.TRUST_ALL_TRUST_MANAGERS;

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
        OkHttpClient client = createOkHttpClient(request);
        okhttp3.Request okhttpRequest = changeToOkHttpRequest(request);
        Call call = client.newCall(okhttpRequest);
        okhttp3.Response okhttpResponse = call.execute();
        resultProcess(request, processor, okhttpResponse);
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
            X509TrustManager trustManager;
            if (sslSocketFactory instanceof SSLSocketFactoryWrap) {
                trustManager = (X509TrustManager) ((SSLSocketFactoryWrap) sslSocketFactory).getTrustManagers()[0];
            } else {
                trustManager = TRUST_ALL_TRUST_MANAGERS[0];
            }

            tempBuilder.sslSocketFactory(sslSocketFactory, trustManager);
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
        builder.method(request.getRequestMethod().toString(), getRequestBody(request));
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
        headerMap.forEach((name, headers) -> addHeaders(name, headers, builder));
    }

    /**
     * 添加请求头
     *
     * @param headerName 请求头名称
     * @param headerList 请求头值
     * @param builder    请求构建器
     */
    private void addHeaders(String headerName, List<Header> headerList, okhttp3.Request.Builder builder) {
        if (ContainerUtils.isEmptyCollection(headerList)) {
            return;
        }
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


    /**
     * 获取OkHttp的请求体对象
     * <pre>
     *     1. 如果Lucky请求中存在{@link BodyObject}对象，则优先使用该对象作为请求体
     *     2. 如果Lucky请求中存在multipart/form-data的表单参数，则使用该表单作为请求体
     *     3. 如果Lucky请求中存在application/x-www-form-urlencoded的表单参数，则使用该表单作为请求体
     * </pre>
     *
     * @param request Lucky请求对象
     * @return OkHttp请求体对象
     * @throws IOException 创建过程中可能出现IO异常
     */
    private RequestBody getRequestBody(Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();

        //如果设置了Body参数，则优先使用Body参数
        BodyObject body = requestParameter.getBody();
        if (body != null) {
            return new InputStreamRequestBody(MediaType.parse(body.getContentType().toString()), body.getBodyStream());
        }

        // multipart/form-data表单参数优先级其次
        Map<String, Object> multipartFromParameters = requestParameter.getMultipartFormParameters();
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            return getMultipartFromBody(multipartFromParameters);
        }

        // application/x-www-form-urlencoded表单优先级最低
        Map<String, Object> fromParameters = requestParameter.getFormParameters();
        if (ContainerUtils.isNotEmptyMap(fromParameters)) {
            return getFormBody(fromParameters);
        }

        return HttpMethod.requiresRequestBody(request.getRequestMethod().toString())
                ? new FormBody.Builder().build()
                : null;
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
    private RequestBody getMultipartFromBody(Map<String, Object> nameValuesMap) throws IOException {
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
                    builder.addFormDataPart(paramName, httpFile.getFileName(), new InputStreamRequestBody(mediaType, in));
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
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        headerChanger(httpHeaderManager, okhttpResponse.headers());
        processor.process(new ResponseMetaData(
                request,
                okhttpResponse.code(),
                httpHeaderManager,
                getResponseInputStreamSource(okhttpResponse)
        ));
    }

    private InputStreamSource getResponseInputStreamSource(okhttp3.Response okhttpResponse) {
        ResponseBody responseBody = okhttpResponse.body();
        if (responseBody != null) {
            return () -> new ResponseInputStream(responseBody.byteStream(), okhttpResponse);
        }
        return () -> new ResponseInputStream(EMPTY_INPUT_STREAM, okhttpResponse);
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

    static class InputStreamRequestBody extends RequestBody {

        private final MediaType mediaType;
        private final InputStream inputStream;

        InputStreamRequestBody(MediaType mediaType, InputStream inputStream) {
            this.mediaType = mediaType;
            this.inputStream = inputStream;
        }


        @Nullable
        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
            byte[] buffer = new byte[FileCopyUtils.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bufferedSink.write(buffer, 0, bytesRead);
            }
            bufferedSink.flush();
            inputStream.close();
        }
    }
}
