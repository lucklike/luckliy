package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonObject;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

public class JsonObjectBodyResolver implements StaticParamResolver {
    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        PropertiesJsonObject jsonAnn = context.toAnnotation(PropertiesJsonObject.class);
        ConfigurationMap configMap = new ConfigurationMap();
        String separation = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            int index = expression.indexOf(separation);
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static parameter expression: '" + expression + "'. Please use the correct separator: '" + separation + "'");
            }
            String name = context.parseExpression(expression.substring(0, index), String.class);
            Object value = context.parseExpression(expression.substring(index + 1));
            if (value == null) {
                continue;
            }
            if (isEasyKey(name)) {
                configMap.put(getEasyKey(name), value);
            } else {
                configMap.addProperty(name, value);
            }
        }

        return Collections.singletonList(new ParamInfo("jsonBody", BodyObject.jsonBody(configMap.getDataMap())));
    }

    private boolean isEasyKey(String key) {
        return key.startsWith("'") && key.endsWith("'");
    }

    private String getEasyKey(String key) {
        return key.substring(1, key.length() - 1);
    }
}
