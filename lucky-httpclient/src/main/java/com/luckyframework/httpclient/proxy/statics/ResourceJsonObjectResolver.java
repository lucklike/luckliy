package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.annotations.CombinableResJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从资源中中提取JSON对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/21 02:12
 */
public class ResourceJsonObjectResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        CombinableResJson fileJsonAnn = context.toAnnotation(CombinableResJson.class);
        String resourcePath = context.parseExpression(fileJsonAnn.value(), String.class);

        // 读取资源内容
        ConfigurationMap resourceContentMap = Resources.resourceAsConfigMap(resourcePath, fileJsonAnn.charset());

        // 转成ParamInfo集合
        List<ParamInfo> paramInfos = new ArrayList<>(resourceContentMap.size());
        resourceContentMap.forEach((k, v) -> paramInfos.add(new ParamInfo(k, v)));

        return Collections.singletonList(new ParamInfo("resourceJson", paramInfos));
    }
}
