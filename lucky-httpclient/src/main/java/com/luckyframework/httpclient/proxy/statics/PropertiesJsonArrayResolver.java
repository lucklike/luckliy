package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Regular;
import com.luckyframework.httpclient.proxy.annotations.PropertiesJsonArray;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
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
        String prefix = jsonAnn.prefix();
        String arrayKey = getPrefix(prefix);

        String separator = jsonAnn.separator();
        String reg = "^" + prefix + "\\[[0-9]\\d*\\]";
        String[] keyValueArray = jsonAnn.value();

        List<ParamInfo> paramInfos = new ArrayList<>();
        paramInfos.add(new ParamInfo(arrayKey, new LinkedList<>()));

        for (String expression : keyValueArray) {
            arrayExpressionCheck(expression, reg);
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            if (propertyParamInfo != null) {
                paramInfos.add(propertyParamInfo);
            }
        }
        return Collections.singletonList(new ParamInfo(arrayKey, paramInfos));
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
