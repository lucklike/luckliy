package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.List;

/**
 * 静态参数解析器，用户将用户配置再注解中的信息转化为Http参数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 09:42
 */
@FunctionalInterface
public interface StaticParamResolver {

    /**
     * 参数解析，将注解解析成为参数集合
     *
     * @param context 静态注解上下文信息
     * @return 参数集合
     */
    List<ParamInfo> parser(StaticParamAnnContext context);

}
