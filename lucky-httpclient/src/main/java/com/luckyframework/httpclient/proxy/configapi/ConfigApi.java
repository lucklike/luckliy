package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttp3Executor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.spel.LazyValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_DEFAULT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQ_SSE;


/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 22:31
 */
public class ConfigApi extends CommonApi {

    private static final Map<String, LazyValue<HttpExecutor>> simpHttpExecutorMap = new ConcurrentHashMap<>(3);

    private CommonApi api = new CommonApi();

    private String type = REQ_DEFAULT;

    private TempPair<String, String> urlPair;

    private RequestMethod _method;

    private Boolean _async;

    private String _asyncExecutor;

    private String _connectTimeout;

    private String _readTimeout;

    private String _writeTimeout;

    private LazyValue<HttpExecutor> _httpExecutor;

    private Map<String, Object> _header;

    private Map<String, List<Object>> _query;

    private Map<String, Object> _form;

    private Map<String, Object> _path;

    private Map<String, Object> _multiData;

    private Map<String, Object> _multiFile;

    private ProxyConf _proxy;

    private Body _body;

    private Convert _responseConvert;

    private SseListenerConf _sseListener;

    private List<InterceptorConf> _interceptor;

    private RedirectConf _redirect;

    private LoggerConf _logger;

    public CommonApi getApi() {
        return api;
    }

    public void setApi(CommonApi api) {
        this.api = api;
    }

    public String getType() {
        return type;
    }

    public synchronized TempPair<String, String> getUrlPair() {
        if (urlPair == null) {
            String methodUrl;
            String sse = super.getSse();
            if (StringUtils.hasText(sse)) {
                type = REQ_SSE;
                methodUrl = sse;
            } else {
                methodUrl = super.getUrl();
            }
            String classUrl = api.getUrl();
            urlPair = TempPair.of(classUrl, methodUrl);
        }
        return urlPair;
    }

    @Override
    public synchronized RequestMethod getMethod() {
        if (_method == null) {
            _method = getValueOrDefault(super.getMethod(), api.getMethod(),  RequestMethod.GET);
        }
        return _method;
    }

    @Override
    public synchronized Boolean isAsync() {
        if (_async == null) {
            Boolean mAsync = super.isAsync();
            Boolean cAsync = api.isAsync();
            _async = mAsync == null ? (cAsync != null) : mAsync;
        }
        return _async;
    }

    @Override
    public synchronized String getAsyncExecutor() {
        if (_asyncExecutor == null) {
            String mAsyncExecutor = super.getAsyncExecutor();
            String cAsyncExecutor = api.getAsyncExecutor();
            _asyncExecutor = getStringValue(mAsyncExecutor, cAsyncExecutor);
        }
        return _asyncExecutor;
    }

    @Override
    public synchronized Map<String, Object> getHeader() {
        if (_header == null) {
            _header = new LinkedHashMap<>(api.getHeader());
            _header.putAll(super.getHeader());
        }
        return _header;
    }

    @Override
    public synchronized Map<String, List<Object>> getQuery() {
        if (_query == null) {
            _query = new LinkedHashMap<>(api.getQuery());
            super.getQuery().forEach((k, queryList) -> {
                List<Object> list = _query.get(k);
                if (list == null) {
                    _query.put(k, queryList);
                } else {
                    list.addAll(queryList);
                }
            });
        }
        return _query;
    }

    @Override
    public synchronized Map<String, Object> getForm() {
        if (_form == null) {
            _form = new LinkedHashMap<>(api.getForm());
            _form.putAll(super.getForm());
        }
        return _form;
    }

    @Override
    public synchronized Map<String, Object> getPath() {
        if (_path == null) {
            _path = new LinkedHashMap<>(api.getPath());
            _path.putAll(super.getPath());
        }
        return _path;
    }

    @Override
    public synchronized Map<String, Object> getMultiData() {
        if (_multiData == null) {
            _multiData = new LinkedHashMap<>(api.getMultiData());
            _multiData.putAll(super.getMultiData());
        }
        return _multiData;
    }

    @Override
    public synchronized Map<String, Object> getMultiFile() {
        if (_multiFile == null) {
            _multiFile = new LinkedHashMap<>(api.getMultiFile());
            _multiFile.putAll(super.getMultiFile());
        }
        return _multiFile;
    }

    @Override
    public synchronized ProxyConf getProxy() {
        if (_proxy == null) {
            _proxy = new ProxyConf();
            ProxyConf mProxy = super.getProxy();
            ProxyConf cProxy = api.getProxy();

            _proxy.setType(getValue(mProxy.getType(), cProxy.getType()));
            _proxy.setIp(getStringValue(mProxy.getIp(), cProxy.getIp()));
            _proxy.setPort(getStringValue(mProxy.getPort(), cProxy.getPort()));
            _proxy.setUsername(getStringValue(mProxy.getUsername(), cProxy.getUsername()));
            _proxy.setPassword(getStringValue(mProxy.getPassword(), cProxy.getPassword()));
        }
        return _proxy;
    }

