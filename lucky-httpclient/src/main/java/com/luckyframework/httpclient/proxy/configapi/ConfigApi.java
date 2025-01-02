package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.spel.LazyValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$REQ_DEFAULT$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$REQ_SSE$__;


/**
 * ConfigApiConfig = ClassApiConfig + MethodApiConfig
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 22:31
 */
@SuppressWarnings("all")
public class ConfigApi extends CommonApi {

    private static final Map<String, LazyValue<HttpExecutor>> simpHttpExecutorMap = new ConcurrentHashMap<>(3);

    private CommonApi api = new CommonApi();

    private String type = __$REQ_DEFAULT$__;

    private TempPair<String, String> urlPair;

    private RequestMethod _method;

    private Boolean _async;

    private String _asyncExecutor;

    private String _connectTimeout;

    private String _readTimeout;

    private String _writeTimeout;

    private SSLConf _ssl;

    private LazyValue<HttpExecutor> _httpExecutor;

    private Map<String, List<Object>> _header;

    private Map<String, List<Object>> _query;

    private Map<String, Object> _form;

    private Map<String, Object> _path;

    private MultipartFormData _multipartFormData;

    private List<ConditionMapList> _conditionHeader;

    private List<ConditionMapList> _conditionQuery;

    private List<ConditionMap> _conditionForm;

    private List<ConditionMap> _conditionPath;

    private List<ConditionMultipartFormData> _conditionMultipartFormData;

    private ProxyConf _proxy;

    private MockConf _mock;

    private Body _body;

    private Convert _responseConvert;

    private Boolean _convertProhibit;

    private SseListenerConf _sseListener;

    private List<InterceptorConf> _interceptor;

    private Set<String> _interceptorProhibit;

    private RedirectConf _redirect;

    private LoggerConf _logger;

    private RetryConf _retry;

