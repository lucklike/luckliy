package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.ResponseInputStream;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.core.meta.Version;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.web.ContentTypeUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.core.io.InputStreamSource;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_CONNECTION_REQUEST_TIMEOUT;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_CONNECTION_TIMEOUT;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_KEEP_ALIVE_DURATION;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_MAX_PER_ROUTE;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_MAX_TOTAL;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_RESPONSE_TIMEOUT;
import static com.luckyframework.httpclient.core.executor.Constant.DEFAULT_VALIDATE_AFTER_INACTIVITY;
import static com.luckyframework.httpclient.core.executor.Constant.HTTP_CLIENT_CONTEXT_REQUEST;

/**
 * 基于Apache Http Client 5.x 的HTTP客户端实现
 *
 * @author fk7075
 * @version 2.0
 * @date 2021/9/3 3:10 下午
 */
public class HttpClient5Executor implements HttpExecutor {

    private final Version defaultVersion;
    private final CloseableHttpClient httpClient;
    private final Map<Version, ProtocolVersion> httpVersionMap = new HashMap<>();

    {
        httpVersionMap.put(Version.HTTP_1_1, HttpVersion.HTTP_1_1);
        httpVersionMap.put(Version.HTTP_1_0, HttpVersion.HTTP_1_0);
        httpVersionMap.put(Version.HTTP_2, HttpVersion.HTTP_2);
    }

    public HttpClient5Executor(HttpClientBuilder builder, Version defaultVersion) {
        this.httpClient = builder.build();
        this.defaultVersion = defaultVersion;
    }

    public HttpClient5Executor(HttpClientBuilder builder) {
        this(builder, Version.NON);
    }

    public HttpClient5Executor(int connectionRequestTimeout,
                               int connectionTimeout,
                               int responseTimeout,
                               int validateAfterInactivity,
                               int maxTotal,
                               int maxPerRoute,
                               long keepAliveDuration,
                               TimeUnit timeUnit,
                               Version defaultVersion) {
        this.httpClient = defaultHttpClientBuilder(
                connectionRequestTimeout,
                connectionTimeout,
                responseTimeout,
                validateAfterInactivity,
                maxTotal,
                maxPerRoute,
                keepAliveDuration,
                timeUnit
        ).build();
        this.defaultVersion = defaultVersion;
    }

    public HttpClient5Executor() {
        this(DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                DEFAULT_CONNECTION_TIMEOUT,
                DEFAULT_RESPONSE_TIMEOUT,
                DEFAULT_VALIDATE_AFTER_INACTIVITY,
                DEFAULT_MAX_TOTAL,
                DEFAULT_MAX_PER_ROUTE,
                DEFAULT_KEEP_ALIVE_DURATION,
                TimeUnit.MINUTES,
                Version.NON
        );
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        DynamicHttpRequest httpRequest = new DynamicHttpRequest(request);
        ProtocolVersion useHttpClientHttpVersion = getUseHttpClientHttpVersion(request);
        if (useHttpClientHttpVersion != null) {
            httpRequest.setVersion(useHttpClientHttpVersion);
        }
        CloseableHttpResponse response = httpClient.execute(httpRequest, createHttpClientContext(request));
        resultProcess(request, processor, response);
    }

    @Override
    public String getHttpVersionString(Request request) {
        ProtocolVersion useHttpClientVersion = getUseHttpClientHttpVersion(request);
        if (useHttpClientVersion == null) {
            return HttpExecutor.super.getHttpVersionString(request);
        }
        return request.getHttpVersion().getVersionStr();
    }

