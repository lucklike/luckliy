package com.luckyframework.loosebind;

import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 松散绑定
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 12:41
 */
public class LooseBind {

    public static final char[] DEFAULT_TO_HUMP = {'-','_'};

    /** 忽略无效字段*/
    private final boolean ignoreInvalidField;
    /** 忽略未知字段*/
    private final boolean ignoreUnknownField;
    /** 是否启用字段优先的匹配方式*/
    private final boolean isFieldPriorityMatch;
    /** 松散绑定中指定的分隔符*/
    private final char[] toHump;
    /** 必要的属性组*/
    private final List<FieldAlias> fieldAliasList = new ArrayList<>();

    private InjectionFactorFilter factorBuilderFilter = InjectionFactorFilter.ALWAYS_TRUE_INSTANCE;

    public LooseBind(boolean ignoreInvalidField, boolean ignoreUnknownField, boolean isFieldPriorityMatch, char[] toHump) {
        this.ignoreInvalidField = ignoreInvalidField;
        this.ignoreUnknownField = ignoreUnknownField;
        this.isFieldPriorityMatch = isFieldPriorityMatch;
        this.toHump = toHump;
    }

    public LooseBind(){
        this(false, true, false, DEFAULT_TO_HUMP);
    }

    public void addFieldAlias(FieldAlias fieldAlias){
        this.fieldAliasList.add(fieldAlias);
    }

    public void setFactorBuilderFilter(InjectionFactorFilter factorBuilderFilter){
        this.factorBuilderFilter = factorBuilderFilter;
    }

    /**
     * 给Bean注入环境变量中属性
     * @param bean  Bean实例
     * @param configProperties 配置项
     */
    public void binding(Object bean,  Map<String,Object> configProperties) throws FieldUnknownException, FieldInvalidException {
        Class<?> beanClass = bean.getClass();
        Map<String, Field> nameFieldMap = new KeyCaseSensitivityMap<>(ClassUtils.getNameFieldMap(beanClass));
        Map<String, Method> nameSetterMethodMap = ClassUtils.getAllSetterMethods(beanClass);

        for (Map.Entry<String, Object> confEntry : configProperties.entrySet()) {
            String configKey = confEntry.getKey();
            Object configValue = confEntry.getValue();
            InjectionFactorBuilder injectionFactorBuilder;
            // 优先匹配原则 --> 属性匹配优先
            if(isFieldPriorityMatch){
                injectionFactorBuilder = findFieldInjectionFactorBuilder(configKey, configValue, nameFieldMap);
                injectionFactorBuilder = injectionFactorBuilder == null ? findMethodInjectionFactorBuilder(configKey, configValue, nameSetterMethodMap) : injectionFactorBuilder;
            }
            // 优先匹配原则 --> 方法匹配优先
            else{
                injectionFactorBuilder = findMethodInjectionFactorBuilder(configKey, configValue, nameSetterMethodMap);
                injectionFactorBuilder = injectionFactorBuilder == null ? findFieldInjectionFactorBuilder(configKey, configValue, nameFieldMap) : injectionFactorBuilder;
            }


            if(injectionFactorBuilder == null && !ignoreUnknownField){
                throw new FieldUnknownException(configKey);
            }

            if (injectionFactorBuilder != null && factorBuilderFilter.pass(injectionFactorBuilder)){
                try {
                    injectionFactorBuilder.setBeanInstance(bean).builder().injection();
                }catch (Exception e){
                    if(!ignoreInvalidField){
                        throw new FieldInvalidException(configKey, injectionFactorBuilder.getFactor(), e);
                    }
                }
            }
        }
    }

