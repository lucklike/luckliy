package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Regular;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.proxy.annotations.NonJson;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonArray;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
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
        String[] keyValueArray = jsonAnn.value();

        // 如果配置了value则优先使用value配置
        if (ContainerUtils.isNotEmptyArray(keyValueArray)) {
            for (String expression : keyValueArray) {
                arrayExpressionCheck(expression, reg);
                addObjectByExpression(context, configMap, expression, separator);
            }
        }
        // 未配置value则遍历参数列表获取Json属性
        else {
            for (ParameterContext parameterContext : context.getContext().getParameterContexts()) {
                if (!parameterContext.isAnnotated(NonJson.class)) {
                    String name = parameterContext.getName();
                    arrayExpressionCheck(name, reg);
                    configMap.addProperty(name, parameterContext.getValue());
                }
            }
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
