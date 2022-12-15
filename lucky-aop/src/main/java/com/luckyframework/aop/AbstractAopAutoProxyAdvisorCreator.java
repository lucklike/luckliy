package com.luckyframework.aop;

import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.aop.proxy.AopProxyUtils;
import com.luckyframework.aop.proxy.ProxyFactory;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.NeedEarlyBeanReferenceBeanPostProcessor;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.reflect.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/15 0015 11:57
 */
public abstract class AbstractAopAutoProxyAdvisorCreator implements NeedEarlyBeanReferenceBeanPostProcessor, ApplicationContextAware {

    private final static String ENABLE_CGLIB_PROXY_CONF = "lucky.aop.proxy-target-class";
    private final List<Advisor> advisors = new ArrayList<>(32);
    private ApplicationContext applicationContext;
    protected boolean enableGlobalCglibProxy = false;

    private final Map<String,Object> cacheAopBeanMap = new ConcurrentHashMap<>(225);

    public void registryAdvisor(Advisor advisor) {
        this.advisors.add(advisor);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.enableGlobalCglibProxy = applicationContext.getEnvironment().getProperty(ENABLE_CGLIB_PROXY_CONF,boolean.class,false);
        registryAdvisor();
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, FactoryBean factoryBean, Object beanInstance) {
        Object proxyBean = createProxyObject(beanName,beanInstance);
        if(proxyBean != beanInstance && ClassUtils.isCglibProxy(proxyBean)){
            AopProxyUtils.fieldCopy(proxyBean,beanInstance);
        }
        return proxyBean;
    }

    @Override
    public Object getEarlyBeanReference(String beanName, Object beanInstance) {
        return createProxyObject(beanName,beanInstance);
    }

    // 注册Advisor
    private void registryAdvisor(){

        // 获取并注册Advisor类型的增强
        String[] advisorNames = applicationContext.getBeanNamesForType(Advisor.class);
        for (String advisorName : advisorNames) {
            registryAdvisor(applicationContext.getBean(advisorName,Advisor.class));
        }

        // 获取并注册AdvisorBatchProductionPlant类型的Advisor批量产出工厂
        String[] advisorFactoryBeanNames = applicationContext.getBeanNamesForType(AdvisorBatchProductionPlant.class);
        for (String advisorFactoryBeanName : advisorFactoryBeanNames) {
            AdvisorBatchProductionPlant advisorFactory = applicationContext.getBean(advisorFactoryBeanName, AdvisorBatchProductionPlant.class);
            advisorFactory.getAdvisors().forEach(this::registryAdvisor);
        }

    }

    private Object createProxyObject(String beanName, Object beanInstance){

        boolean isIocBean = applicationContext.containsBean(beanName);
        ProxyMode proxyMode = ProxyFactory.getProxyMode(applicationContext,beanInstance,beanName);
        boolean isCache = isIocBean && applicationContext.getBeanDefinition(beanName).isSingleton();

        //切面不接受代理,bean定义文件中配置为不支持代理的bean
        if((beanInstance instanceof Advisor) || (ProxyMode.NO == proxyMode)){
            return beanInstance;
        }

        Object proxyBean = cacheAopBeanMap.get(beanName);
        if(proxyBean == null){
            Class<?> beanClass = beanInstance.getClass();
            ProxyFactory proxyFactory = getProxyFactory(applicationContext, beanName, beanInstance);
            for (Advisor advisor : advisors) {
                com.luckyframework.aop.pointcut.Pointcut pointcut = advisor.getPointcut();
                if(pointcut.matchClass(beanName,beanClass)){
                    proxyFactory.registryAdvisor(advisor);
                }
            }
            if(ContainerUtils.isEmptyCollection(proxyFactory.getAdvisors())){
                proxyBean = beanInstance;
            }else{
                proxyBean = proxyFactory.getProxy();
            }
            if(isCache && proxyBean!=beanInstance){
                cacheAopBeanMap.put(beanName,proxyBean);
            }
        }
        return proxyBean;
    }

    public abstract ProxyFactory getProxyFactory(ApplicationContext applicationContext, String beanName, Object target);

}
