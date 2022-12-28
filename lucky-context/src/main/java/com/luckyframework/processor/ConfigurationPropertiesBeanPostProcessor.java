package com.luckyframework.processor;


import com.luckyframework.annotations.Autowired;
import com.luckyframework.annotations.ConfigurationProperties;
import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.annotations.Qualifier;
import com.luckyframework.loosebind.FieldInvalidException;
import com.luckyframework.loosebind.FieldUnknownException;
import com.luckyframework.loosebind.LooseBind;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.*;
import com.luckyframework.common.StringUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.ConfigurationPropertiesInjectionException;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 给被{@link ConfigurationProperties @ConfigurationProperties}注解标注的Bean注入属性的BeanPostProcessor
 */
@DisableProxy
public class ConfigurationPropertiesBeanPostProcessor implements BeanPostProcessor, EnvironmentAware {

    private final ConfigurationPropertiesInjectionFactorFilter injectionFactorFilter = new ConfigurationPropertiesInjectionFactorFilter();

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
    @SuppressWarnings("unchecked")
    private void injectConfigProperties(Object bean, ConfigurationProperties configurationProperties){
        Class<?> beanClass = bean.getClass();
        String prefix = configurationProperties.prefix();
        char[] toHump = configurationProperties.toHump();

        Object configObject = getConfigurationProperties(prefix, toHump);
        if(configObject instanceof Map){
            Map<String,Object> configMap = (Map<String, Object>) configObject;

            LooseBind looseBind = new LooseBind(configurationProperties.ignoreInvalidFields(), configurationProperties.ignoreUnknownFields(),
                    configurationProperties.givePriorityMatch() == ConfigurationProperties.Type.FIELD, toHump);
            looseBind.setFactorBuilderFilter(injectionFactorFilter);
            try {
                looseBind.binding(bean, configMap);
            } catch (FieldUnknownException e) {
                throw new ConfigurationPropertiesInjectionException("An exception occurred while injecting attributes into the '" + beanClass + "' class annotated by the @ConfigurationProperties(prefix='"+prefix+"',ignoreUnknownFields=false) annotation. No property matching the environment variable '"+prefix+"."+e.getExKey()+"' was found in the class");
            } catch (FieldInvalidException e) {
                throw new ConfigurationPropertiesInjectionException("An exception occurred while injecting an environment variable value into the '" + e.getFactor() + "' attribute of the '" + beanClass + "' class annotated by the @ConfigurationProperties(prefix='"+prefix+"',ignoreInvalidFields=false) annotation!", e);
            }
        }
    }

    private Object getConfigurationProperties(String prefix, char[] toHump){
        if(environment.containsProperty(prefix)){
           return ((LuckyStandardEnvironment)environment).getPropertyForObject(prefix);
        }
        for (char to : toHump) {
            String toKey = StringUtils.otherFormatsToCamel(prefix, to);
            if(environment.containsProperty(toKey)){
                return ((LuckyStandardEnvironment)environment).getPropertyForObject(toKey);
            }

            toKey = StringUtils.humpToOtherFormats(prefix, String.valueOf(to));
            if(environment.containsProperty(toKey)){
                return ((LuckyStandardEnvironment)environment).getPropertyForObject(toKey);
            }
        }
        return null;
    }

    static class ConfigurationPropertiesInjectionFactorFilter implements LooseBind.InjectionFactorFilter{

        private final Class<? extends Annotation>[] LUCKY_INJECTION_ANNOTATIONS = new Class[]{
                Autowired.class, Qualifier.class, Resource.class
        };

        @Override
        public boolean pass(LooseBind.InjectionFactorBuilder factorBuilder) {
            Object factor = factorBuilder.getFactor();
            if(factor instanceof Field){
                return !AnnotationUtils.isExistOrByArray((Field) factor, LUCKY_INJECTION_ANNOTATIONS);
            }
            Method method = (Method) factor;
            return !(AnnotationUtils.isExistOrByArray(method, LUCKY_INJECTION_ANNOTATIONS)
                    || AnnotationUtils.isExistOrByArray(method.getParameters()[0], LUCKY_INJECTION_ANNOTATIONS));
        }
    }
}
