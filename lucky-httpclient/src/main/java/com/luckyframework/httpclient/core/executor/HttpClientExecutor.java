package com.luckyframework.httpclient.core.executor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.*;
import com.luckyframework.httpclient.core.impl.DefaultHttpHeaderManager;
import com.luckyframework.httpclient.exception.NotFindRequestException;
import com.luckyframework.io.MultipartFile;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        builder = HttpClients.custom();
        httpClientSetting(builder);
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = builder.build();
            HttpRequestBase httpRequestBase = createHttpClientRequest(request);
            timeOutSetting(httpRequestBase, request);
            httpRequestSetting(httpRequestBase, request);
            response = client.execute(httpRequestBase);
            resultProcess(processor, response);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                throw new HttpExecutorException("An exception occurred when releasing resources after the request ended:" + request, e);
            }
        }
    }

    /**
     * 请求级别的超时时间设置
     *
     * @param httpRequestBase HttpClient请求
     * @param request         Lucky请求
     */
    private void timeOutSetting(HttpRequestBase httpRequestBase, Request request) {
        Integer connectTimeout = request.getConnectTimeout();
        Integer readTimeout = request.getReadTimeout();
        if (connectTimeout != null || readTimeout != null) {
            RequestConfig.Builder builder = RequestConfig.custom();
            if (connectTimeout != null) {
                builder.setConnectTimeout(connectTimeout);
            }
            if (readTimeout != null) {
                builder.setSocketTimeout(readTimeout);
            }
            httpRequestBase.setConfig(builder.build());
        }

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
     * 设置默认的连接参数
     *
     * @param builder HttpClient构建器
     */
    protected void httpClientSetting(HttpClientBuilder builder) {
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(Request.DEF_CONNECTION_TIME_OUT);
        requestConfig.setConnectionRequestTimeout(5000);
        requestConfig.setSocketTimeout(Request.DEF_CONNECTION_TIME_OUT);
        requestConfig.setStaleConnectionCheckEnabled(true);
        builder.setDefaultRequestConfig(requestConfig.build());

    }

    /**
     * 将HttpClient的响应体转化为Lucky规范的响应
     *
     * @param response HttpClient的响应体
     * @return Lucky规范的响应
     * @throws IOException
     */
    protected void resultProcess(ResponseProcessor processor, CloseableHttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        Header[] allHeaders = response.getAllHeaders();
        HttpEntity entity = response.getEntity();
        HttpHeaderManager httpHeaderManager = changeToLuckyHeader(allHeaders);
        processor.process(code, httpHeaderManager, entity::getContent);
    }

    /**
     * 将HttpClient的响应头信息转化为Lucky规范中的响应头
     *
     * @param allHeaders HttpClient的响应头信息
     * @return
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
     * @return
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
     * @throws IOException
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
     * @throws IOException
     */
    private HttpRequestBase createHttpTrace(Request request) {
        return new HttpTrace(request.getUrl());
    }

    /**
     * 创建[OPTIONS]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [OPTIONS]类型的HttpClient规范的请求
     * @throws IOException
     */
    private HttpRequestBase createHttpOptions(Request request) {
        return new HttpOptions(request.getUrl());
    }

    /**
     * 创建[GET]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [GET]类型的HttpClient规范的请求
     * @throws IOException
     */
    private HttpRequestBase createHttpGet(Request request) {
        return new HttpGet(request.getUrl());
    }

    /**
     * 创建[POST]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [POST]类型的HttpClient规范的请求
     * @throws IOException
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
     * @throws IOException
     */
    private HttpRequestBase createHttpDelete(Request request) {
        return new HttpDelete(request.getUrl());
    }

    /**
     * 创建[PUT]类型的HttpClient规范的请求，并设置请求参数
     *
     * @param request LuckyRequest
     * @return [PUT]类型的HttpClient规范的请求
     * @throws IOException
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
     * @throws IOException
     */
    private HttpRequestBase createHttpHead(Request request) {
        return new HttpHead(request.getUrl());
    }

    /**
     * 获取HttpClient的请求体部分的参数
     *
     * @param request LuckyRequest
     * @return
     * @throws IOException
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

        return isFileRequest(nameValuesMap) ? getFileHttpEntity(nameValuesMap) : getUrlEncodedFormEntity(nameValuesMap);
    }


    /**
     * 包装文件类型的参数
     *
     * @param multipartFileParams 文件类型的参数
     * @return
     * @throws FileNotFoundException
     */
    private static HttpEntity getFileHttpEntity(Map<String, Object> multipartFileParams) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(StandardCharsets.UTF_8);
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);//加上此行代码解决返回中文乱码问题
        for (Map.Entry<String, Object> e : multipartFileParams.entrySet()) {
            Class<?> paramValueClass = e.getValue().getClass();
            //包装File类型的参数
            if (File.class == paramValueClass) {
                File file = (File) e.getValue();
                //文件参数-File
                builder.addBinaryBody(e.getKey(), Files.newInputStream(file.toPath()), ContentType.MULTIPART_FORM_DATA, file.getName());
            }
            //包装File[]类型的参数
            else if (File[].class == paramValueClass) {
                File[] files = (File[]) e.getValue();
                for (File file : files) {
                    //文件参数-File[]
                    builder.addBinaryBody(e.getKey(), Files.newInputStream(file.toPath()), ContentType.MULTIPART_FORM_DATA, file.getName());
                }
            }
            //包装MultipartFile类型的参数
            else if (MultipartFile.class == paramValueClass) {
                MultipartFile mf = (MultipartFile) e.getValue();
                builder.addBinaryBody(e.getKey(), mf.getInputStream(), ContentType.MULTIPART_FORM_DATA, mf.getFileName());
            }
            //包装MultipartFile[]类型的参数
            else if (MultipartFile[].class == paramValueClass) {
                MultipartFile[] mfs = (MultipartFile[]) e.getValue();
                for (MultipartFile mf : mfs) {
                    builder.addBinaryBody(e.getKey(), mf.getInputStream(), ContentType.MULTIPART_FORM_DATA, mf.getFileName());
                }
            }
            //包装Resource类型的参数
            else if (Resource.class.isAssignableFrom(paramValueClass)){
                Resource resource = (Resource) e.getValue();
                builder.addBinaryBody(e.getKey(), resource.getInputStream(), ContentType.MULTIPART_FORM_DATA, resource.getFilename());
            }
            //包装Resource[]类型的参数
            else if(Resource[].class.isAssignableFrom(paramValueClass)) {
                Resource[] resources = (Resource[]) e.getValue();
                for (Resource resource : resources) {
                    builder.addBinaryBody(e.getKey(), resource.getInputStream(), ContentType.MULTIPART_FORM_DATA, resource.getFilename());
                }
            }
            //其他类型将会被当做String类型的参数
            else {
                builder.addTextBody(e.getKey(), e.getValue().toString(), ContentType.APPLICATION_JSON);
            }

        }
        return builder.build();
    }


    /**
     * 得到非文件类型的请求的参数
     *
     * @param params
     * @return
     * @throws UnsupportedEncodingException
     */
    private static HttpEntity getUrlEncodedFormEntity(Map<String, Object> params) throws UnsupportedEncodingException {
        List<BasicNameValuePair> list = new ArrayList<>();
        for (String key : params.keySet()) {
            list.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
        return new UrlEncodedFormEntity(list);
    }


}