    @Override
    public String getConnectTimeout() {
        if (_connectTimeout == null) {
            _connectTimeout = getValue(super.getConnectTimeout(), api.getConnectTimeout());
        }
        return _connectTimeout;
    }

    @Override
    public String getReadTimeout() {
        if (_readTimeout == null) {
            _readTimeout = getValue(super.getReadTimeout(), api.getReadTimeout());
        }
        return _readTimeout;
    }

    @Override
    public String getWriteTimeout() {
        if (_writeTimeout == null) {
            _writeTimeout = getValue(super.getWriteTimeout(), api.getWriteTimeout());
        }
        return _writeTimeout;
    }

    public synchronized LazyValue<HttpExecutor> getLazyHttpExecutor(Context context) {
        if (_httpExecutor == null) {
            _httpExecutor = createHttpExecutorByConfig(context, super.getHttpExecutorConfig());
            if (_httpExecutor == null) {
                _httpExecutor = getSimpHttpExecutor(super.getHttpExecutor());
            }
            if (_httpExecutor == null) {
                _httpExecutor = createHttpExecutorByConfig(context, api.getHttpExecutorConfig());
            }
            if (_httpExecutor == null) {
                _httpExecutor = getSimpHttpExecutor(api.getHttpExecutor());
            }
        }
        return _httpExecutor;
    }

    private synchronized LazyValue<HttpExecutor> getSimpHttpExecutor(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return simpHttpExecutorMap.computeIfAbsent(name, this::createHttpExecutorByName);
    }

    @Override
    public synchronized Body getBody() {
        if (_body == null) {
            _body = new Body();
            Body mBody = super.getBody();
            Body cBody = api.getBody();

            _body.setCharset(getStringValue(mBody.getCharset(), cBody.getCharset()));
            _body.setMimeType(getStringValue(mBody.getMimeType(), cBody.getMimeType()));
            _body.setData(getStringValue(mBody.getData(), cBody.getData()));
            _body.setFile(getStringValue(mBody.getFile(), cBody.getFile()));
            _body.setJson(getValue(mBody.getJson(), cBody.getJson()));
            _body.setXml(getStringValue(mBody.getXml(), cBody.getXml()));
            _body.setForm(getStringValue(mBody.getForm(), cBody.getForm()));
            _body.setJava(getStringValue(mBody.getJava(), cBody.getJava()));
            _body.setProtobuf(getStringValue(mBody.getProtobuf(), cBody.getProtobuf()));
        }
        return _body;
    }

    @Override
    public synchronized Convert getRespConvert() {
        if (_responseConvert == null) {
            _responseConvert = new Convert();
            Convert mConvert = super.getRespConvert();
            Convert cConvert = api.getRespConvert();

            _responseConvert.setResult(getStringValue(mConvert.getResult(), cConvert.getResult()));
            _responseConvert.setException(getStringValue(mConvert.getException(), cConvert.getException()));
            _responseConvert.setMetaType(Object.class == mConvert.getMetaType() ? cConvert.getMetaType() : mConvert.getMetaType());
            List<Condition> newConditions = new ArrayList<>(cConvert.getCondition());
            newConditions.addAll(mConvert.getCondition());
            _responseConvert.setCondition(newConditions);
        }
        return _responseConvert;
    }

    @Override
    public SseListenerConf getSseListener() {
        if (_sseListener == null) {
            SseListenerConf mListener = super.getSseListener();
            SseListenerConf cListener = api.getSseListener();

            _sseListener = new SseListenerConf();
            _sseListener.setBeanName(getStringValue(mListener.getBeanName(), cListener.getBeanName()));

            Class<?> mClazz = mListener.getClassName();
            Class<?> cClazz = cListener.getClassName();
            _sseListener.setClassName(mClazz == EventListener.class ? cClazz : mClazz);

            Scope mScope = mListener.getScope();
            Scope cScope = cListener.getScope();
            _sseListener.setScope(getValueOrDefault(mScope, cScope,  Scope.SINGLETON));
        }
        return _sseListener;
    }

    @Override
    public synchronized List<InterceptorConf> getInterceptor() {
        if (_interceptor == null) {
            _interceptor = new ArrayList<>(api.getInterceptor());
            _interceptor.addAll(super.getInterceptor());
        }
        return _interceptor;
    }

