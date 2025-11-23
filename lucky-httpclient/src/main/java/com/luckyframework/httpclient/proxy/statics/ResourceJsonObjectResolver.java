package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.annotations.CombinableResJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 从资源中中提取JSON对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/21 02:12
 */
public class ResourceJsonObjectResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        CombinableResJson resJsonAnn = context.toAnnotation(CombinableResJson.class);
        String resPath = context.parseExpression(resJsonAnn.value(), String.class);
        String charset = context.parseExpression(resJsonAnn.charset(), String.class);
        return Collections.singletonList(new ParamInfo("", Resources.resourceAsConfigMap(resPath, charset)));
    }
}
