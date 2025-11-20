package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.annotations.CombinableResJsonArray;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 从资源中中提取JSON对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/21 02:53
 */
public class ResourceJsonArrayObjectResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        CombinableResJsonArray jsonAnn = context.toAnnotation(CombinableResJsonArray.class);
        String arrayKey = jsonAnn.prefix();
        String resourcePath = context.parseExpression(jsonAnn.value(), String.class);

        // 读取资源内容
        ConfigurationMap resourceContentMap = Resources.resourceAsConfigMap(resourcePath, jsonAnn.charset());
        List<ConfigurationMap> resultList = resourceContentMap.getMapList(arrayKey);

        // 空对象直接返回空集合
        if (ContainerUtils.isEmptyCollection(resultList)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new ParamInfo(arrayKey, Collections.singletonList(new ParamInfo(arrayKey, resultList))));
    }
}
