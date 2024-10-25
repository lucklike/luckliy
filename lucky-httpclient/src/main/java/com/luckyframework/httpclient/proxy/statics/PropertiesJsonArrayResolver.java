package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Regular;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonArray;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * properties文件格式的JSON对象请求体解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class PropertiesJsonArrayResolver extends AbstractPropertiesJsonResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        PropertiesJsonArray jsonAnn = context.toAnnotation(PropertiesJsonArray.class);
        ConfigurationMap configMap = new ConfigurationMap();
        String prefix = jsonAnn.prefix();
        String arrayKey = getPrefix(prefix);

        configMap.put(arrayKey, new LinkedList<>());
        String separator = jsonAnn.separator();
        String reg = "^" + prefix + "\\[[0-9]\\d*\\]";
        for (String expression : jsonAnn.value()) {
            arrayExpressionCheck(expression, reg);
            addObjectByExpression(context, configMap, expression, separator);
        }
        return Collections.singletonList(new ParamInfo("jsonBody", BodyObject.jsonBody(configMap.get(arrayKey))));
    }

    private void arrayExpressionCheck(String expression, String reg) {
        if (ContainerUtils.isEmptyCollection(Regular.getArrayByExpression(expression, reg))) {
            throw new IllegalArgumentException("Wrong array expression: '" + expression + "' does not match regular expression: '" + reg + "'");
        }
    }

    private String getPrefix(String prefix) {
        return prefix.startsWith("\\") ? prefix.substring(1) : prefix;
    }

}
