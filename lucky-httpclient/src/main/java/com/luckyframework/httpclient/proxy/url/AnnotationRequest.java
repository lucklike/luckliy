package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.RequestMethod;

/**
 * 注解请求实例
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/4 16:52
 */
public class AnnotationRequest extends DefaultRequest {

    private final String domain;
    private final String path;

    AnnotationRequest(String domain, String path, RequestMethod requestMethod) {
        super(StringUtils.joinUrlPath(domain, path), requestMethod);
        this.domain = domain;
        this.path = path;
    }

    public static AnnotationRequest create(String domain, String path, RequestMethod requestMethod) {
        return new AnnotationRequest(domain, path, requestMethod);
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }
}