    @Override
    public synchronized RedirectConf getRedirect() {
        if (_redirect == null) {
            RedirectConf mRedirect = super.getRedirect();
            RedirectConf cRedirect = api.getRedirect();

            _redirect = new RedirectConf();
            _redirect.setEnable(getValue(mRedirect.isEnable(), cRedirect.isEnable()));
            _redirect.setLocation(getStringValue(mRedirect.getLocation(), cRedirect.getLocation()));
            _redirect.setCondition(getStringValue(mRedirect.getCondition(), cRedirect.getCondition()));
            _redirect.setStatus(ContainerUtils.isEmptyArray(mRedirect.getStatus()) ? cRedirect.getStatus() : mRedirect.getStatus());
            _redirect.setPriority(getValueOrDefault(mRedirect.getPriority(), cRedirect.getPriority(), PriorityConstant.REDIRECT_PRIORITY));
            _redirect.setMaxCount(getValueOrDefault(mRedirect.getMaxCount(), cRedirect.getMaxCount(), 5));

        }
        return _redirect;
    }

    @Override
    public synchronized LoggerConf getLogger() {
        if (_logger == null) {
            LoggerConf mLogger = super.getLogger();
            LoggerConf cLogger = api.getLogger();

            _logger = new LoggerConf();
            _logger.setEnable(getValue(mLogger.isEnable(), cLogger.isEnable()));
            _logger.setEnableReqLog(getValueOrDefault(mLogger.isEnableReqLog(), cLogger.isEnableReqLog(), true));
            _logger.setEnableRespLog(getValueOrDefault(mLogger.isEnableRespLog(), cLogger.isEnableRespLog(), true));
            _logger.setEnableAnnotationLog(getBooleanValue(mLogger.isEnableAnnotationLog(), cLogger.isEnableAnnotationLog()));
            _logger.setEnableArgsLog(getBooleanValue(mLogger.isEnableArgsLog(), cLogger.isEnableArgsLog()));
            _logger.setForcePrintBody(getBooleanValue(mLogger.isForcePrintBody(), cLogger.isForcePrintBody()));
            _logger.setPriority(getValueOrDefault(mLogger.getPriority(), cLogger.getPriority(), PriorityConstant.DEFAULT_PRIORITY));
            _logger.setSetAllowMimeTypes(ContainerUtils.isEmptyCollection(mLogger.getSetAllowMimeTypes()) ? cLogger.getSetAllowMimeTypes() : mLogger.getSetAllowMimeTypes());
            _logger.setAddAllowMimeTypes(ContainerUtils.isEmptyCollection(mLogger.getAddAllowMimeTypes()) ? cLogger.getAddAllowMimeTypes() : mLogger.getAddAllowMimeTypes());
            _logger.setBodyMaxLength(getValueOrDefault(mLogger.getBodyMaxLength(), cLogger.getBodyMaxLength(), -1L));
            _logger.setReqLogCondition(getStringValue(mLogger.getReqLogCondition(), cLogger.getReqLogCondition()));
            _logger.setRespLogCondition(getStringValue(mLogger.getRespLogCondition(), cLogger.getRespLogCondition()));
        }
        return _logger;
    }

    private LazyValue<HttpExecutor> createHttpExecutorByConfig(Context context, HttpExecutorConf httpExecutorConf) {
        if (httpExecutorConf == null) {
            return null;
        }
        return LazyValue.of(() -> (HttpExecutor)context.getHttpProxyFactory().getObjectCreator().newObject(httpExecutorConf.getClassName(), httpExecutorConf.getBeanName(), context, httpExecutorConf.getScope()));
    }

    private LazyValue<HttpExecutor> createHttpExecutorByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        switch (name.toUpperCase()) {
            case "JDK" : return LazyValue.of(JdkHttpExecutor::new);
            case "HTTP_CLIENT": return LazyValue.of(HttpClientExecutor::new);
            case "OK_HTTP": return LazyValue.of(() -> {
                try {
                    Class.forName("okhttp3.RequestBody$Companion");
                    return new OkHttp3Executor();
                }catch (Exception e) {
                    return new OkHttpExecutor();
                }
            });
            default: throw new ConfigurationParserException("Unsupported HttpExecutor type: '{}' Optional configuration values are: JDK/HTTP_CLIENT/OK_HTTP", name);
        }
    }

    private <T> T getValue(T mValue, T cValue) {
        return getValueOrDefault(mValue, cValue, null);
    }

    private <T> T getValueOrDefault(T mValue, T cValue, T defaultValue) {
        return mValue != null ? mValue : (cValue != null ? cValue : defaultValue);
    }


    private Boolean getBooleanValue(Boolean mValue, Boolean cValue) {
        return getValueOrDefault(mValue, cValue, false);
    }

    private String getStringValue(String mValue, String cValue) {
        return StringUtils.hasText(mValue) ? mValue : cValue;
    }



}
