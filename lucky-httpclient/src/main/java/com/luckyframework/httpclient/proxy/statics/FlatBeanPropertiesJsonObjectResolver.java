package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.FlatBean;
import com.luckyframework.common.ObjectUtils;
import com.luckyframework.httpclient.proxy.annotations.CombinablePropJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * properties文件格式的JSON对象请求体解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class FlatBeanPropertiesJsonObjectResolver extends AbstractPropertiesJsonResolver {



    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        CombinablePropJson jsonAnn = context.toAnnotation(CombinablePropJson.class);

        FlatBean<?> flatBean = null;
        String separator = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            String key = String.valueOf(propertyParamInfo.getName());
            Object value = propertyParamInfo.getValue();

            if (flatBean == null) {
                if (ObjectUtils.firstIsArrayKey(key)) {
                    flatBean = FlatBean.of(new ArrayList<>());
                } else {
                    flatBean = FlatBean.of(new LinkedHashMap<>());
                }
            }

            flatBean.set(key, value);
        }
        return Collections.singletonList(new ParamInfo("", flatBean));
    }

}
