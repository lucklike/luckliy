package com.luckyframework.proxy.scope;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.MethodFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.proxy.configuration.ConfigurationBeanFactoryPostProcessor;
import org.springframework.core.ResolvableType;

import static com.luckyframework.definition.BeanDefinition.TARGET_TEMP_BEAN;
import static com.luckyframework.proxy.scope.RefreshScope.REFRESH_SCOPE_BEAN_NAME;

/**
 * 作用域相关的扩展处理器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 02:16
 */

public class ScopeProxyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public final static String SCOPE_TARGET_BEAN_NAME_PREFIX = "scopedTarget.";

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {

        // 注册Scope接口实例
        registerScope(listableBeanFactory);

        String[] beanDefinitionNames = listableBeanFactory.getBeanDefinitionNames();
        for (String definitionName : beanDefinitionNames) {

            BeanDefinition definition = listableBeanFactory.getBeanDefinition(definitionName);

            if(BeanFactoryPostProcessor.isTempTargetBeanName(listableBeanFactory, definitionName)){
                continue;
            }

            if(!definition.getScope().isNeedProxy() &&
               !(definition instanceof NonSupportAopScopeProxyBeanDefinition)){
                continue;
            }

            // 使用代理BeanDefinition代替原有的BeanDefinition，并将原有的BeanDefinition注册到另一个固定的名称上
            listableBeanFactory.removeBeanDefinition(definitionName);
            registerScopeProxyBeanDefinition(listableBeanFactory, definitionName, definition);
            definition.setRole(TARGET_TEMP_BEAN);
            FactoryBean factoryBean = definition.getFactoryBean();
            if(factoryBean instanceof MethodFactoryBean){
                MethodFactoryBean methodFactoryBean = (MethodFactoryBean) factoryBean;
                String beanName = methodFactoryBean.getBeanName();
                String targetConfigurationBeanName = ConfigurationBeanFactoryPostProcessor.getTargetConfigurationBeanName(beanName);
                if(listableBeanFactory.containsBean(targetConfigurationBeanName)){
                    methodFactoryBean.setBeanName(targetConfigurationBeanName);
                    definition.setDependsOn(new String[]{targetConfigurationBeanName});
                }
            }
            listableBeanFactory.registerBeanDefinition(getScopedTargetBeanName(definitionName), definition);

        }
    }

    private void registerScope(VersatileBeanFactory listableBeanFactory){
        listableBeanFactory.registerScope(BeanScope.REFRESH,listableBeanFactory.getBean(REFRESH_SCOPE_BEAN_NAME, Scope.class));
        listableBeanFactory.registerScope(BeanScope.THREAD_LOCAL,new SimpleThreadScope());
        listableBeanFactory.registerScope(BeanScope.SINGLETON,new DefaultScope());
        listableBeanFactory.registerScope(BeanScope.PROTOTYPE,new DefaultScope());
    }


    /**
     * 注册scope代理对象的BeanDefinition
     * @param listableBeanFactory BeanFactory
     * @param targetBeanName      真实Bean的名称
     * @param targetDefinition    真实Bean的BeanDefinition
     */
    private void registerScopeProxyBeanDefinition(VersatileBeanFactory listableBeanFactory, String targetBeanName, BeanDefinition targetDefinition){

        BeanScopePojo scopePojo = targetDefinition.getScope();
        BeanDefinition scopeProxyDefinition = targetDefinition.copy();
        scopeProxyDefinition.setScope(BeanScopePojo.DEF_SINGLETON);
        scopeProxyDefinition.setProxyDefinition(true);
        scopeProxyDefinition.setFactoryBean(new FactoryBean() {
            @Override
            public Object createBean() {
                Class<?> scopedTargetClass = getResolvableType().getRawClass();
                return scopePojo.isJdkProxy()
                        ? ScopeProxyObjectFactory.createJdkScopeProxy(getScopedTargetBeanName(targetBeanName), scopedTargetClass, listableBeanFactory)
                        : ScopeProxyObjectFactory.createCglibScopeProxy(getScopedTargetBeanName(targetBeanName), scopedTargetClass, listableBeanFactory);
            }

            @Override
            public ResolvableType getResolvableType() {
                return targetDefinition.getFactoryBean().getResolvableType();
            }
        });
        listableBeanFactory.registerBeanDefinition(targetBeanName, scopeProxyDefinition);
    }


    /**
     * 使用固定前缀为原有的BeanDefinition生成新的名称
     * @param targetBeanName 真实BeanDefinition的名称
     * @return 代理BeanDefinition的bean名称
     */
    private String getScopedTargetBeanName(String targetBeanName){
        return TEMP_BEAN_NAME_PREFIX + SCOPE_TARGET_BEAN_NAME_PREFIX + targetBeanName;
    }
}
