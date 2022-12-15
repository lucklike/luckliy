package com.luckyframework.common;

import java.util.Map;

/**
 * 支持纯字符串操作的Map接口
 * @author fk7075
 * @version 1.0
 * @date 2021/9/17 5:52 下午
 */
public interface SupportsStringManipulationMap {


    Object getConfigProperty(String configKey);

    void addConfigProperty(String configKey,Object confValue);

    void addConfigProperties(Map<?,?> properties);

    void addAsItExists(String configKey,Object confValue);

    void addAsItExists(Map<String,Object> properties);

    boolean containsConfigKey(String configKey);

    Object removeConfigProperty(String configKey);

    void mergeConfig(Map<String,Object> newConfigMap);

    Object putIn(String sourceKey,String newKey,Object newValue);

    void putEntity(Object entity);
}
