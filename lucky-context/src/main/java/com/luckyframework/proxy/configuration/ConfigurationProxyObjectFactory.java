package com.luckyframework.proxy.configuration;

import com.luckyframework.annotations.Bean;
import com.luckyframework.bean.factory.StandardVersatileBeanFactory;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.StringUtils;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;
import com.luckyframework.proxy.CglibObjectCreator;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * configuration代理对象生成器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 12:44
 */
public class ConfigurationProxyObjectFactory {

    private final VersatileBeanFactory beanFactory;
    private final Class<?> configurationClass;

    public ConfigurationProxyObjectFactory(Class<?> configurationClass, VersatileBeanFactory beanFactory) {
        this.configurationClass = configurationClass;
        this.beanFactory = beanFactory;
    }

    public Object getConfigurationProxyObject(){
        ConfigurationCglibMethodInterceptor cglibCallback = new ConfigurationCglibMethodInterceptor();
        CglibObjectCreator cglibObjectCreator = new BeanFactoryCglibObjectCreator(configurationClass, beanFactory, beanFactory.getEnvironment());
        return ProxyFactory.getCglibProxyObject(configurationClass, cglibObjectCreator, cglibCallback);
    }


    class ConfigurationCglibMethodInterceptor implements MethodInterceptor {

        @Override
        public synchronized Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Bean bean = AnnotationUtils.findMergedAnnotation(method, Bean.class);
            if(bean != null){
                String beanName = StringUtils.hasText(bean.name()) ? bean.name() : method.getName();
                return ((StandardVersatileBeanFactory) beanFactory).isSingletonCurrentlyInCreation(beanName)
                        ? methodProxy.invokeSuper(proxy, args)
                        : beanFactory.getBean(beanName);
            }
            return methodProxy.invokeSuper(proxy, args);
        }
    }

}