    private List<Extension<RequestExtendHandle>> _requestExtension;

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
                type = __$REQ_SSE$__;
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
            _method = getValueOrDefault(super.getMethod(), api.getMethod(), RequestMethod.GET);
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
    public synchronized Map<String, List<Object>> getHeader() {
        if (_header == null) {
            _header = new LinkedHashMap<>(api.getHeader());
            super.getHeader().forEach((k, headerList) -> {
                List<Object> list = _header.get(k);
                if (list == null) {
                    _header.put(k, headerList);
                } else {
                    list.addAll(headerList);
                }
            });
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
    public synchronized MultipartFormData getMultipartFormData() {
        if (_multipartFormData == null) {
            _multipartFormData = new MultipartFormData();

            MultipartFormData cMultipartFormData = api.getMultipartFormData();
            MultipartFormData mMultipartFormData = super.getMultipartFormData();

            if (cMultipartFormData != null) {
                _multipartFormData.putAllTxt(cMultipartFormData.getTxt());
                _multipartFormData.putAllFile(cMultipartFormData.getFile());
            }

            if (mMultipartFormData != null) {
                _multipartFormData.putAllTxt(mMultipartFormData.getTxt());
                _multipartFormData.putAllFile(mMultipartFormData.getFile());
            }
        }

        return _multipartFormData;
    }

    @Override
    public synchronized List<ConditionMapList> getConditionHeader() {
        if (_conditionHeader == null) {
            _conditionHeader = new LinkedList<>();
            _conditionHeader.addAll(api.getConditionHeader());
            _conditionHeader.addAll(super.getConditionHeader());
        }
        return _conditionHeader;
    }

    @Override
    public synchronized List<ConditionMapList> getConditionQuery() {
        if (_conditionQuery == null) {
            _conditionQuery = new LinkedList<>();
            _conditionQuery.addAll(api.getConditionQuery());
            _conditionQuery.addAll(super.getConditionQuery());
        }
        return _conditionQuery;
    }

    @Override
    public synchronized List<ConditionMap> getConditionForm() {
        if (_conditionForm == null) {
            _conditionForm = new LinkedList<>();
            _conditionForm.addAll(api.getConditionForm());
            _conditionForm.addAll(super.getConditionForm());
        }
        return _conditionForm;
    }

    @Override
    public synchronized List<ConditionMap> getConditionPath() {
        if (_conditionPath == null) {
            _conditionPath = new LinkedList<>();
            _conditionPath.addAll(api.getConditionPath());
            _conditionPath.addAll(super.getConditionPath());
        }
        return _conditionPath;
    }

    @Override
    public synchronized List<ConditionMultipartFormData> getConditionMultipartFormData() {
        if (_conditionMultipartFormData == null) {
            _conditionMultipartFormData = new LinkedList<>();
            _conditionMultipartFormData.addAll(api.getConditionMultipartFormData());
            _conditionMultipartFormData.addAll(super.getConditionMultipartFormData());
        }
        return _conditionMultipartFormData;
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

    public synchronized MockConf getMock() {
        if (_mock == null) {

            MockConf mMock = super.getMock();
            MockConf cMock = api.getMock();
            if (mMock != null && cMock != null) {
                _mock = new MockConf();
                _mock.setEnable(getStringValue(mMock.getEnable(), cMock.getEnable()));
                _mock.setCache(getValueOrDefault(mMock.getCache(), cMock.getCache(), true));
                _mock.setResponse(getStringValue(mMock.getResponse(), cMock.getResponse()));
                _mock.setStatus(getValue(mMock.getStatus(), cMock.getStatus()));
                _mock.setBody(getValue(mMock.getBody(), cMock.getBody()));

                Map<String, List<Object>> headerMap = new LinkedHashMap<>(cMock.getHeader());
                mMock.getHeader().forEach((k, headerList) -> {
                    List<Object> cList = headerMap.get(k);
                    if (cList == null) {
                        headerMap.put(k, headerList);
                    } else {
                        cList.addAll(headerList);
                    }
                });
                _mock.setHeader(headerMap);
            } else if (cMock == null) {
                _mock = mMock;
            } else {
                _mock = cMock;
            }
            if (_mock != null && _mock.getCache() == null) {
                _mock.setCache(true);
            }
        }
        return _mock;
    }

    @Override
    public synchronized String getConnectTimeout() {
        if (_connectTimeout == null) {
            _connectTimeout = getValue(super.getConnectTimeout(), api.getConnectTimeout());
        }
        return _connectTimeout;
    }

    @Override
    public synchronized String getReadTimeout() {
        if (_readTimeout == null) {
            _readTimeout = getValue(super.getReadTimeout(), api.getReadTimeout());
        }
        return _readTimeout;
    }

    @Override
    public synchronized String getWriteTimeout() {
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
    public synchronized SSLConf getSsl() {
        if (_ssl == null) {
            _ssl = new SSLConf();
            SSLConf mSsl = super.getSsl();
            SSLConf cSsl = api.getSsl();

            _ssl.setEnable(getValue(mSsl.getEnable(), cSsl.getEnable()));
            _ssl.setProtocol(getStringValue(mSsl.getProtocol(), cSsl.getProtocol()));
            _ssl.setHostnameVerifier(getValue(mSsl.getHostnameVerifier(), cSsl.getHostnameVerifier()));
            _ssl.setSslSocketFactory(getStringValue(mSsl.getSslSocketFactory(), cSsl.getSslSocketFactory()));
            _ssl.setKeyStoreInfo(getValue(mSsl.getKeyStoreInfo(), cSsl.getKeyStoreInfo()));
            _ssl.setTrustStoreInfo(getValue(mSsl.getTrustStoreInfo(), cSsl.getTrustStoreInfo()));
            _ssl.setKeyStore(getStringValue(mSsl.getKeyStore(), cSsl.getKeyStore()));
            _ssl.setTrustStore(getStringValue(mSsl.getTrustStore(), cSsl.getTrustStore()));

        }
        return _ssl;
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
            _body.setXml(getValue(mBody.getXml(), cBody.getXml()));
            _body.setForm(getValue(mBody.getForm(), cBody.getForm()));
            _body.setJava(getValue(mBody.getJava(), cBody.getJava()));
            _body.setProtobuf(getValue(mBody.getProtobuf(), cBody.getProtobuf()));
        }
        return _body;
    }

    @Override
    public synchronized Convert getRespConvert() {
        if (_responseConvert == null) {
            _responseConvert = new Convert();
            Convert mConvert = super.getRespConvert();
            Convert cConvert = api.getRespConvert();

            _responseConvert.setConvert(getValue(mConvert.getConvert(), cConvert.getConvert()));
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
    public synchronized Boolean getConvertProhibit() {
        if (_convertProhibit == null) {
            _convertProhibit = getValue(super.getConvertProhibit(), api.getConvertProhibit());
        }
        return _convertProhibit;
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
            _sseListener.setScope(getValueOrDefault(mScope, cScope, Scope.SINGLETON));
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
    public synchronized Set<String> getInterceptorProhibit() {
        if (_interceptorProhibit == null) {
            _interceptorProhibit = new HashSet<>(api.getInterceptorProhibit());
            _interceptorProhibit.addAll(super.getInterceptorProhibit());
        }
        return _interceptorProhibit;
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

    @Override
    public synchronized RetryConf getRetry() {
        if (_retry == null) {
            RetryConf mRetry = super.getRetry();
            RetryConf cRetry = api.getRetry();

            _retry = new RetryConf();
            _retry.setEnable(getValue(mRetry.getEnable(), cRetry.getEnable()));
            _retry.setTaskName(getStringValue(mRetry.getTaskName(), cRetry.getTaskName()));
            _retry.setMaxCount(getValue(mRetry.getMaxCount(), cRetry.getMaxCount()));
            _retry.setWaitMillis(getValue(mRetry.getWaitMillis(), cRetry.getWaitMillis()));
            _retry.setMultiplier(getValue(mRetry.getMultiplier(), cRetry.getMultiplier()));
            _retry.setMaxWaitMillis(getValue(mRetry.getMaxWaitMillis(), cRetry.getMaxWaitMillis()));
            _retry.setMinWaitMillis(getValue(mRetry.getMinWaitMillis(), cRetry.getMinWaitMillis()));
            _retry.setExpression(getStringValue(mRetry.getExpression(), cRetry.getExpression()));

            Set<Class<? extends Throwable>> exception = new HashSet<>(cRetry.getException());
            exception.addAll(mRetry.getException());
            if (exception.isEmpty()) {
                exception.add(Exception.class);
            }
            _retry.setException(exception);

            Set<Class<? extends Throwable>> exclude = new HashSet<>(cRetry.getExclude());
            exclude.addAll(mRetry.getExclude());
            _retry.setExclude(exclude);

            Set<Integer> exceptionStatus = new HashSet<>(cRetry.getExceptionStatus());
            exceptionStatus.addAll(mRetry.getExceptionStatus());
            _retry.setExceptionStatus(exceptionStatus);

            Set<Integer> normalStatus = new HashSet<>(cRetry.getNormalStatus());
            normalStatus.addAll(mRetry.getNormalStatus());
            _retry.setNormalStatus(normalStatus);
        }
        return _retry;
    }

    private LazyValue<HttpExecutor> createHttpExecutorByConfig(Context context, HttpExecutorConf httpExecutorConf) {
        if (httpExecutorConf == null) {
            return null;
        }
        return LazyValue.of(() -> (HttpExecutor) context.generateObject(httpExecutorConf.getClassName(), httpExecutorConf.getBeanName(), httpExecutorConf.getScope()));
    }

    private LazyValue<HttpExecutor> createHttpExecutorByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        switch (name.toUpperCase()) {
            case "JDK":
                return LazyValue.of(JdkHttpExecutor::new);
            case "HTTP_CLIENT":
                return LazyValue.of(HttpClientExecutor::new);
            case "OK_HTTP":
                return LazyValue.of(OkHttpExecutor::new);
            default:
                throw new ConfigurationParserException("Unsupported HttpExecutor type: '{}' Optional configuration values are: JDK/HTTP_CLIENT/OK_HTTP", name);
        }
    }

    @Override
    public synchronized List<Extension<RequestExtendHandle>> getRequestExtension() {
        if (_requestExtension == null) {
            _requestExtension = new ArrayList<>();

            List<Extension<RequestExtendHandle>> cRe = api.getRequestExtension();
            List<Extension<RequestExtendHandle>> mRe = super.getRequestExtension();

            if (ContainerUtils.isNotEmptyCollection(cRe)) {
                _requestExtension.addAll(cRe);
            }

            if (ContainerUtils.isNotEmptyCollection(mRe)) {
                _requestExtension.addAll(mRe);
            }
        }
        return _requestExtension;
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
