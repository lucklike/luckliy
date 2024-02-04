package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestParameter;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.exception.NotFindRequestException;
import com.luckyframework.web.ContentTypeUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于Apache Http Client 的HTTP客户端实现
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 3:10 下午
 */
public class HttpClientExecutor implements HttpExecutor {
    private final HttpClientBuilder builder;

    public HttpClientExecutor(HttpClientBuilder builder) {
        this.builder = builder;
    }

    public HttpClientExecutor() {
        builder = defaultHttpClientBuilder();
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        CloseableHttpClient client;
        CloseableHttpResponse response = null;
        try {

            client = getCurrentBuilder(request).build();
            HttpRequestBase httpRequestBase = createHttpClientRequest(request);
            requestConfigSetting(httpRequestBase, request);
            httpRequestSetting(httpRequestBase, request);
            response = client.execute(httpRequestBase);
            resultProcess(request, processor, response);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                throw new HttpExecutorException("An exception occurred when releasing resources after the request ended:" + request, e);
            }
        }
    }

    private synchronized HttpClientBuilder getCurrentBuilder(Request request) {
        builder.setSSLHostnameVerifier(request.getHostnameVerifier())
               .setSSLSocketFactory(new SSLConnectionSocketFactory(request.getSSLSocketFactory(), request.getHostnameVerifier()));
        return builder;

    }

    /**
     * 请求级别的超时时间设置
     *
     * @param httpRequestBase HttpClient请求
     * @param request         Lucky请求
     */
    private void requestConfigSetting(HttpRequestBase httpRequestBase, Request request) {
        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        Integer writerTimeout = request.getWriterTimeout();
        RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
        if (request.getProxy() != null) {
            InetSocketAddress address = (InetSocketAddress) request.getProxy().address();
            reqConfigBuilder.setProxy(new HttpHost(address.getHostName(), address.getPort()));
        }
        if (connectTimeout != null && connectTimeout > 0) {
            reqConfigBuilder.setConnectTimeout(connectTimeout);
        }
        if (readTimeout != null && readTimeout > 0) {
            reqConfigBuilder.setSocketTimeout(readTimeout);
        }
        if (writerTimeout != null && writerTimeout > 0) {
            reqConfigBuilder.setConnectionRequestTimeout(writerTimeout);
        }
        httpRequestBase.setConfig(reqConfigBuilder.build());
    }

    protected void httpRequestSetting(HttpRequestBase httpRequestBase, Request request) {
        doHeaderSetting(httpRequestBase, request);
    }

    /**
     * 设置请求头信息
     *
     * @param httpRequestBase Http Client需要的的请求
     * @param request         请求信息
     */
    protected void doHeaderSetting(HttpRequestBase httpRequestBase, Request request) {
        Map<String, List<com.luckyframework.httpclient.core.Header>> headerMap = request.getHeaderMap();
        for (Map.Entry<String, List<com.luckyframework.httpclient.core.Header>> entry : headerMap.entrySet()) {
            String headerName = entry.getKey();
            List<com.luckyframework.httpclient.core.Header> headerList = entry.getValue();
            if (!ContainerUtils.isEmptyCollection(headerList)) {
                for (com.luckyframework.httpclient.core.Header header : headerList) {
                    Object headerValue = header.getValue();
                    if (headerValue != null) {
                        switch (header.getHeaderType()) {
                            case ADD:
                                httpRequestBase.addHeader(headerName, headerValue.toString());
                                break;
                            case SET:
                                httpRequestBase.setHeader(headerName, headerValue.toString());
                                break;
                        }
                    }
                }
            }
        }
    }


    /**
     * 默认的HttpClientBuilder
     */
    protected HttpClientBuilder defaultHttpClientBuilder() {
        HttpClientBuilder builder = HttpClients.custom();
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(Request.DEF_CONNECTION_TIME_OUT);
        requestConfig.setConnectionRequestTimeout(5000);
        requestConfig.setSocketTimeout(Request.DEF_CONNECTION_TIME_OUT);

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setCharset(StandardCharsets.UTF_8).build());
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(30000)
                .setSoReuseAddress(true).build();
        connectionManager.setDefaultSocketConfig(socketConfig);
        builder.setConnectionManager(connectionManager);

        builder.setDefaultRequestConfig(requestConfig.build());

        return builder;
    }


    /**
     * 响应结果处理
     *
     * @param request   请求实例
     * @param processor 响应处理器
     * @param response  Apache HttpClient的{@link CloseableHttpResponse}
     */
    protected void resultProcess(Request request, ResponseProcessor processor, CloseableHttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        Header[] allHeaders = response.getAllHeaders();
        HttpEntity entity = response.getEntity();
        HttpHeaderManager httpHeaderManager = changeToLuckyHeader(allHeaders);
        processor.process(new ResponseMetaData(request, code, httpHeaderManager, entity::getContent));
    }

    /**
     * 将HttpClient的响应头信息转化为Lucky规范中的响应头
     *
     * @param allHeaders HttpClient的响应头信息
     * @return Lucky规范中的响应头
     */
    private HttpHeaderManager changeToLuckyHeader(Header[] allHeaders) {
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        for (Header header : allHeaders) {
            httpHeaderManager.putHeader(header.getName(), header.getValue());
        }
        return httpHeaderManager;
    }

    //--------------------------------------------------------------------------
    //                 Create Apache HttpClient Request
    //--------------------------------------------------------------------------

    /**
     * 使用Lucky的Request创建HttpClient所需要的Request
     *
     * @param request HttpClient的响应头信息
     * @return HttpClient所需要的Request
     */
    private HttpRequestBase createHttpClientRequest(Request request) throws IOException {
        switch (request.getRequestMethod()) {
            case GET:
                return createHttpGet(request);
            case POST:
                return createHttpPost(request);
            case DELETE:
                return createHttpDelete(request);
            case PUT:
                return createHttpPut(request);
            case HEAD:
                return createHttpHead(request);
            case OPTIONS:
                return createHttpOptions(request);
            case TRACE:
                return createHttpTrace(request);
            case PATCH:
                return createHttpPatch(request);
            default:
                throw new NotFindRequestException("Apache Http Client does not support requests of type ['" + request.getRequestMethod() + "'].");
        }
    }

    /**
     * 创建[PATCH]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [PATCH]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpPatch(Request request) throws IOException {
        HttpPatch patch = new HttpPatch(request.getUrl());
        HttpEntity entity = getHttpEntity(request);
        if (entity != null) {
            patch.setEntity(entity);
        }
        return patch;
    }

    /**
     * 创建[TRACE]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [TRACE]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpTrace(Request request) {
        return new HttpTrace(request.getUrl());
    }

    /**
     * 创建[OPTIONS]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [OPTIONS]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpOptions(Request request) {
        return new HttpOptions(request.getUrl());
    }

    /**
     * 创建[GET]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [GET]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpGet(Request request) {
        return new HttpGet(request.getUrl());
    }

    /**
     * 创建[POST]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [POST]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpPost(Request request) throws IOException {
        HttpPost post = new HttpPost(request.getUrl());
        HttpEntity entity = getHttpEntity(request);
        if (entity != null) {
            post.setEntity(entity);
        }
        return post;
    }

    /**
     * 创建[DELETE]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [DELETE]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpDelete(Request request) {
        return new HttpDelete(request.getUrl());
    }

    /**
     * 创建[PUT]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [PUT]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpPut(Request request) throws IOException {
        HttpPut put = new HttpPut(request.getUrl());
        HttpEntity entity = getHttpEntity(request);
        if (entity != null) {
            put.setEntity(entity);
        }
        return put;
    }

    /**
     * 创建[HEAD]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [HEAD]类型的HttpClient规范的请求
     */
    private HttpRequestBase createHttpHead(Request request) {
        return new HttpHead(request.getUrl());
    }

    /**
     * 获取HttpClient的请求体部分的参数
     *
     * @param request LuckyRequest
     * @return HttpClient的请求体部分的参数
     */
    private HttpEntity getHttpEntity(Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();
        BodyObject body = requestParameter.getBody();
        Map<String, Object> nameValuesMap = requestParameter.getRequestParameters();
        if (body != null) {
            return new StringEntity(body.getBody(), body.getContentType().getCharset());
        }

        if (ContainerUtils.isEmptyMap(nameValuesMap)) {
            return null;
        }

        return HttpExecutor.isFileRequest(nameValuesMap) ? getFileHttpEntity(nameValuesMap) : getUrlEncodedFormEntity(nameValuesMap);
    }


    /**
     * 包装文件类型的参数
     *
     * @param multipartFileParams 文件类型的参数
     * @return 包装文件类型的参数
     */
    private HttpEntity getFileHttpEntity(Map<String, Object> multipartFileParams) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);
        //加上此行代码解决返回中文乱码问题
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, Object> paramEntry : multipartFileParams.entrySet()) {
            String paramName = paramEntry.getKey();
            Object paramValue = paramEntry.getValue();

            // 资源文件参数处理
            if (HttpExecutor.isResourceParam(paramValue)) {
                HttpFile[] httpFiles = HttpExecutor.toHttpFiles(paramValue);
                for (HttpFile httpFile : httpFiles) {
                    InputStream inputStream = httpFile.getInputStream();
                    String fileName = httpFile.getFileName();
                    String mimeType = ContentTypeUtils.getMimeType(fileName);
                    ContentType contentType = mimeType == null ? ContentType.TEXT_PLAIN : ContentType.create(mimeType);
                    builder.addBinaryBody(paramName, inputStream, contentType, fileName);
                }
            }
            // 其他类型将会被当做String类型的参数
            else {
                builder.addTextBody(paramName, String.valueOf(paramValue), ContentType.create("text/plain", Consts.UTF_8));
            }
        }
        return builder.build();
    }


    /**
     * 得到非文件类型的请求的参数
     *
     * @param params 参数Map
     * @return URL编码后的表单参数
     */
    private static HttpEntity getUrlEncodedFormEntity(Map<String, Object> params) {
        List<BasicNameValuePair> list = new ArrayList<>();
        for (Map.Entry<String, Object> paramEntry : params.entrySet()) {
            list.add(new BasicNameValuePair(paramEntry.getKey(), String.valueOf(paramEntry.getValue())));
        }
        return new UrlEncodedFormEntity(list, StandardCharsets.UTF_8);
    }


}
