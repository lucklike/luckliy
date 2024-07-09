package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.core.meta.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于描述一个API的所有描述信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:05
 */
public class CommonApi {

    private String url;

    private String sse;

    private RequestMethod method;

    @TargetField("connect-timeout")
    private String connectTimeout;

    @TargetField("read-timeout")
    private String readTimeout;

    @TargetField("write-timeout")
    private String writeTimeout;

    private Map<String, Object> header = new ConcurrentHashMap<>();

    private Map<String, List<Object>> query = new ConcurrentHashMap<>();

    private Map<String, Object> form = new ConcurrentHashMap<>();

    private Map<String, Object> path = new ConcurrentHashMap<>();

    @TargetField("multi-data")
    private Map<String, Object> multiData = new ConcurrentHashMap<>();

    @TargetField("multi-file")
    private Map<String, Object> multiFile = new ConcurrentHashMap<>();

    private ProxyConf proxy = new ProxyConf();

    private Body body = new Body();

    @TargetField("resp-convert")
    private Convert respConvert = new Convert();

    @TargetField("sse-convert")
    private SseConvert sseConvert = new SseConvert();

    private List<InterceptorConf> interceptor = new ArrayList<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSse() {
        return sse;
    }

    public void setSse(String sse) {
        this.sse = sse;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public void setMethod(RequestMethod method) {
        this.method = method;
    }

    public Map<String, Object> getHeader() {
        return header;
    }

    public void setHeader(Map<String, Object> header) {
        this.header = header;
    }

    public Map<String, List<Object>> getQuery() {
        return query;
    }

    public void setQuery(Map<String, List<Object>> query) {
        this.query = query;
    }

    public Map<String, Object> getForm() {
        return form;
    }

    public void setForm(Map<String, Object> form) {
        this.form = form;
    }

    public Map<String, Object> getPath() {
        return path;
    }

    public void setPath(Map<String, Object> path) {
        this.path = path;
    }

    public Map<String, Object> getMultiData() {
        return multiData;
    }

    public void setMultiData(Map<String, Object> multiData) {
        this.multiData = multiData;
    }

    public Map<String, Object> getMultiFile() {
        return multiFile;
    }

    public void setMultiFile(Map<String, Object> multiFile) {
        this.multiFile = multiFile;
    }

    public ProxyConf getProxy() {
        return proxy;
    }

    public void setProxy(ProxyConf proxy) {
        this.proxy = proxy;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public Convert getRespConvert() {
        return respConvert;
    }

    public void setRespConvert(Convert respConvert) {
        this.respConvert = respConvert;
    }

    public SseConvert getSseConvert() {
        return sseConvert;
    }

    public void setSseConvert(SseConvert sseConvert) {
        this.sseConvert = sseConvert;
    }

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(String writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public List<InterceptorConf> getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(List<InterceptorConf> interceptor) {
        this.interceptor = interceptor;
    }

}