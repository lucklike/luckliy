package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.annotations.JsonParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.spel.hook.callback.VarUnfoldException;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * 返回JSON参数的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/19 18:30
 */
public class JsonParamDynamicParamResolver  implements DynamicParamResolver {

    @Override
    public List<? extends ParamInfo> parser(DynamicParamContext context) {
        JsonParam jsonParamAnn = context.toAnnotation(JsonParam.class);
        ValueContext valueContext = context.getContext();
        // 配置为不展开时
        if (!jsonParamAnn.unfold()) {
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), valueContext.getValue()));
        }

        // 配置为展开时
        try {
            Map<String, Object> convertMap = ConversionUtils.conversion(valueContext.getValue(), new SerializationTypeToken<Map<String, Object>>() {
            });
            List<ParamInfo> paramInfos = new ArrayList<>(convertMap.size());
            convertMap.forEach((k, v) -> paramInfos.add(new ParamInfo(k, v)));
            return paramInfos;
        } catch (Exception e) {
            throw new VarUnfoldException(e, "An exception occurs when expanding the Object property: '{}': {}", valueContext.getName(), valueContext.getValue());
        }

    }

}
