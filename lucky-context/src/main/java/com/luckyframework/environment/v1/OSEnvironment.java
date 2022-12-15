package com.luckyframework.environment.v1;

import com.luckyframework.exception.EnvironmentSettingException;

import java.util.Map;

/**
 * 系统环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午7:47
 */
public class OSEnvironment extends AbstractEnvironment{

    private final static StorageUnit dataUnit = new ConfigurationMapStorageUnit(System.getenv());

    @Override
    public Object getProperty(String key) {
        return dataUnit.getRealValue(key);
    }

    @Override
    public Object parsSingleExpression(String single$Expression) {
        return dataUnit.parsSingleExpression(single$Expression);
    }

    @Override
    public Object parsExpression(Object $Expression) {
        return dataUnit.parsExpression($Expression);
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new EnvironmentSettingException("Unable to set system environment variable.");
    }

    @Override
    public Map<String, Object> getProperties() {
        return dataUnit.getRealMap();
    }

    @Override
    public Map<String, Object> getOriginalMap() {
        return dataUnit.getOriginalMap();
    }

    @Override
    public boolean containsKey(String key) {
        return getOriginalMap().containsKey(key);
    }

}
