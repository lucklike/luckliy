package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestParameter;
import com.luckyframework.httpclient.core.meta.ResponseInputStream;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import com.luckyframework.httpclient.core.meta.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.core.meta.DefaultRequestParameter;
import com.luckyframework.httpclient.core.exception.NotFindRequestException;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 基本的基于JDK的Http客户端实现的执行器
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
        if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
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
        for (Map.Entry<String, List<Header>> entry : headerMap.entrySet()) {
            String name = entry.getKey();
            List<Header> valueList = entry.getValue();
            for (Header header : valueList) {
                Object headerValue = header.getValue();
                if (headerValue != null) {
                    switch (header.getHeaderType()) {
                        case ADD: {
                            connection.addRequestProperty(name, String.valueOf(headerValue));
                            break;
                        }
                        case SET: {
                            connection.setRequestProperty(name, String.valueOf(headerValue));
                            break;
                        }
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
        switch (request.getRequestMethod()) {
            case GET:
                getSetting(request, connection);
                break;
            case POST:
                postSetting(request, connection);
                break;
            case DELETE:
                deleteSetting(request, connection);
                break;
            case PUT:
                putSetting(request, connection);
                break;
            case OPTIONS:
                optionsSetting(request, connection);
                break;
            case HEAD:
                headSetting(request, connection);
                break;
            case TRACE:
                traceSetting(request, connection);
                break;
            default:
                throw new NotFindRequestException("Jdk HttpURLConnection does not support requests of type ['" + request.getRequestMethod() + "'].");
        }
    }

    /**
     * 设置为[GET]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void getSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("GET");
    }

    /**
     * 设置为[POST]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void postSetting(Request request, HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("POST");
        setRequestParameters(connection, request);
    }

    /**
     * 设置为[DELETE]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void deleteSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("DELETE");

    }

    /**
     * 设置为[PUT]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void putSetting(Request request, HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("PUT");
        setRequestParameters(connection, request);
    }

    /**
     * 设置为[OPTIONS]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void optionsSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("OPTIONS");
    }

    /**
     * 设置为[HEAD]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void headSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("HEAD");
    }

    /**
     * 设置为[TRACE]请求，并添加请求参数
     *
     * @param request    请求信息
     * @param connection Http连接
     */
    private void traceSetting(Request request, HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("TRACE");
    }

    /**
     * 设置具体的请求参数
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
            FileCopyUtils.copy(body.getBody(), connection.getOutputStream());
            return;
        }

        // multipart/form-data表单参数优先级其次
        if (ContainerUtils.isNotEmptyMap(multipartFromParameters)) {
            setMultipartFormData(connection, multipartFromParameters);
            return;
        }

        // form表单优先级最低
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

        int bufferSize = 1024 * 4;
        byte[] buffer = new byte[bufferSize];
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