    public static void checkConfigProperties(List<FieldAlias> requiredFieldAliases, Map<String, Object> configProperties, char[] toHump) throws NotFoundRequiredFieldException {
        if(!requiredFieldAliases.isEmpty()){
            KeyCaseSensitivityMap<Object> kcsMap = new KeyCaseSensitivityMap<>(configProperties);
            for (FieldAlias requiredFieldAlias : requiredFieldAliases) {
                String requiredFieldName = requiredFieldAlias.getName();
                Set<String> aliasSet = requiredFieldAlias.getAliases();
                aliasSet.add(requiredFieldName);
                if(!containsKey(kcsMap, aliasSet, toHump)){
                    throw new NotFoundRequiredFieldException(aliasSet);
                }
            }
        }
    }

    private static boolean containsKey(KeyCaseSensitivityMap<Object> kcsMap, Set<String> keys, char[] toHump){
        for (String key : keys) {
            if(kcsMap.containsKey(key)){
                return true;
            }
            for (char to : toHump) {

                String toKey2 = StringUtils.humpToOtherFormats(key, String.valueOf(to));
                if(kcsMap.containsKey(toKey2)){
                    return true;
                }

                String toKey1 = StringUtils.otherFormatsToCamel(key, to);
                if(kcsMap.containsKey(toKey1)){
                    return true;
                }

            }
        }
        return false;
    }


    /**
     * 获取一个基于方法的属性注入因子的构造器
     * @param configKey         配置Key
     * @param configValue       配置Value
     * @param setterMethodMap   Setter方法名与方法组成的Map
     * @return  基于方法的注入因子构造器
     */
    private InjectionFactorBuilder findMethodInjectionFactorBuilder(String configKey, Object configValue, Map<String,Method> setterMethodMap){
        Map<String, Method> temp = new LinkedHashMap<>();
        setterMethodMap.forEach((k,v) -> temp.put(k.substring(3), v));
        Method method = getInjection(configKey, temp);
        return method == null ? null : new InjectionFactorBuilder().setFactor(method).setFactoryValue(configValue);
    }

    /**
     * 获取一个基于属性的属性注入因子的构造器
     * @param configKey         配置Key
     * @param configValue       配置Value
     * @param nameFieldMap      属性名与属性组成的Map
     * @return 基于属性的注入因子构造器
     */
    private InjectionFactorBuilder findFieldInjectionFactorBuilder(String configKey, Object configValue, Map<String,Field> nameFieldMap){
        Field field = getInjection(configKey, nameFieldMap);
        return field == null ? null : new InjectionFactorBuilder().setFactor(field).setFactoryValue(configValue);
    }

    /**
     * 获取一个注入因子
     * @param configKey         配置Key
     * @param nameFieldMap      属性名与属性组成的Map
     * @return                  注入因子
     * @param <T>               注入因子类型
     */
    private <T> T getInjection(String configKey, Map<String, T> nameFieldMap){
        Map<String, T> injectionMap = new KeyCaseSensitivityMap<>(nameFieldMap);
        T injection = injectionMap.get(configKey);
        if(injection == null){
            for (char to : toHump) {

                // 尝试转成驼峰格式后进行匹配
                String toKey1 = StringUtils.otherFormatsToCamel(configKey, to);
                if(nameFieldMap.containsKey(toKey1)){
                    injection = nameFieldMap.get(toKey1);
                    break;
                }

                // 尝试将驼峰转成其他之后进行匹配
                String toKey2 = StringUtils.humpToOtherFormats(configKey, String.valueOf(to));
                if(nameFieldMap.containsKey(toKey2)){
                    injection = nameFieldMap.get(toKey2);
                    break;
                }
            }
            if(injection == null){
                injection = getRequiredInjection(injectionMap, configKey);
            }
        }
        return injection;
    }

    private <T> T getRequiredInjection(Map<String, T> nameFieldMap, String configKey){
        for (FieldAlias fieldAlias : fieldAliasList) {
            String fieldName = fieldAlias.getName();
            Set<String> aliases = fieldAlias.getAliases();

            boolean isEqualConfigKey = isEqualKey(configKey, fieldName);
            if(!isEqualConfigKey){
                for (String alias : aliases) {
                    if(isEqualKey(configKey, alias)){
                        isEqualConfigKey = true;
                        break;
                    }
                }
            }

            if(isEqualConfigKey && nameFieldMap.containsKey(fieldName)){
                return nameFieldMap.get(fieldName);
            }
        }
        return null;
    }

