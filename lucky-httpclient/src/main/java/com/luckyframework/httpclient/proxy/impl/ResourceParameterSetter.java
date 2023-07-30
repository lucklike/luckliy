package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.io.HttpResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * 文件资源参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class ResourceParameterSetter implements ParameterSetter {

    @Override
    public void set(Request request, String paramName, Object paramValue) {
        Resource[] resources = (Resource[]) paramValue;
        if (resources.length == 1 && (resources[0] instanceof UrlResource)) {
            request.addResources(paramName, new HttpResource(((UrlResource) resources[0])));
        } else {
            request.addResources(paramName, resources);
        }
    }
}
