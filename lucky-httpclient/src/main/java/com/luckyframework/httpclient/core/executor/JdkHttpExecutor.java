package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.DefaultRequestParameter;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.ResponseInputStream;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 基本的基于JDK的{@link URLConnection}实现的Http执行器
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 3:08 下午
 */
public class JdkHttpExecutor implements HttpExecutor {

    private final String end = "\r\n";
    private final String twoHyphens = "--";
    private final String boundary = "LuckyBoundary";
    private final URLConnectionFactory connectionFactory;

    public JdkHttpExecutor(URLConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public JdkHttpExecutor() {
        this(request -> {
            URL url = new URL(request.getURI().toASCIIString());
            ProxyInfo proxyInfo = request.getProxyInfo();
            URLConnection connection = proxyInfo == null ? url.openConnection() : url.openConnection(proxyInfo.getProxy());
            if (connection instanceof HttpsURLConnection) {
                if (request.getHostnameVerifier() != null) {
                    ((HttpsURLConnection) connection).setHostnameVerifier(request.getHostnameVerifier());
                }
                if (request.getSSLSocketFactory() != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(request.getSSLSocketFactory());
                }
            }
            return connection;
        });
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) connectionFactory.createURLConnection(request);
        connectionConfigSetting(connection, request);
        connectionHeaderSetting(connection, request);
        connectionParamsSetting(connection, request);
        connection.connect();
        int code = connection.getResponseCode();
        HttpHeaderManager httpHeaderManager = getHttpHeaderManager(connection);
        processor.process(
                new ResponseMetaData(
                        request,
                        code,
                        httpHeaderManager,
                        getResponseInputStreamSource(connection, code)
                )
        );
    }

    private InputStreamSource getResponseInputStreamSource(HttpURLConnection connection, int code) {
        if (code < HttpURLConnection.HTTP_BAD_REQUEST) {
            return () -> new ResponseInputStream(connection.getInputStream(), connection::disconnect);
        }
        return () -> new ResponseInputStream(connection.getErrorStream(), connection::disconnect);
    }

