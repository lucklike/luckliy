package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonObject;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * properties文件格式的JSON对象请求体解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class PropertiesJsonObjectResolver extends AbstractPropertiesJsonResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        PropertiesJsonObject jsonAnn = context.toAnnotation(PropertiesJsonObject.class);
        ConfigurationMap configMap = new ConfigurationMap();
        String separator = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            addObjectByExpression(context, configMap, expression, separator);
        }
        return Collections.singletonList(new ParamInfo("jsonBody", BodyObject.jsonBody(configMap.getDataMap())));
    }

}
