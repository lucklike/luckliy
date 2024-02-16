package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.ResultSelect;
import com.luckyframework.httpclient.proxy.annotations.VoidResultSelect;

import java.lang.reflect.Type;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTENT_LENGTH;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_BODY;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_COOKIE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_HEADER;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.RESPONSE_STATUS;

/**
 * 获取指定值的转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/19 22:44
 */
public class VoidResponseSelectConvert extends AbstractSpELVoidResponseConvert {

    @Override
    public <T> T convert(VoidResponse voidResponse, ConvertContext context) {
        // 获取配置
        String select = context.toAnnotation(VoidResultSelect.class).value();
        Type resultType = context.getContext().getRealMethodReturnType();


        // 做了取值配置时，会在响应对象外层包装一层ConfigurationMap
        ConfigurationMap resultMap = new ConfigurationMap();
        if (RESPONSE_STATUS.equals(select)) {
            resultMap.addProperty(RESPONSE_STATUS, voidResponse.getStatus());
        }
        if (CONTENT_LENGTH.equals(select)) {
           resultMap.addProperty(CONTENT_LENGTH, voidResponse.getContentLength());
        }
        if (select.startsWith(RESPONSE_HEADER)) {
            resultMap.addProperty(RESPONSE_HEADER, voidResponse.getHeaderManager().getSimpleHeaders());
        }
        if (select.startsWith(RESPONSE_COOKIE)) {
            resultMap.addProperty(RESPONSE_COOKIE, voidResponse.getSimpleCookies());
        }

        // 指定值存在时，取出指定值进行转换
        if (resultMap.containsConfigKey(select)) {
            return resultMap.getEntry(select, resultType);
        }

        // 指定值不存在时，尝试通过defaultValue属性中配置的SpEL表达式来获取默认值
       return getDefaultValue(voidResponse, context);
    }

}
