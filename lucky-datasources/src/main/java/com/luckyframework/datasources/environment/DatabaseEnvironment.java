package com.luckyframework.datasources.environment;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.environment.v1.AbstractEnvironment;
import com.luckyframework.environment.v1.ConfigurationMapStorageUnit;
import com.luckyframework.environment.v1.StorageUnit;
import com.luckyframework.exception.EnvironmentSettingException;

import java.util.Map;

/**
 * 数据库环境变量
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 3:06 下午
 */
public class DatabaseEnvironment extends AbstractEnvironment {

    private StorageUnit dataUnit;

    public DatabaseEnvironment(ConfigurationMap databaseData){
        dataUnit = new ConfigurationMapStorageUnit(databaseData);
    }


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
        throw new EnvironmentSettingException("Unable to set databases environment variable.");
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
