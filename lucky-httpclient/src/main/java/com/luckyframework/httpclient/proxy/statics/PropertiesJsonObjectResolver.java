package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonObject;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * properties文件格式的JSON请求体解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class PropertiesJsonObjectResolver implements StaticParamResolver {

    private final static String ARRAY_IDENTIFICATION = "^$\\[[0-9]\\d*\\].";

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        final String ARRAY_NAME = "$";
        PropertiesJsonObject jsonAnn = context.toAnnotation(PropertiesJsonObject.class);
        ConfigurationMap configMap = new ConfigurationMap();
        String separation = jsonAnn.separator();
        boolean isArray = jsonAnn.array();
        if (isArray) {
            configMap.put(ARRAY_NAME, new ArrayList<>());
        }
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
            addObject(configMap, name, value);
        }
        Object body = isArray ? configMap.get(ARRAY_NAME) : configMap.getDataMap();
        return Collections.singletonList(new ParamInfo("jsonBody", BodyObject.jsonBody(body)));
    }

    private void addObject(ConfigurationMap configMap, String name, Object value) {
        if (isEasyKey(name)) {
            configMap.put(getEasyKey(name), value);
        } else {
            configMap.addProperty(name, value);
        }
    }

    private boolean isEasyKey(String key) {
        return key.startsWith("'") && key.endsWith("'");
    }

    private String getEasyKey(String key) {
        return key.substring(1, key.length() - 1);
    }
}
