package com.luckyframework.httpclient.proxy.impl.convert;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.MethodContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 获取指定值的转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/19 22:44
 */
public class ResponseSelectConvert extends AbstractSpELResponseConvert {
    /**
     * 取值表达式约定的前缀
     */
    private static final String CONVENTION_PREFIX = "@resp";

    @Override
    public <T> T convert(Response response, MethodContext methodContext, Annotation resultConvertAnn) {
        // 获取配置
        String mapKey = methodContext.getAnnotationAttribute(resultConvertAnn, "key", String.class);
        Type resultType = methodContext.getRealMethodReturnType();

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(mapKey)) {
            return response.getEntity(resultType);
        }

        // 做了取值配置时，会在响应对象外层包装一层ConfigurationMap
        ConfigurationMap resultMap = new ConfigurationMap();
        resultMap.addProperty(CONVENTION_PREFIX, getResponseResult(response));

        // 指定值存在时，取出指定值进行转换
        if (resultMap.containsConfigKey(mapKey)) {
            return resultMap.getEntry(mapKey, resultType);
        }

        // 指定值不存在时，尝试通过defaultValue属性中配置的SpEL表达式来获取默认值
       return getDefaultValue(response, methodContext, resultConvertAnn);
    }


}