    @NonNull
    private HttpHeaderManager getHttpHeaderManager(HttpURLConnection connection) {
        HttpHeaderManager httpHeaderManager = new DefaultHttpHeaderManager();
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String name = entry.getKey();
            if (name == null) continue;
            List<String> valueList = entry.getValue();
            for (String value : valueList) {
                httpHeaderManager.putHeader(isoToUTF8(name), isoToUTF8(value));
            }
        }
        return httpHeaderManager;
    }

    private String isoToUTF8(String isoStr) {
        if (isoStr == null) {
            return null;
        }
        return new String(isoStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }


    /***
     * 连接设置
     * @param connection HTTP连接
     */
    private void connectionConfigSetting(HttpURLConnection connection, Request request) {
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(Request.DEF_CONNECTION_TIME_OUT);
        connection.setReadTimeout(Request.DEF_READ_TIME_OUT);
        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        if (connectTimeout != null && connectTimeout > 0) {
            connection.setConnectTimeout(connectTimeout);
        }
        if (readTimeout != null && readTimeout > 0) {
            connection.setReadTimeout(readTimeout);
        }

    }

    /**
     * 请求头设置
     *
     * @param connection Http连接
     * @param request    请求
     */
    protected void connectionHeaderSetting(HttpURLConnection connection, Request request) {
        Map<String, List<Header>> headerMap = request.getHeaderMap();
        headerMap.forEach((name, headers) -> addHeaders(name, headers, connection));
    }


    /**
     * 添加请求头
     *
     * @param headerName 请求头名称
     * @param headerList 请求头值
     * @param connection HTTP连接
     */
    private void addHeaders(String headerName, List<Header> headerList, HttpURLConnection connection) {
        if (ContainerUtils.isEmptyCollection(headerList)) {
            return;
        }
        for (Header header : headerList) {
            Object headerValue = header.getValue();
            if (headerValue != null) {
                switch (header.getHeaderType()) {
                    case ADD: {
                        connection.addRequestProperty(headerName, String.valueOf(headerValue));
                        break;
                    }
                    case SET: {
                        connection.setRequestProperty(headerName, String.valueOf(headerValue));
                        break;
                    }
                }
            }
        }
    }


    /**
     * 请求参数设置以及请求类型确认
     *
     * @param connection Http连接
     * @param request    请求信息
     * @throws IOException IO异常
     */
    protected void connectionParamsSetting(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod(request.getRequestMethod().toString());
        setRequestParameters(connection, request);
    }

    /**
     * 设置具体的请求参数
     * <pre>
     *     1. 如果Lucky请求中存在{@link BodyObject}对象，则优先使用该对象作为请求体
     *     2. 如果Lucky请求中存在multipart/form-data的表单参数，则使用该表单作为请求体
     *     3. 如果Lucky请求中存在application/x-www-form-urlencoded的表单参数，则使用该表单作为请求体
     * </pre>
     *
     * @param connection Http连接
     * @param request    请求信息
     */
    private void setRequestParameters(HttpURLConnection connection, Request request) throws IOException {
        RequestParameter requestParameter = request.getRequestParameter();
        BodyObject body = requestParameter.getBody();
        Map<String, Object> fromParameters = requestParameter.getFormParameters();
        Map<String, Object> multipartFromParameters = requestParameter.getMultipartFormParameters();
        //如果设置了Body参数，则优先使用Body参数
        if (body != null) {
            connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, body.getContentType().toString());
            FileCopyUtils.copy(body.getBodyStream(), connection.getOutputStream());
            return;
        }

        // multipart/form-data表单参数优先级其次
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            setMultipartFormData(connection, multipartFromParameters);
            return;
        }

        // application/x-www-form-urlencoded表单优先级最低
        if (ContainerUtils.isNotEmptyMap(fromParameters)) {
            setFormParam(connection, request);
        }
    }

    private void setFormParam(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8;");
        DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
        ds.writeBytes(((DefaultRequestParameter) request.getRequestParameter()).getUrlencodedParameterString());
        ds.flush();
        ds.close();
    }

    private void setMultipartFormData(HttpURLConnection connection, Map<String, Object> multipartFromDataMap) throws IOException {
        connection.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);
        DataOutputStream ds = new DataOutputStream(connection.getOutputStream());
        for (Map.Entry<String, Object> paramEntry : multipartFromDataMap.entrySet()) {
            String paramName = paramEntry.getKey();
            Object paramValue = paramEntry.getValue();

            // 资源类型参数
            if (HttpExecutor.isResourceParam(paramValue)) {
                HttpFile[] httpFiles = HttpExecutor.toHttpFiles(paramValue);
                for (HttpFile httpFile : httpFiles) {
                    InputStream inputStream = httpFile.getInputStream();
                    writerFileData(ds, paramName, httpFile.getFileName(), inputStream);
                }
            }
            //其他类型将会被当做String类型的参数
            else {
                ds.writeBytes(twoHyphens + boundary + end);
                ds.write(("Content-Disposition: form-data; " + "name=\"" + paramName + "\"" + end + end).getBytes(StandardCharsets.UTF_8));
                ds.write(String.valueOf(paramValue).getBytes(StandardCharsets.UTF_8));
                ds.writeBytes(end);
            }
        }
        ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
        ds.flush();
        ds.close();
    }

    /**
     * 将具体的文件参数写入请求体中
     *
     * @param ds          数据输出流
     * @param name        参数名
     * @param fileName    文件名
     * @param inputStream 文件的输入流
     */
    private void writerFileData(DataOutputStream ds, String name, String fileName, InputStream inputStream) throws IOException {
        ds.writeBytes(twoHyphens + boundary + end);
        ds.write(("Content-Disposition: form-data; " + "name=\"" + name + "\"; filename=\"" + fileName + "\"" + end).getBytes(StandardCharsets.UTF_8));
        ds.write(("Content-Type: " + ContentTypeUtils.getMimeType(fileName) + end).getBytes(StandardCharsets.UTF_8));
        ds.writeBytes(end);

        byte[] buffer = new byte[FileCopyUtils.BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            ds.write(buffer, 0, length);
        }
        ds.writeBytes(end);
        ds.flush();
        inputStream.close();
    }

    /**
     * URL连接工厂
     */
    public interface URLConnectionFactory {

        /**
         * 创建一个URLConnection实例
         *
         * @param request 请求实例
         * @return URLConnection实例
         */
        URLConnection createURLConnection(Request request) throws IOException;
    }

}
