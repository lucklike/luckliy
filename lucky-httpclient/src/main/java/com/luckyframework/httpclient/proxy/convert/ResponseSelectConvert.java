package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.ResultSelect;

import java.lang.reflect.Type;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_COOKIE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADER;

/**
 * 获取指定值的转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/19 22:44
 */
public class ResponseSelectConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) {
        // 获取配置
        String mapKey = context.toAnnotation(ResultSelect.class).select();
        Type resultType = context.getContext().getRealMethodReturnType();

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(mapKey)) {
            return response.getEntity(resultType);
        }

        // 做了取值配置时，会在响应对象外层包装一层ConfigurationMap
        ConfigurationMap resultMap = new ConfigurationMap();
        if (mapKey.startsWith(RESPONSE_BODY)) {
            resultMap.addProperty(RESPONSE_BODY, getBodyResult(response));
        }
        if (mapKey.startsWith(RESPONSE_HEADER)) {
            resultMap.addProperty(RESPONSE_HEADER, response.getHeaderManager().getSimpleHeaders());
        }
        if (mapKey.startsWith(RESPONSE_COOKIE)) {
            resultMap.addProperty(RESPONSE_COOKIE, response.getSimpleCookies());
        }

        // 指定值存在时，取出指定值进行转换
        if (resultMap.containsConfigKey(mapKey)) {
            return resultMap.getEntry(mapKey, resultType);
        }

        // 指定值不存在时，尝试通过defaultValue属性中配置的SpEL表达式来获取默认值
       return getDefaultValue(response, context);
    }

}
