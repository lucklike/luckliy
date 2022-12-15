package com.luckyframework.environment.v1;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * JVM环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午10:59
 */
public class JVMEnvironment extends AbstractEnvironment{

    private final static StorageUnit dataUnit;
    static {
        RuntimeMXBean MXB = ManagementFactory.getRuntimeMXBean();
        System.setProperty("PID",MXB.getName().split("@")[0]);
        Properties properties = System.getProperties();
        Map<String,Object> jvmMap = new HashMap<>(properties.size());
        properties.forEach((k,v)-> jvmMap.put(k.toString(),v));
        dataUnit = new ConfigurationMapStorageUnit(jvmMap);
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
        String valueString = value == null ? "" : value.toString();
        dataUnit.setProperties(key,valueString);
        System.setProperty(key,dataUnit.changeToReal(valueString).toString());
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