    private boolean isEqualKey(String key, String testKey){
        if(key.equalsIgnoreCase(testKey)) return true;
        for (char to : toHump) {
            String toKey = StringUtils.otherFormatsToCamel(key,to);
            String toTestKey = StringUtils.otherFormatsToCamel(testKey,to);
            if(toKey.equalsIgnoreCase(toTestKey)){
                return true;
            }
        }
        return false;
    }

    /**
     * 注入因子过滤器接口
     */
    public interface InjectionFactorFilter {

        InjectionFactorFilter ALWAYS_TRUE_INSTANCE = new AlwaysTrueInjectionFactorFilter();

        boolean pass(InjectionFactorBuilder factorBuilder);
    }

    static class AlwaysTrueInjectionFactorFilter implements InjectionFactorFilter {
        @Override
        public boolean pass(InjectionFactorBuilder factorBuilder) {
            return true;
        }
    }

    /**
     * 注入因子构造器，用于生产一个{@link InjectionFactor}
     */
    @SuppressWarnings("all")
    public static class InjectionFactorBuilder{
        private Object beanInstance;
        private Object factor;
        private Object factoryValue;

        public Object getBeanInstance() {
            return beanInstance;
        }

        public Object getFactor() {
            return factor;
        }

        public Object getFactoryValue() {
            return factoryValue;
        }

        public InjectionFactorBuilder setBeanInstance(Object beanInstance){
            this.beanInstance = beanInstance;
            return this;
        }

        public InjectionFactorBuilder setFactor(Object factor){
            this.factor = factor;
            return this;
        }

        public InjectionFactorBuilder setFactoryValue(Object factoryValue){
            this.factoryValue = factoryValue;
            return this;
        }

        public InjectionFactor builder(){
            return new InjectionFactor(beanInstance, factor, factoryValue);
        }

    }

    /**
     * 注入因子
     * 负责将值赋值给某个对象的某个属性或者执行其Setter方法
     */
    static class InjectionFactor{

        /** Bean实例*/
        private final Object beanInstance;
        /** 注入因子[Field/Method]*/
        private final Object factor;
        /** 值*/
        private final Object factoryValue;
        /** 转化的目标类型*/
        private final ResolvableType factoryType;

        InjectionFactor(Object beanInstance, Object factor, Object factoryValue) {
            this.beanInstance = beanInstance;
            this.factor = factor;
            this.factoryValue = factoryValue;
            if(factor instanceof Field){
                factoryType = ResolvableType.forField((Field) factor);
            } else if(factor instanceof Method){
                factoryType = ResolvableType.forType(((Method)factor).getGenericParameterTypes()[0]);
            } else {
                throw new IllegalArgumentException("Unknown injection factor type !");
            }
        }

        /**
         * 执行属性注入操作
         */
        public void injection(){
            if(factor instanceof Field){
                setFieldValue(beanInstance,(Field)factor, ConversionUtils.conversion(factoryValue, factoryType));
            } else {
                invokeSetterMethod(beanInstance,(Method)factor,ConversionUtils.conversion(factoryValue, factoryType));
            }
        }

        private void setFieldValue(Object object, Field field, Object fieldValue){
            if(Modifier.isStatic(field.getModifiers())){
                FieldUtils.setValue(object.getClass(), field, fieldValue);
            } else {
                FieldUtils.setValue(object, field, fieldValue);
            }
        }

        private void invokeSetterMethod(Object object, Method method, Object fieldValue){
            if(Modifier.isStatic(method.getModifiers())){
                MethodUtils.invoke(object.getClass(), method, fieldValue);
            } else {
                MethodUtils.invoke(object, method, fieldValue);
            }
        }
    }
}
