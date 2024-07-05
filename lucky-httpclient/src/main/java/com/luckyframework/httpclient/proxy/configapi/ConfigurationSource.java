package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;

/**
 * 配置源接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 21:06
 */
@FunctionalInterface
public interface ConfigurationSource {

    /**
     * 获取一个配置源
     *
     * @param source 配置源信息
     * @param prefix 配置前缀
     * @return 配置源对象
     */
    ConfigurationMap getConfigMap(String source, String prefix);

}
