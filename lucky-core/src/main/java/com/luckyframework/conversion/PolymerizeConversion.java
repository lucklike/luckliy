package com.luckyframework.conversion;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 聚合转换器，将多个原对象聚合转换为一个目标对象
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/11 16:20
 */
public class PolymerizeConversion<T> {
    
    private final Class<T> targetClass;

    private final Map<String, String> mapping;
    private Supplier<T> targetSupplier;
    private final Map<String, Supplier<Object>> targetFieldsSupplier;
    private final Map<String, Object> sourceNamedObjectMap;
    /** SpEL表达式运行时环境*/
    private SpELRuntime spELRuntime = new SpELRuntime();
    /** 导入的SpEL包*/
    private final List<String> spelImports = new ArrayList<>();



    public PolymerizeConversion(@NonNull Class<T> targetClass,
                                @NonNull Map<String, String> mapping,
                                @NonNull Supplier<T> targetSupplier,
                                @NonNull Map<String, Supplier<Object>> targetFieldsSupplier,
                                @NonNull Map<String, Object> sourceNamedObjectMap) {
        this.targetClass = targetClass;
        this.mapping = mapping;
        this.targetSupplier = targetSupplier;
        this.targetFieldsSupplier = targetFieldsSupplier;
        this.sourceNamedObjectMap = sourceNamedObjectMap;
    }

    public PolymerizeConversion(@NonNull Class<T> targetClass){
        this(targetClass, new HashMap<>(), () -> ClassUtils.newObject(targetClass), new HashMap<>(), new HashMap<>());
    }



    public T polymerize(){
        T targetObject = targetSupplier.get();
        mapping.keySet().forEach((fc) -> SpELConversion.initializeTargetObjectFieldChain(spELRuntime, getImportPackages(), targetObject, targetFieldsSupplier, fc));
        targetMappingSetting(targetObject);
        return targetObject;
    }

    private void targetMappingSetting(T targetObject) {
        mapping.forEach((targetFieldName, valueExpression) -> {
            Object value = spELRuntime.getValueForType(new ParamWrapper(valueExpression).setVariables(sourceNamedObjectMap).importPackage(getImportPackages()));
            ResolvableType fieldType = spELRuntime.getValueForType(new ParamWrapper(targetFieldName).setRootObject(targetObject.getClass()).importPackage(getImportPackages()));
            spELRuntime.setValue(new ParamWrapper(targetFieldName).setRootObject(targetObject).importPackage(getImportPackages()), ConversionUtils.conversion(value, fieldType));
        });
    }

    public PolymerizeConversion<T> addMapping(String targetName, String valueExpression){
        mapping.put(targetName, valueExpression);
        return this;
    }

    public PolymerizeConversion<T> addMappings(Map<String, String> mapping){
        this.mapping.putAll(mapping);
        return this;
    }

    public PolymerizeConversion<T> removeMapping(String ...fields){
        for (String field : fields) {
            mapping.remove(field);
        }
        return this;
    }

    public PolymerizeConversion<T> setInitialTargetSuppler(@NonNull Supplier<T> targetSupplier){
        this.targetSupplier = targetSupplier;
        return this;
    }

    public PolymerizeConversion<T> setInitialTargetSuppler(@NonNull String spelExpression){
        this.targetSupplier = () -> spELRuntime.getValueForType(new ParamWrapper(spelExpression).setExpectedResultType(targetClass).importPackage(getImportPackages()));
        return this;
    }

    public PolymerizeConversion<T> addFieldConstruction(String targetName, String fieldConstructionEx){
        this.targetFieldsSupplier.put(targetName, () -> spELRuntime.getValueForType(new ParamWrapper(fieldConstructionEx).importPackage(getImportPackages())));
        return this;
    }

    public PolymerizeConversion<T> addFieldConstruction(String targetName, Supplier<Object> fieldConstruction){
        this.targetFieldsSupplier.put(targetName, fieldConstruction);
        return this;
    }

    public PolymerizeConversion<T> addSourceObject(String sourceName, Object sourceObject){
        this.sourceNamedObjectMap.put(sourceName, sourceObject);
        return this;
    }

    /**
     * 获取SpEL运行时环境
     * @return SpEL运行时环境
     */
    public SpELRuntime getSpELRuntime() {
        return spELRuntime;
    }

    /**
     * 设置SpEL表达式的运行环境
     * @param spELRuntime SpEL表达式的运行环境
     * @return 对象本身
     */
    public PolymerizeConversion<T> setSpELRunTime(SpELRuntime spELRuntime){
        this.spELRuntime = spELRuntime;
        return this;
    }

    /**
     * 导入一个执行SpEL表达式时所依赖的包
     * @param packageNames 依赖包
     * @return 对象本身
     */
    public PolymerizeConversion<T> importPackages(String... packageNames){
        for (String packageName : packageNames) {
            if(!this.spelImports.contains(packageName)){
                this.spelImports.add(packageName);
            }
        }
        return this;
    }

    /**
     * 获取执行SpEL表达式时所依赖的包
     * @return 执行SpEL表达式时所依赖的包
     */
    public String[] getImportPackages(){
        return this.spelImports.toArray(new String[0]);
    }

}
