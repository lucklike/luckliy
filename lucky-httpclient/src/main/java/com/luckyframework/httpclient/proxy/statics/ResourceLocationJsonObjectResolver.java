package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.annotations.CombinableResJson;

/**
 * 从资源中中提取JSON对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/21 02:12
 */
public class ResourceLocationJsonObjectResolver extends LocationConfigurationMapResolver {

    @Override
    protected ConfigurationMap loadConfig(StaticParamAnnContext context, String location) {
        CombinableResJson resJsonAnn = context.toAnnotation(CombinableResJson.class);
        String charset = context.parseExpression(resJsonAnn.charset(), String.class);
        return Resources.resourceAsConfigMap(location, charset);
    }
}
