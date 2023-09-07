package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.ResponseConvert;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.spel.ParamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.common.TemplateParserContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 获取指定值的转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/19 22:44
 */
public class ResponseSelectConvert implements ResponseConvert {
    private static final Logger log = LoggerFactory.getLogger(ResponseSelectConvert.class);

    /**
     * 取值表达式约定的前缀
     */
    private static final String CONVENTION_PREFIX = "@resp";

    @Override
    public <T> T convert(Response response, Type resultType, Annotation resultConvertAnn) throws Exception {
        // 获取配置
        String mapKey = AnnotationUtils.getValue(resultConvertAnn, "key", String.class);
        String defaultValueSpEL = AnnotationUtils.getValue(resultConvertAnn, "defaultValue", String.class);

        // 没有做任何配置时，直接对返回值进行转换
        if (!StringUtils.hasText(mapKey)) {
            return response.getEntity(resultType);
        }

        // 做了取值配置时，会在响应对象外层包装一层ConfigurationMap
        ConfigurationMap resultMap = new ConfigurationMap();
        if (response.isJsonType()) {
            resultMap.addProperty(CONVENTION_PREFIX, response.jsonStrToEntity(Object.class));
        } else if (response.isXmlType()) {
            resultMap.addProperty(CONVENTION_PREFIX, response.xmlStrToEntity(Object.class));
        }

        // 指定值存在时，取出指定值进行转换
        if (resultMap.containsConfigKey(mapKey)) {
            return resultMap.getEntry(mapKey, resultType);
        }

        // 指定值不存在时，尝试通过defaultValue属性中配置的SpEL表达式来获取默认值
        if (StringUtils.hasText(defaultValueSpEL)) {
            log.warn("The content specified by '{}' does not exist in the response body. The default configuration is enabled", mapKey);
            return HttpClientProxyObjectFactory
                    .getSpELConverter()
                    .getSpELRuntime()
                    .getValueForType(new ParamWrapper(defaultValueSpEL)
                            .setParserContext(new TemplateParserContext())
                            .setExpectedResultType(resultType));
        }

        throw new ResponseProcessException("A value for '{}' does not exist in the response body, and the default value configuration is not checked", mapKey);
    }


}
