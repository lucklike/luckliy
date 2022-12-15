package com.luckyframework.aop;

import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.definition.BeanDefinition;

import java.util.List;

public class SettingSupportNestedBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        List<BeanDefinition> beanDefinitions = listableBeanFactory.getBeanDefinitions();
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if(beanDefinition.getProxyMode() == ProxyMode.AUTO){
                beanDefinition.setProxyMode(ProxyMode.SUPPORT_NESTED);
            }
        }
    }
}
