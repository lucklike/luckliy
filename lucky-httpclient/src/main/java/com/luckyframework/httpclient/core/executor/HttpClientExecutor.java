package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.ResponseInputStream;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.web.ContentTypeUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Apache Http Client 的HTTP客户端实现
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 3:10 下午
 */
public class HttpClientExecutor implements HttpExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpClientExecutor.class);
    private static final String HTTP_CLIENT_CONTEXT_REQUEST = "__REQUEST__";

    private final HttpClientBuilder builder;

    public HttpClientExecutor(HttpClientBuilder builder) {
        this.builder = builder;
    }

    public HttpClientExecutor() {
        this(10, 5, TimeUnit.MINUTES);
    }

    public HttpClientExecutor(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        builder = defaultHttpClientBuilder(maxIdleConnections, keepAliveDuration, timeUnit);
    }


    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        CloseableHttpClient client = builder.build();
        HttpRequestBase httpRequestBase = new DynamicHttpRequest(request);
        requestConfigSetting(httpRequestBase, request);
        httpRequestSetting(httpRequestBase, request);
        CloseableHttpResponse response = client.execute(httpRequestBase, createHttpClientContext(request));
        resultProcess(request, processor, response);
    }

    private HttpClientContext createHttpClientContext(Request request) {
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(HTTP_CLIENT_CONTEXT_REQUEST, request);
        return HttpClientContext.adapt(httpContext);
    }

    private Request getRequestByHttpContext(HttpContext context) {
        Object request = context.getAttribute(HTTP_CLIENT_CONTEXT_REQUEST);
        if (request == null) {
            throw new HttpExecutorException("Current Lucky request is NULL!");
        }
        return (Request) request;
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
        ProxyInfo proxyInfo = request.getProxyInfo();
        if (proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.HTTP) {
            InetSocketAddress address = (InetSocketAddress) proxyInfo.getProxy().address();
            final HttpHost proxy = new HttpHost(address.getHostName(), address.getPort());
            proxyInfo.setHttpAuthenticator(request);
            reqConfigBuilder.setProxy(proxy);
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
        Map<String, List<com.luckyframework.httpclient.core.meta.Header>> headerMap = request.getHeaderMap();
        for (Map.Entry<String, List<com.luckyframework.httpclient.core.meta.Header>> entry : headerMap.entrySet()) {
            String headerName = entry.getKey();
            List<com.luckyframework.httpclient.core.meta.Header> headerList = entry.getValue();
            if (!ContainerUtils.isEmptyCollection(headerList)) {
                for (com.luckyframework.httpclient.core.meta.Header header : headerList) {
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
    protected HttpClientBuilder defaultHttpClientBuilder(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        HttpClientBuilder builder = HttpClients.custom();
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setRedirectsEnabled(false);
        requestConfig.setConnectTimeout(Request.DEF_CONNECTION_TIME_OUT);
        requestConfig.setSocketTimeout(Request.DEF_READ_TIME_OUT);
        requestConfig.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
        builder.setConnectionManager(new HttpClientConnectionManagerFactory(maxIdleConnections, keepAliveDuration, timeUnit).getHttpClientConnectionManager());
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
    protected void resultProcess(Request request, ResponseProcessor processor, CloseableHttpResponse response) throws Exception {
        Header[] allHeaders = response.getAllHeaders();
        HttpHeaderManager httpHeaderManager = changeToLuckyHeader(allHeaders);
        processor.process(
                new ResponseMetaData(
                        request,
                        response.getStatusLine().getStatusCode(),
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
    private HttpHeaderManager changeToLuckyHeader(Header[] allHeaders) {
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        for (Header header : allHeaders) {
            httpHeaderManager.putHeader(header.getName(), header.getValue());
        }
        return httpHeaderManager;
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
        Map<String, Object> fromParameters = requestParameter.getFormParameters();
        Map<String, Object> multipartFromParameters = requestParameter.getMultipartFormParameters();

        //如果设置了Body参数，则优先使用Body参数
        if (body != null) {
            return new InputStreamEntity(body.getBodyStream(), ContentType.create(body.getContentType().getMimeType(), body.getCharset()));
        }

        // multipart/form-data表单参数优先级其次
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            return getFileHttpEntity(multipartFromParameters);
        }

        // form表单优先级最低
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

    /**
     * 支持设置请求体的动态HTTP请求
     */
    public class DynamicHttpRequest extends HttpEntityEnclosingRequestBase {

        public final String METHOD_NAME;

        public DynamicHttpRequest(final Request request) throws IOException {
            super();
            setURI(request.getURI());
            this.METHOD_NAME = request.getRequestMethod().toString();
            HttpEntity entity = getHttpEntity(request);
            if (entity != null) {
                setEntity(entity);
            }
        }


        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    /**
     * 连接工厂
     */
    class LuckyConnectionFactory extends PlainConnectionSocketFactory {
        @Override
        public Socket createSocket(HttpContext context) throws IOException {
            final Request request = getRequestByHttpContext(context);
            final ProxyInfo proxyInfo = request.getProxyInfo();
            if (proxyInfo != null && proxyInfo.getProxy().type() == Proxy.Type.SOCKS) {
                return new Socket(proxyInfo.getProxy());
            }
            return super.createSocket(context);
        }

        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
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
        public Socket connectSocket(final int connectTimeout, final Socket socket, final HttpHost host, final InetSocketAddress remoteAddress, final InetSocketAddress localAddress, final HttpContext context) throws IOException {
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

        private String[][] getSupportedSSLProtocolsAndCipherSuites(Request request) {
            try {
                SSLSocketFactory sslFactory = request.getSSLSocketFactory();
                SSLSocket socket = (SSLSocket) sslFactory.createSocket();
                String[] protocols = socket.getSupportedProtocols();
                String[] cipherSuites = socket.getSupportedCipherSuites();
                return new String[][]{protocols, cipherSuites};
            } catch (IOException e) {
                throw new HttpExecutorException(e);
            }
        }

        /**
         * 根据当前请求构建独立的SSLConnectionSocketFactory
         *
         * @param request 当前请求
         * @return
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

        private final int maxIdleConnections;
        private final long keepAliveDuration;
        private final TimeUnit timeUnit;

        public HttpClientConnectionManagerFactory(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
            this.maxIdleConnections = maxIdleConnections;
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
            connectionManager.setMaxTotal(maxIdleConnections);
            connectionManager.closeIdleConnections(keepAliveDuration, timeUnit);
            connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(StandardCharsets.UTF_8).build());
            connectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
            connectionManager.setValidateAfterInactivity(60);
            SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(30000).setSoReuseAddress(true).build();
            connectionManager.setDefaultSocketConfig(socketConfig);
            return connectionManager;
        }
    }

}
