package com.luckyframework.environment.v1;

import com.luckyframework.common.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lucky环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/26 下午10:30
 */
public class LuckyEnvironment extends AbstractEnvironment{

    private final static Environment osEnv = new OSEnvironment();
    private final static Environment jvmEnv = new JVMEnvironment();
    private final Environment confEnv;
    private final StorageUnit dataUnit;

    LuckyEnvironment(){
        confEnv = new ConfigurationEnvironment();
        addSystemProperty();
        ConfigurationMap luckyEnvMap = new ConfigurationMap();
        luckyEnvMap.mergeConfig(confEnv.getOriginalMap());
        Map<String, Object> jvmEnvOriginalMap = jvmEnv.getOriginalMap();
        //让JVM参数直接作用在Config环境中，只有Config中有对应的key时才发生作用
        luckyEnvMap.addAsItExists(jvmEnvOriginalMap);
        //将JVM参数直接加到Config环境中
        luckyEnvMap.mergeConfig(jvmEnvOriginalMap);
        luckyEnvMap.mergeConfig(osEnv.getOriginalMap());
        dataUnit = new ConfigurationMapStorageUnit(luckyEnvMap);
    }

    public LuckyEnvironment(List<AnnotationMetadata> componentAnnotationMetadata){
        this(new PropertySourceEnvironment(componentAnnotationMetadata));
    }

    public LuckyEnvironment(@NonNull PropertySourceEnvironment propertyEnvironment){
        confEnv = new ConfigurationEnvironment(propertyEnvironment);
        addSystemProperty();
        ConfigurationMap luckyEnvMap = new ConfigurationMap();
        luckyEnvMap.mergeConfig(confEnv.getOriginalMap());
        Map<String, Object> jvmEnvOriginalMap = jvmEnv.getOriginalMap();
        //让JVM参数直接作用在Config环境中，只有Config中有对应的key时才发生作用
        luckyEnvMap.addAsItExists(jvmEnvOriginalMap);
        //将JVM参数直接加到Config环境中
        luckyEnvMap.mergeConfig(jvmEnvOriginalMap);
        luckyEnvMap.mergeConfig(osEnv.getOriginalMap());
        addDefaultConfig(luckyEnvMap);
        dataUnit = new ConfigurationMapStorageUnit(luckyEnvMap);
    }

    private void addDefaultConfig(ConfigurationMap luckyEnvMap) {

    }

    private void addSystemProperty(){
        Map<String, Object> originalMap = confEnv.getOriginalMap();
        originalMap.put("systemProperties", jvmEnv.getOriginalMap());
        originalMap.put("systemEnvironment", osEnv.getOriginalMap());
    }

    @Override
    public Object getProperty(String key) {
        return dataUnit.getRealValue(key);
    }

    @Override
    public Object parsSingleExpression(String single$Expression) {
        single$Expression = single$Expression.trim();
        return getProperty(single$Expression.substring(2, single$Expression.length()-1));
    }

    @Override
    public Object parsExpression(Object $Expression) {
        if($Expression instanceof String){
            String expression = (String) $Expression;
            TempPair<String[], List<String>> regularCut = StringUtils.regularCut(expression, Pattern.compile(Regular.$_$));
            List<String> regularEx = regularCut.getTwo();
            if(ContainerUtils.isEmptyCollection(regularEx)){
                return $Expression;
            }
            if(regularEx.size() == 1 && expression.trim().startsWith("${") && expression.endsWith("}")){
                return parsSingleExpression(regularEx.get(0));
            }

            List<Object> regularValue = new ArrayList<>(regularEx.size());
            for (String ex : regularEx) {
                regularValue.add(parsSingleExpression(ex));
            }
            return StringUtils.misalignedSplice(regularCut.getOne(), regularValue.toArray(new Object[0]));
        }
        return $Expression;
    }

    @Override
    public synchronized void setProperty(String key, Object value) {
        if(!osEnv.containsKey(key) && !jvmEnv.containsKey(key)){
            dataUnit.setProperties(key, value);
        }
    }

    public synchronized void setJvmEnv(String key, Object value) {
        if(!osEnv.containsKey(key)){
            dataUnit.setProperties(key, value);
            Object property = getProperty(key);
            System.setProperty(key,property == null?"null":property.toString());
        }
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
    public String getProfiles() {
        return this.confEnv.getProfiles();
    }

    @Override
    public boolean containsKey(String key) {
        if(osEnv.containsKey(key) || jvmEnv.containsKey(key)){
            return true;
        }
        return confEnv.containsKey(key);
    }

}
