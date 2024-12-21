package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.PropertiesJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
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
        PropertiesJson jsonAnn = context.toAnnotation(PropertiesJson.class);
        String[] keyValueArray = jsonAnn.value();

        List<ParamInfo> paramInfos = new ArrayList<>(keyValueArray.length);
        String separator = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            if (propertyParamInfo != null) {
                paramInfos.add(propertyParamInfo);
            }
        }
        return Collections.singletonList(new ParamInfo("jsonBody", paramInfos));
    }

}
