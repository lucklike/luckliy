package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用于描述一个API的所有描述信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:05
 */
public class CommonApi {

    /**
     * 请求URL
     */
    private String url;

    /**
     * SSE类型请求使用此属性定义URL
     */
    private String sse;

    /**
     * 请求方法
     */
    private RequestMethod method;

    /**
     * 是否异步执行，只对void方法生效，非void方法如果需要启用异步，只需要定义Future类型的返回值即可
     */
    private Boolean async;

    /**
     * 指定执行异步任务的线程池
     */
    @TargetField("async-executor")
    private String asyncExecutor = "";

    /**
     * 定义连接超时时间
     */
    @TargetField("connect-timeout")
    private String connectTimeout;

    /**
     * 定义读取超时时间
     */
    @TargetField("read-timeout")
    private String readTimeout;

    /**
     * 定义写入超时时间
     */
    @TargetField("write-timeout")
    private String writeTimeout;

    /**
     * 向SpEL运行时环境中导入变量、函数、包等
     */
    @TargetField("spring-el-import")
    private SpELImportConf springElImport = new SpELImportConf();

    /**
     * 定义HTTP执行器配置，可以通过bean-name/class-name+scope来指定
     */
    @TargetField("http-executor-config")
    private HttpExecutorConf httpExecutorConfig;

    /**
     * 定义HTTP执行器配置，通过字符串来指定：JDK/HTTP_CLIENT/OK_HTTP
     */
    @TargetField("http-executor")
    private String httpExecutor;

    private SSLConf ssl = new SSLConf();

    /**
     * 定义请求头参数
     */
    private Map<String, Object> header = new LinkedHashMap<>();

    /**
     * 定义Query参数
     */
    private Map<String, List<Object>> query = new LinkedHashMap<>();

    /**
     * 定义Form表单参数
     */
    private Map<String, Object> form = new LinkedHashMap<>();

    /**
     * 定义Path路径参数
     */
    private Map<String, Object> path = new LinkedHashMap<>();

    /**
     * 定义代理配置
     */
    private ProxyConf proxy = new ProxyConf();

    /**
     * Mock相关配置
     */
    private MockConf mock;

    /**
     * 定义请求体参数
     */
    private Body body = new Body();

    @TargetField("multipart-form-data")
    private MultipartFormData multipartFormData;

    /**
     * 定义响应转换器相关的配置
     */
    @TargetField("resp-convert")
    private Convert respConvert = new Convert();

    /**
     * 禁止使用响应转换器
     */
    @TargetField("convert-prohibit")
    private Boolean convertProhibit;

    /**
     * 定义SSE请求的监听器
     */
    @TargetField("sse-listener")
    private SseListenerConf sseListener = new SseListenerConf();

    /**
     * 定义拦截器配置
     */
    private List<InterceptorConf> interceptor = new ArrayList<>();

    /**
     * 定义禁用的拦截器，此处配置{@link Interceptor#uniqueIdentification()}
     */
    @TargetField("interceptor-prohibit")
    private Set<String> interceptorProhibit = new HashSet<>();

    /**
     * 重试相关的配置
     */
    private RedirectConf redirect = new RedirectConf();

    /**
     * 日志相关的配置
     */
    private LoggerConf logger = new LoggerConf();

    /**
     * 重试相关的配置
     */
    private RetryConf retry = new RetryConf();


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

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getAsyncExecutor() {
        return asyncExecutor;
    }

    public void setAsyncExecutor(String asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
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

    public ProxyConf getProxy() {
        return proxy;
    }

    public void setProxy(ProxyConf proxy) {
        this.proxy = proxy;
    }

    public MockConf getMock() {
        return mock;
    }

    public void setMock(MockConf mock) {
        this.mock = mock;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public MultipartFormData getMultipartFormData() {
        return multipartFormData;
    }

    public void setMultipartFormData(MultipartFormData multipartFormData) {
        this.multipartFormData = multipartFormData;
    }

    public Convert getRespConvert() {
        return respConvert;
    }

    public void setRespConvert(Convert respConvert) {
        this.respConvert = respConvert;
    }

    public SseListenerConf getSseListener() {
        return sseListener;
    }

    public void setSseListener(SseListenerConf sseListener) {
        this.sseListener = sseListener;
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

    public SpELImportConf getSpringElImport() {
        return springElImport;
    }

    public void setSpringElImport(SpELImportConf springElImport) {
        this.springElImport = springElImport;
    }

    public SSLConf getSsl() {
        return ssl;
    }

    public void setSsl(SSLConf ssl) {
        this.ssl = ssl;
    }

    public String getHttpExecutor() {
        return httpExecutor;
    }

    public void setHttpExecutor(String httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public HttpExecutorConf getHttpExecutorConfig() {
        return httpExecutorConfig;
    }

    public void setHttpExecutorConfig(HttpExecutorConf httpExecutorConfig) {
        this.httpExecutorConfig = httpExecutorConfig;
    }

    public List<InterceptorConf> getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(List<InterceptorConf> interceptor) {
        this.interceptor = interceptor;
    }

    public RedirectConf getRedirect() {
        return redirect;
    }

    public void setRedirect(RedirectConf redirect) {
        this.redirect = redirect;
    }

    public LoggerConf getLogger() {
        return logger;
    }

    public void setLogger(LoggerConf logger) {
        this.logger = logger;
    }

    public Boolean getConvertProhibit() {
        return convertProhibit;
    }

    public void setConvertProhibit(Boolean convertProhibit) {
        this.convertProhibit = convertProhibit;
    }

    public Set<String> getInterceptorProhibit() {
        return interceptorProhibit;
    }

    public void setInterceptorProhibit(Set<String> interceptorProhibit) {
        this.interceptorProhibit = interceptorProhibit;
    }

    public RetryConf getRetry() {
        return retry;
    }

    public void setRetry(RetryConf retry) {
        this.retry = retry;
    }
}
