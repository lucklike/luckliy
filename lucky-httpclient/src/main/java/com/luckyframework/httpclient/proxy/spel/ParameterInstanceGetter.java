package com.luckyframework.httpclient.proxy.spel;

import org.springframework.lang.Nullable;

import java.lang.reflect.Parameter;

@FunctionalInterface
public interface ParameterInstanceGetter {

    /**
     * 获取参数对应的实例对象，无法获取时返回空
     *
     * @param parameterInfo 参数信息
     * @return 参数对应的实例对象
     */
    @Nullable
    Object getParameterInstance(ParameterInfo parameterInfo);
}
