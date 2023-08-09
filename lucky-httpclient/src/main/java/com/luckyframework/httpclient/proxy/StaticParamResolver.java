package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.TempPair;

import java.lang.annotation.Annotation;
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
     * 参数解析，将注解解析成为参数集合<br/>
     * 注：<br/>
     * TempPair-One -- paramName<br/>
     * TempPair-Two -- paramValue<br/>
     *
     * @param staticParamAnn 静态参数注解实例
     * @return 参数集合
     */
    List<TempPair<String, Object>> parser(Annotation staticParamAnn);
}
