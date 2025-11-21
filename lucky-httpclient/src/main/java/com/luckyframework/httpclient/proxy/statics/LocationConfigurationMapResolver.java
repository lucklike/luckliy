package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.proxy.annotations.Location;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 支持位置注解的ConfigurationMap动态资源解析器
 */
public abstract class LocationConfigurationMapResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        Location locationAnn = context.toAnnotation(Location.class);
        String location = context.parseExpression(locationAnn.value(), String.class);
        String arrayKey = context.parseExpression(locationAnn.array(), String.class);
        return Collections.singletonList(new ParamInfo(arrayKey, loadConfig(context, location)));
    }

    /**
     * 加载指定位置的资源
     *
     * @param context  上下文信息
     * @param location 位置信息
     * @return 配置信息
     */
    protected abstract ConfigurationMap loadConfig(StaticParamAnnContext context, String location);
}
