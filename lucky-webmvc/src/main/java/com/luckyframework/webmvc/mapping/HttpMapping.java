package com.luckyframework.webmvc.mapping;

import com.luckyframework.httpclient.core.RequestMethod;

import java.util.Map;

public interface HttpMapping extends Mapping {

    String getURL();

    String getURI();

    RequestMethod getRequestMethod();

    Map<String,Object> getHeaders();

    Map<String,Object> getParameters();
}
