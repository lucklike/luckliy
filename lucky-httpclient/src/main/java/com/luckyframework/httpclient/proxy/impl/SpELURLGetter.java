package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.proxy.URLGetter;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.SpELConvert;

/**
 * 支持SpEL表达式的域名获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELURLGetter implements URLGetter {


    @Override
    public String getUrl(String configValue) {
        SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();
        return String.valueOf(spELConverter.parseExpression(configValue));
    }
}
