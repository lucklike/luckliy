package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.RequestMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 22:31
 */
public class ConfigApi extends Api {

    private Api api = new Api();

    private String _url;

    private RequestMethod _method;

    private String _connectTimeout;

    private String _readTimeout;

    private String _writeTimeout;

    private Map<String, Object> _header;

    private Map<String, List<Object>> _query;

    private Map<String, Object> _form;

    private Map<String, Object> _path;

    private Map<String, Object> _multiData;

    private Map<String, Object> _multiFile;

    private ProxyConf _proxy;

    private Body _body;

    private Convert _responseConvert;

    private List<InterceptorConf> _interceptor;

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    @Override
    public synchronized String getUrl() {
        if (_url == null) {
            String classUrl = api.getUrl();
            if (StringUtils.hasText(classUrl)) {
                _url = StringUtils.joinUrlPath(classUrl, super.getUrl());
            } else {
                _url = super.getUrl();
            }
        }
        return _url;
    }

    @Override
    public synchronized RequestMethod getMethod() {
        if (_method == null) {
            _method = super.getMethod() != null ? super.getMethod() : api.getMethod();
        }
        return _method;
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

            _proxy.setType(mProxy.getType() != null ? mProxy.getType() : cProxy.getType());
            _proxy.setIp(StringUtils.hasText(mProxy.getIp()) ? mProxy.getIp() : cProxy.getIp());
            _proxy.setPort(StringUtils.hasText(mProxy.getPort()) ? mProxy.getPort() : cProxy.getPort());
            _proxy.setUsername(StringUtils.hasText(mProxy.getUsername()) ? mProxy.getUsername() : cProxy.getUsername());
            _proxy.setPassword(StringUtils.hasText(mProxy.getPassword()) ? mProxy.getPassword() : cProxy.getPassword());
        }
        return _proxy;
    }

    @Override
    public String getConnectTimeout() {
        if (_connectTimeout == null) {
            _connectTimeout = super.getConnectTimeout() != null ? super.getConnectTimeout() : api.getConnectTimeout();
        }
        return _connectTimeout;
    }

    @Override
    public String getReadTimeout() {
        if (_readTimeout == null) {
            _readTimeout = super.getReadTimeout() != null ? super.getReadTimeout() : api.getReadTimeout();
        }
        return _readTimeout;
    }

    @Override
    public String getWriteTimeout() {
        if (_writeTimeout == null) {
            _writeTimeout = super.getWriteTimeout() != null ? super.getWriteTimeout() : api.getWriteTimeout();
        }
        return _writeTimeout;
    }

    @Override
    public synchronized Body getBody() {
        if (_body == null) {
            _body = new Body();
            Body mBody = super.getBody();
            Body cBody = api.getBody();

            _body.setCharset(StringUtils.hasText(mBody.getCharset()) ? mBody.getCharset() : cBody.getCharset());
            _body.setMimeType(StringUtils.hasText(mBody.getMimeType()) ? mBody.getMimeType() : cBody.getMimeType());
            _body.setData(StringUtils.hasText(mBody.getData()) ? mBody.getData() : cBody.getData());
            _body.setFile(StringUtils.hasText(mBody.getFile()) ? mBody.getFile() : cBody.getFile());
            _body.setJson(StringUtils.hasText(mBody.getJson()) ? mBody.getJson() : cBody.getJson());
            _body.setXml(StringUtils.hasText(mBody.getXml()) ? mBody.getXml() : cBody.getXml());
            _body.setForm(StringUtils.hasText(mBody.getForm()) ? mBody.getForm() : cBody.getForm());
        }
        return _body;
    }

    @Override
    public synchronized Convert getRespConvert() {
        if (_responseConvert == null) {
            _responseConvert = new Convert();
            Convert mConvert = super.getRespConvert();
            Convert cConvert = api.getRespConvert();

            _responseConvert.setResult(StringUtils.hasText(mConvert.getResult()) ? mConvert.getResult() : cConvert.getResult());
            _responseConvert.setException(StringUtils.hasText(mConvert.getException()) ? mConvert.getException() : cConvert.getException());
            _responseConvert.setMetaType(Object.class == mConvert.getMetaType() ? cConvert.getMetaType() : mConvert.getMetaType());
            List<Condition> newConditions = new ArrayList<>(cConvert.getCondition());
            newConditions.addAll(mConvert.getCondition());
            _responseConvert.setCondition(newConditions);
        }
        return _responseConvert;
    }

    @Override
    public synchronized List<InterceptorConf> getInterceptor() {
        if (_interceptor == null) {
            _interceptor = new ArrayList<>(api.getInterceptor());
            _interceptor.addAll(super.getInterceptor());
            _interceptor.sort(Comparator.comparingInt(InterceptorConf::getPriority));
        }
        return _interceptor;
    }
}
