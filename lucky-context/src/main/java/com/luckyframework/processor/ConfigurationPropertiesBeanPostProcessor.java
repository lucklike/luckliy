package com.luckyframework.processor;


import com.luckyframework.annotations.*;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.*;
import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.ConfigurationPropertiesInjectionException;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 给被{@link ConfigurationProperties @ConfigurationProperties}注解标注的Bean注入属性的BeanPostProcessor
 */
@DisableProxy
public class ConfigurationPropertiesBeanPostProcessor implements BeanPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(String beanName, FactoryBean factoryBean, Object bean) {
        Class<?> beanClass = bean.getClass();

        ConfigurationProperties configurationProperties = null;
        // 由构造方法生产的Bean
        if(factoryBean instanceof ConstructorFactoryBean){
            configurationProperties = AnnotatedElementUtils.getMergedAnnotation(beanClass, ConfigurationProperties.class);
        }
        // 由工厂方法生产的Bean
        else if (factoryBean instanceof MethodFactoryBean){
            Method beanMethod = ((MethodFactoryBean) factoryBean).findMethod();
            configurationProperties = AnnotatedElementUtils.getMergedAnnotation(beanMethod, ConfigurationProperties.class);
        }
        // 由静态工厂生产的Bean
        else if(factoryBean instanceof StaticMethodFactoryBean){
            Method staticMethod = ((StaticMethodFactoryBean) factoryBean).findStaticMethod();
            configurationProperties = AnnotatedElementUtils.getMergedAnnotation(staticMethod, ConfigurationProperties.class);
        }
        if(configurationProperties != null){
            injectConfigProperties(bean, configurationProperties);
        }
        return bean;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 给Bean注入环境变量中属性
     * @param bean  Bean实例
     * @param configurationProperties ConfigurationProperties注解实例
     */
    private void injectConfigProperties(Object bean, ConfigurationProperties configurationProperties){
        Class<?> beanClass = bean.getClass();
        String prefix = configurationProperties.prefix();

        if(environment.containsProperty(prefix)){
            Object property = ((LuckyStandardEnvironment)environment).getPropertyForObject(prefix);
            if(!(property instanceof Map)){
                throw new ConfigurationPropertiesInjectionException("An exception occurred while injecting attributes into the '" + beanClass + "' class annotated by the @ConfigurationProperties(prefix='"+prefix+"') annotation. The value type '"+property.getClass()+"' corresponding to '"+prefix+"' in the environment variable is invalid");
            }

            Map<String,Object> envMap = (Map<String, Object>) property;
            boolean ignoreInvalidField = configurationProperties.ignoreInvalidFields();
            boolean ignoreUnknownField = configurationProperties.ignoreUnknownFields();
            boolean isFieldPriorityMatch = configurationProperties.givePriorityMatch() == ConfigurationProperties.Type.FIELD;
            char[] toHump = configurationProperties.toHump();

            Map<String, Field> nameFieldMap = new KeyCaseSensitivityMap<>(ClassUtils.getNameFieldMap(beanClass));
            Map<String, Method> nameSetterMethodMap = ClassUtils.getAllSetterMethods(beanClass);

            for (Map.Entry<String, Object> confEntry : envMap.entrySet()) {
                String configKey = confEntry.getKey();
                Object configValue = confEntry.getValue();
                InjectionFactorBuilder injectionFactorBuilder;
                // 优先匹配原则 --> 属性匹配优先
                if(isFieldPriorityMatch){
                    injectionFactorBuilder = findFieldInjectionFactorBuilder(configKey, configValue, toHump, nameFieldMap);
                    injectionFactorBuilder = injectionFactorBuilder == null ? findMethodInjectionFactorBuilder(configKey, configValue, toHump, nameSetterMethodMap) : injectionFactorBuilder;
                }
                // 优先匹配原则 --> 方法匹配优先
                else{
                    injectionFactorBuilder = findMethodInjectionFactorBuilder(configKey, configValue, toHump, nameSetterMethodMap);
                    injectionFactorBuilder = injectionFactorBuilder == null ? findFieldInjectionFactorBuilder(configKey, configValue, toHump, nameFieldMap) : injectionFactorBuilder;
                }


                if(injectionFactorBuilder == null && !ignoreUnknownField){
                    throw new ConfigurationPropertiesInjectionException("An exception occurred while injecting attributes into the '" + beanClass + "' class annotated by the @ConfigurationProperties(prefix='"+prefix+"',ignoreUnknownFields=false) annotation. No property matching the environment variable '"+prefix+"."+configKey+"' was found in the class");
                }

                if (injectionFactorBuilder != null && !injectionFactorBuilder.isLuckyInjectionAnnotationMark()){
                    try {
                        injectionFactorBuilder.setBeanInstance(bean).builder().injection();
                    }catch (Exception e){
                        if(!ignoreInvalidField){
                            throw new ConfigurationPropertiesInjectionException("An exception occurred while injecting an environment variable value into the '" + injectionFactorBuilder.getFactor() + "' attribute of the '" + beanClass + "' class annotated by the @ConfigurationProperties(prefix='"+prefix+"',ignoreInvalidFields=false) annotation!", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取一个基于方法的属性注入因子的构造器
     * @param configKey         配置Key
     * @param configValue       配置Value
     * @param toHump            松散绑定字符，以这些分隔符分隔的单词会被自动转化为驼峰规则后进行匹配尝试
     * @param setterMethodMap   Setter方法名与方法组成的Map
     * @return  基于方法的注入因子构造器
     */
    private InjectionFactorBuilder findMethodInjectionFactorBuilder(String configKey, Object configValue, char[] toHump, Map<String,Method> setterMethodMap){
        Map<String, Method> temp = new LinkedHashMap<>();
        setterMethodMap.forEach((k,v) -> temp.put(k.substring(3), v));

        Map<String,Method> nameMethodMap = new KeyCaseSensitivityMap<>(temp);
        Method method = nameMethodMap.get(configKey);
        if(method == null){
            for (char to : toHump) {
                String toKey = StringUtils.otherFormatsToCamel(configKey, to);
                if(nameMethodMap.containsKey(toKey)){
                    method = nameMethodMap.get(toKey);
                    break;
                }
            }
        }
        return method == null ? null : new InjectionFactorBuilder().setFactor(method).setFactoryValue(configValue);
    }

    /**
     * 获取一个基于属性的属性注入因子的构造器
     * @param configKey         配置Key
     * @param configValue       配置Value
     * @param toHump            松散绑定字符，以这些分隔符分隔的单词会被自动转化为驼峰规则后进行匹配尝试
     * @param nameFieldMap      属性名与属性组成的Map
     * @return 基于属性的注入因子构造器
     */
    private InjectionFactorBuilder findFieldInjectionFactorBuilder(String configKey, Object configValue, char[] toHump, Map<String,Field> nameFieldMap){
        Map<String, Field> fieldMap = new KeyCaseSensitivityMap<>(nameFieldMap);
        Field field = fieldMap.get(configKey);
        if(field == null){
            for (char to : toHump) {
                String toKey = StringUtils.otherFormatsToCamel(configKey,to);
                if(nameFieldMap.containsKey(toKey)){
                    field = nameFieldMap.get(toKey);
                    break;
                }
            }
        }
        return field == null ? null : new InjectionFactorBuilder().setFactor(field).setFactoryValue(configValue);
    }

    /**
     * 注入因子构造器，用于生产一个{@link InjectionFactor}
     */
    static class InjectionFactorBuilder{
        private Object beanInstance;
        private Object factor;
        private Object factoryValue;

        private final Class<?extends Annotation>[] LUCKY_INJECTION_ANNOTATIONS = new Class[]{
                Autowired.class, Qualifier.class, Resource.class
        };

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

        public boolean isLuckyInjectionAnnotationMark(){
            if(factor instanceof Field){
                return AnnotationUtils.isExistOrByArray((Field) factor, LUCKY_INJECTION_ANNOTATIONS);
            }
            Method method = (Method) factor;
            return AnnotationUtils.isExistOrByArray(method, LUCKY_INJECTION_ANNOTATIONS)
                    || AnnotationUtils.isExistOrByArray(method.getParameters()[0], LUCKY_INJECTION_ANNOTATIONS);
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
                setFieldValue(beanInstance,(Field)factor,ConversionUtils.conversion(factoryValue, factoryType));
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
