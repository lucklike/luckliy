package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.HttpVersion;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * Http版本处理器
 *
 * @author fukang
 * @version 3.0.2
 * @date 2023/9/5 00:10
 */
public class HttpVersionStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        HttpVersion httpVersion = context.toAnnotation(HttpVersion.class);
        return Collections.singletonList(new ParamInfo("httpVersion", httpVersion.value()));
    }
}
