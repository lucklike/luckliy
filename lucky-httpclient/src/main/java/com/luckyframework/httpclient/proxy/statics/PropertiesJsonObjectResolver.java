package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.NonJson;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJson;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
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
        PropertiesJson jsonAnn = context.toAnnotation(PropertiesJson.class);
        ConfigurationMap configMap = new ConfigurationMap();
        String[] keyValueArray = jsonAnn.value();

        // 如果配置了value则优先使用value配置
        if (ContainerUtils.isNotEmptyArray(keyValueArray)) {
            String separator = jsonAnn.separator();
            for (String expression : jsonAnn.value()) {
                addObjectByExpression(context, configMap, expression, separator);
            }
        }
        // 未配置value则遍历参数列表获取Json属性
        else {
            for (ParameterContext parameterContext : context.getContext().getParameterContexts()) {
                if (!parameterContext.isAnnotated(NonJson.class)) {
                    configMap.addProperty(parameterContext.getName(), parameterContext.getValue());
                }
            }
        }

        return Collections.singletonList(new ParamInfo("jsonBody", BodyObject.jsonBody(configMap.getDataMap())));
    }

}
