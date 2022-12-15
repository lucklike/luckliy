package com.luckyframework.proxy.configuration;

import com.luckyframework.annotations.Configuration;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.proxy.scope.NonSupportAopScopeProxyBeanDefinition;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.ResolvableType;

import static com.luckyframework.definition.BeanDefinition.TARGET_TEMP_BEAN;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 13:02
 */
public class ConfigurationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public final static String CONFIGURATION_TARGET_BEAN_NAME_PREFIX = "configurationTarget.";

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        String[] beanDefinitionNames = listableBeanFactory.getBeanDefinitionNames();
        for (String definitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = listableBeanFactory.getBeanDefinition(definitionName);
            ResolvableType resolvableType = beanDefinition.getResolvableType();
            Class<?> beanType = resolvableType.getRawClass();
            if (isNeedProxyConfiguration(beanType)){
                registerConfigurationDefinition(listableBeanFactory, beanDefinition, definitionName, resolvableType);
            }
        }
    }

    // 重新注册Configuration的Bean定义,其中包含代理Bean定义和真实Bean定义
    private void registerConfigurationDefinition(VersatileBeanFactory listableBeanFactory, BeanDefinition sourceDefinition, String sourceDefinitionName, ResolvableType beanResolvableType){
        String targetDefinitionName = getTargetConfigurationBeanName(sourceDefinitionName);

        BeanDefinition proxyDefinition = sourceDefinition.copy();
        FunctionalFactoryBean factoryBean = () -> TempPair.of(new ConfigurationProxyObjectFactory(beanResolvableType.getRawClass(), listableBeanFactory).getConfigurationProxyObject(), beanResolvableType);
        proxyDefinition.setFactoryBean(factoryBean);
        proxyDefinition.setProxyDefinition(true);
        listableBeanFactory.removeBeanDefinition(sourceDefinitionName);
        if(proxyDefinition.getScope().isNeedProxy()){
            listableBeanFactory.registerBeanDefinition(sourceDefinitionName, new NonSupportAopScopeProxyBeanDefinition(proxyDefinition));
        }else{
            listableBeanFactory.registerBeanDefinition(sourceDefinitionName,proxyDefinition);
        }
        proxyDefinition.setScope(BeanScopePojo.DEF_SINGLETON);
        sourceDefinition.setRole(TARGET_TEMP_BEAN);
        listableBeanFactory.registerBeanDefinition(targetDefinitionName, sourceDefinition);
    }

    private boolean isNeedProxyConfiguration(Class<?> configurationClass){
        Configuration configuration = AnnotationUtils.findMergedAnnotation(configurationClass, Configuration.class);
        return configuration != null && configuration.proxyBeanMethods();
    }

    /**
     * 使用固定前缀为原有的BeanDefinition生成新的名称
     * @param targetBeanName 真实BeanDefinition的名称
     * @return 代理BeanDefinition的bean名称
     */
    public static String getTargetConfigurationBeanName(String targetBeanName){
        return TEMP_BEAN_NAME_PREFIX + CONFIGURATION_TARGET_BEAN_NAME_PREFIX + targetBeanName;
    }
}