    private HttpClientContext createHttpClientContext(Request request) {
        HttpClientContext context = HttpClientContext.create();

        // 设置请求级别的超时配置
        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        Integer writerTimeout = request.getWriterTimeout();

        RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
        ProxyInfo proxyInfo = request.getProxyInfo();
        if (proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.HTTP) {
            InetSocketAddress address = (InetSocketAddress) proxyInfo.getProxy().address();
            final HttpHost proxy = new HttpHost(address.getHostName(), address.getPort());
            proxyInfo.setHttpAuthenticator(request);
            reqConfigBuilder.setProxy(proxy);
        }
        if (connectTimeout != null && connectTimeout > 0) {
            reqConfigBuilder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout));
        }
        if (readTimeout != null && readTimeout > 0) {
            reqConfigBuilder.setResponseTimeout(Timeout.ofMilliseconds(readTimeout));
        }
        if (writerTimeout != null && writerTimeout > 0) {
            reqConfigBuilder.setConnectionRequestTimeout(Timeout.ofMilliseconds(writerTimeout));
        }
        context.setRequestConfig(reqConfigBuilder.build());
        context.setAttribute(HTTP_CLIENT_CONTEXT_REQUEST, request);
        return context;
    }

    private Request getRequestByHttpContext(HttpContext context) {
        Object request = context.getAttribute(HTTP_CLIENT_CONTEXT_REQUEST);
        if (request == null) {
            throw new HttpExecutorException("Current Lucky request is NULL in HttpContext! Make sure createHttpClientContext() is called before executing the request.");
        }
        if (!(request instanceof Request)) {
            throw new HttpExecutorException("Invalid request object in HttpContext: " + request.getClass());
        }
        return (Request) request;
    }

    /**
     * 添加请求头
     *
     * @param headerName  请求头名称
     * @param headerList  请求头值
     * @param httpRequest Http请求对象
     */
    private void addHeaders(String headerName, List<Header> headerList, org.apache.hc.client5.http.classic.methods.HttpUriRequestBase httpRequest) {
        if (ContainerUtils.isEmptyCollection(headerList)) {
            return;
        }
        for (Header header : headerList) {
            Object headerValue = header.getValue();
            if (headerValue != null) {
                switch (header.getHeaderType()) {
                    case ADD:
                        httpRequest.addHeader(new BasicHeader(headerName, headerValue.toString()));
                        break;
                    case SET:
                        httpRequest.setHeader(headerName, headerValue.toString());
                        break;
                }
            }
        }
    }

    /**
     * 默认的HttpClientBuilder
     */
    protected HttpClientBuilder defaultHttpClientBuilder(int connectionRequestTimeout,
                                                         int connectionTimeout,
                                                         int responseTimeout,
                                                         int validateAfterInactivity,
                                                         int maxTotal,
                                                         int maxPerRoute,
                                                         long keepAliveDuration,
                                                         TimeUnit timeUnit) {
        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setRedirectsEnabled(false)
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout))
                        .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                        .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                        .setCookieSpec(StandardCookieSpec.IGNORE).build())
                .setConnectionManager(new HttpClientConnectionManagerFactory(validateAfterInactivity, maxTotal, maxPerRoute, keepAliveDuration, timeUnit).getHttpClientConnectionManager())
                .evictIdleConnections(Timeout.of(keepAliveDuration, timeUnit))
                .evictExpiredConnections();
    }

    /**
     * 响应结果处理
     *
     * @param request   请求实例
     * @param processor 响应处理器
     * @param response  Apache HttpClient的{@link CloseableHttpResponse}
     */
    protected void resultProcess(Request request, ResponseProcessor processor, CloseableHttpResponse response) throws Exception {
        org.apache.hc.core5.http.Header[] allHeaders = response.getHeaders();
        HttpHeaderManager httpHeaderManager = changeToLuckyHeader(allHeaders);
        processor.process(
                new ResponseMetaData(
                        request,
                        response.getCode(),
                        httpHeaderManager,
                        getResponseInputStreamSource(response)
                )
        );
    }

    private InputStreamSource getResponseInputStreamSource(CloseableHttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            return () -> new ResponseInputStream(entity.getContent(), response);
        }
        return () -> new ResponseInputStream(EMPTY_INPUT_STREAM, response);
    }

    /**
     * 将HttpClient的响应头信息转化为Lucky规范中的响应头
     *
     * @param allHeaders HttpClient的响应头信息
     * @return Lucky规范中的响应头
     */
    private HttpHeaderManager changeToLuckyHeader(org.apache.hc.core5.http.Header[] allHeaders) {
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        for (org.apache.hc.core5.http.Header header : allHeaders) {
            httpHeaderManager.putHeader(header.getName(), header.getValue());
        }
        return httpHeaderManager;
    }

    /**
     * 获取HttpClient的请求体部分的参数
     * <pre>
     *     1. 如果Lucky请求中存在{@link BodyObject}对象，则优先使用该对象作为请求体
     *     2. 如果Lucky请求中存在multipart/form-data的表单参数，则使用该表单作为请求体
     *     3. 如果Lucky请求中存在application/x-www-form-urlencoded的表单参数，则使用该表单作为请求体
     * </pre>
     *
     * @param request LuckyRequest
     * @return HttpClient的请求体部分的参数
     */
    private HttpEntity getHttpEntity(Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();
        BodyObject body = requestParameter.getBody();
        Map<String, Object> fromParameters = requestParameter.getFormParameters();
        Map<String, Object> multipartFromParameters = requestParameter.getMultipartFormParameters();

        //如果设置了Body参数，则优先使用Body参数
        if (body != null) {
            return new InputStreamEntity(body.getBodyStream(), ContentType.parse(body.getContentType().toString()));
        }

        // multipart/form-data表单参数优先级其次
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            return getMultipartFormEntity(multipartFromParameters);
        }

        // application/x-www-form-urlencoded表单优先级最低
        if (ContainerUtils.isNotEmptyMap(fromParameters)) {
            return getUrlEncodedFormEntity(fromParameters);
        }

        return null;
    }

    /**
     * 包装文件类型的参数
     *
     * @param multipartFileParams 文件类型的参数
     * @return 包装文件类型的参数
     */
    private HttpEntity getMultipartFormEntity(Map<String, Object> multipartFileParams) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);
        //加上此行代码解决返回中文乱码问题
        builder.setMode(HttpMultipartMode.LEGACY);
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
                    ContentType contentType = mimeType == null ? ContentType.TEXT_PLAIN : ContentType.parse(mimeType);
                    builder.addBinaryBody(paramName, inputStream, contentType, fileName);
                }
            }
            // 其他类型将会被当做String类型的参数
            else {
                builder.addTextBody(paramName, String.valueOf(paramValue), ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
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

    private ProtocolVersion getUseHttpClientHttpVersion(Request request) {
        Version version = request.getHttpVersion();
        if (version == null || version == Version.NON) {
            version = defaultVersion;
        }
        return httpVersionMap.get(request.getHttpVersion());
    }

    /**
     * 支持设置请求体的动态HTTP请求
     */
    public class DynamicHttpRequest extends HttpUriRequestBase {

        public DynamicHttpRequest(final Request request) throws IOException {
            super(request.getRequestMethod().toString(), URI.create(request.getURI().toASCIIString()));

            // 设置请求头
            request.getHeaderMap().forEach((name, headers) -> addHeaders(name, headers, this));

            // 设置请求体
            HttpEntity entity = getHttpEntity(request);
            Optional.ofNullable(entity).ifPresent(this::setEntity);
        }
    }

    /**
     * 连接工厂
     */
    class LuckyConnectionFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(org.apache.hc.core5.http.protocol.HttpContext context) throws IOException {
            final Request request = getRequestByHttpContext(context);
            final ProxyInfo proxyInfo = request.getProxyInfo();
            if (proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.SOCKS) {
                return new Socket(proxyInfo.getProxy());
            }
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            final Request request = getRequestByHttpContext(context);
            final ProxyInfo proxyInfo = request.getProxyInfo();
            final InetSocketAddress address = proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.SOCKS
                    ? InetSocketAddress.createUnresolved(host.getHostName(), host.getPort())
                    : remoteAddress;
            return super.connectSocket(connectTimeout, socket, host, address, localAddress, context);
        }
    }

    /**
     * SSL连接工厂
     */
    class LuckySSLConnectionFactory implements LayeredConnectionSocketFactory {

        private final SSLConnectionSocketFactory defaultSocketFactory = SSLConnectionSocketFactory.getSocketFactory();

        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            final Request request = getRequestByHttpContext(context);
            final ProxyInfo proxyInfo = request.getProxyInfo();
            if (proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.SOCKS) {
                return new Socket(proxyInfo.getProxy());
            }
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
            Request request = getRequestByHttpContext(context);
            final ProxyInfo proxyInfo = request.getProxyInfo();
            InetSocketAddress address = proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.SOCKS
                    ? InetSocketAddress.createUnresolved(host.getHostName(), host.getPort())
                    : remoteAddress;
            return getSslConnectionSocketFactory(request).connectSocket(connectTimeout, socket, host, address, localAddress, context);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException {
            Request request = getRequestByHttpContext(context);
            return getSslConnectionSocketFactory(request).createLayeredSocket(socket, target, port, context);
        }

        /**
         * 根据当前请求构建独立的SSLConnectionSocketFactory
         *
         * @param request 当前请求
         * @return SSLConnectionSocketFactory
         */
        private SSLConnectionSocketFactory getSslConnectionSocketFactory(Request request) {
            SSLSocketFactory sslSocketFactory = request.getSSLSocketFactory();
            return sslSocketFactory == null
                    ? defaultSocketFactory
                    : new SSLConnectionSocketFactory(sslSocketFactory, request.getHostnameVerifier());
        }
    }

    /**
     * 连接管理器工厂
     */
    public class HttpClientConnectionManagerFactory {

        private final int validateAfterInactivity;
        private final int maxTotal;
        private final int maxPerRoute;
        private final long keepAliveDuration;
        private final TimeUnit timeUnit;


        public HttpClientConnectionManagerFactory(int validateAfterInactivity, int maxTotal, int maxPerRoute, long keepAliveDuration, TimeUnit timeUnit) {
            this.validateAfterInactivity = validateAfterInactivity;
            this.maxTotal = maxTotal;
            this.maxPerRoute = maxPerRoute;
            this.keepAliveDuration = keepAliveDuration;
            this.timeUnit = timeUnit;
        }

        public HttpClientConnectionManager getHttpClientConnectionManager() {
            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", new LuckyConnectionFactory())
                            .register("https", new LuckySSLConnectionFactory())
                            .build();
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connectionManager.setMaxTotal(maxTotal);
            connectionManager.setDefaultMaxPerRoute(maxPerRoute);

            // 设置连接配置（HttpClient5 使用 ConnectionConfig）
            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setValidateAfterInactivity(Timeout.ofMilliseconds(validateAfterInactivity))
                    .setTimeToLive(keepAliveDuration, timeUnit)
                    .build();
            connectionManager.setDefaultConnectionConfig(connectionConfig);
            return connectionManager;
        }
    }
}