package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.annotations.ResourceJava;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * 从资源中中提取JAVA对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/21 02:12
 */
public class ResourceJavaObjectResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        ResourceJava resJsonAnn = context.toAnnotation(ResourceJava.class);
        String resPath = context.parseExpression(resJsonAnn.value(), String.class);
        String charset = context.parseExpression(resJsonAnn.charset(), String.class);
        return Collections.singletonList(new ParamInfo("", Resources.resourceAsFlatBean(resPath, Charset.forName(charset))));
    }
}
