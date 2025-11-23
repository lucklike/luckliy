package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.proxy.annotations.CombinablePropJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        CombinablePropJson jsonAnn = context.toAnnotation(CombinablePropJson.class);

        ConfigurationMap propMap = new ConfigurationMap();
        String separator = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            if (propertyParamInfo != null) {
                propMap.addProperty(String.valueOf(propertyParamInfo.getName()), propertyParamInfo.getValue());
            }
        }
        return Collections.singletonList(new ParamInfo("", propMap));
    }

}
