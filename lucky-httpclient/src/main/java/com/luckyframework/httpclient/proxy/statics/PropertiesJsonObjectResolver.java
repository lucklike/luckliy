package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.FlatBean;
import com.luckyframework.httpclient.proxy.annotations.CombinablePropJson;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        CombinablePropJson jsonAnn = context.toAnnotation(CombinablePropJson.class);

        FlatBean<?> flatBean = null;
        String separator = jsonAnn.separator();
        for (String expression : jsonAnn.value()) {
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            String key = String.valueOf(propertyParamInfo.getName());
            Object value = propertyParamInfo.getValue();

            if (flatBean == null) {
                if (firstIsArrayKey(key)) {
                    flatBean = FlatBean.of(new ArrayList<>());
                } else {
                    flatBean = FlatBean.of(new LinkedHashMap<>());
                }
            }

            flatBean.set(key, value);
        }
        return Collections.singletonList(new ParamInfo("", flatBean));
    }


    private boolean firstIsArrayKey(String str) {
        if (str == null || str.length() < 3 || str.charAt(0) != '[') {
            return false;
        }

        int i = 1;
        // 检查数字部分
        while (i < str.length() && Character.isDigit(str.charAt(i))) {
            i++;
        }

        // 确保有数字且以']'结尾
        return i > 1 && i < str.length() && str.charAt(i) == ']';
    }

}
