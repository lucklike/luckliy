package com.luckyframework.aop.proxy;


import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.annotations.ProxyModel;
import com.luckyframework.aop.advisor.Advisor;
import com.luckyframework.aop.advisor.AdvisorRegistry;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.PropertyValue;
import com.luckyframework.definition.SetterValue;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理工厂，用于生产代理对象
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 14:45
 */
public class ProxyFactory implements AdvisorRegistry {

    /** 使用与当前Bean的切面集合*/
    private final List<Advisor> advisors = new ArrayList<>();
    /** 当前Bean的名称*/
    private final String beanName;
    /** 当前Bean的真实对象*/
    private final Object target;
    /** 应用程序上下文*/
    private final ApplicationContext applicationContext;
    /** 是否开启了全局CGLIB代理配置*/
    private boolean enableGlobalCglibProxy = false;

    /**
     * 生成一个代理工厂实例
     * @param applicationContext 应用程序上下文
     * @param beanName 要代理的bean的名称
     * @param target   要代理的Bean的真实对象
     */
    public ProxyFactory(ApplicationContext applicationContext,String beanName,Object target) {
        this.target = target;
        this.applicationContext = applicationContext;this.beanName = beanName;

    }

    /***
     * 获取当前要代理的Bean的名称
     * @return Bean的名称
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * 设置是否启用全局的CGLIB代理
     * @param enableGlobalCglibProxy 是/否 ？
     */
    public void setEnableGlobalCglibProxy(boolean enableGlobalCglibProxy) {
        this.enableGlobalCglibProxy = enableGlobalCglibProxy;
    }

    /**
     * 注册一个切面
     * @param advisor Advisor 切面
     */
    @Override
    public void registryAdvisor(Advisor advisor) {
        this.advisors.add(advisor);
    }

    /**
     * 获取所有的注册的切面
     * @return 所有注册的切面
     */
    @Override
    public List<Advisor> getAdvisors() {
        return this.advisors;
    }

    /**
     * 获取当前真实对象的代理对象
     * @return 代理对象
     */
    public Object getProxy(){
        return createAopProxy(target,beanName,advisors).getProxy();
    }

    /**
     * 创建一个AOP代理对象生产器
     * @param target 真实对象
     * @param matchAdvisors 被配该真实对象的所有切面
     * @return {@link AopProxy}
     */
    private AopProxy createAopProxy(Object target,String beanName, List<Advisor> matchAdvisors) {
        // 是该用jdk动态代理还是cglib？
        if (shouldUseJDKDynamicProxy(applicationContext,target,beanName)) {
            return new JdkDynamicAopProxy(beanName,target, matchAdvisors);
        } else {
            return new CglibDynamicAopProxy(applicationContext,beanName,target, matchAdvisors,getProxyMode(applicationContext,target,beanName)==ProxyMode.SUPPORT_NESTED);
        }
    }

    /**
     * 是否需要支持嵌套代理
     * @return true/false
     */
    public static ProxyMode getProxyMode(ApplicationContext applicationContext,Object beanObject,String beanName){
        if(applicationContext.containsBeanDefinition(beanName)){
            return applicationContext.getBeanDefinition(beanName).getProxyMode();
        }
        Assert.notNull(beanObject,"bean is null");
        Class<?> beanClass = beanObject.getClass();
        ProxyModel proxyModel = AnnotatedElementUtils.findMergedAnnotation(beanClass, ProxyModel.class);
        return proxyModel == null ? ProxyMode.AUTO : proxyModel.value();
    }


    /**
     * 是否使用JDK自带的动态代理方式成成代理对象
     * 1.如果明确指定了代理方式则使用指定的方式进行代理
     * 2.未明确指定代理方式时会自动推导代理方式，规则如下：
     *    a.如果开启了全局CGLIB代理配置，则使用CGLIB方式代理
     *    b.如果当前被代理的对象已经是一个JDK代理对象，则使用CGLIB方式代理
     *    c.如果当前被代理的对象已经是一个CGLIB代理对象，则使用JDK方式代理
     *    d.当前被代理的对象没有实现任何一个接口，使用CGLIB方式代理
     *    e.明确指定了需要支持嵌套代理时，使用CGLIB方式代理
     *    f.判断是否被其他Bean对象直接依赖，如果有则使用CGLIB方式代理，否则使用JDK方式代理
     * @param bean 被代理的Bean对象
     * @return true/false
     */
    public boolean shouldUseJDKDynamicProxy(ApplicationContext applicationContext,Object bean,String beanName) {
        ProxyMode proxyMode = getProxyMode(applicationContext,bean,beanName);
        // 指定使用JDK代理
        if(proxyMode == ProxyMode.INTERFACES){
            return true;
        }
        // 指定使用Cglib代理
        if(proxyMode == ProxyMode.TARGET_CLASS){
            return false;
        }

        //开启了全局Cglib代理配置
        if(enableGlobalCglibProxy){
            return false;
        }

        Class<?> targetClass = bean.getClass();

        // 没有实现任何接口的对象，使用Cglib代理
        if(ContainerUtils.isEmptyArray(targetClass.getInterfaces())){
            return false;
        }
        // 配置了嵌套代理，使用Cglib代理
        if(proxyMode == ProxyMode.SUPPORT_NESTED){
            return false;
        }
        // 如果被其他Bean直接依赖则使用Cglib代理，否则使用JDK代理
        return !areDirectlyDependentOn(beanName,targetClass);
    }

    /**
     * 判断当前啊Bean是否被其他Bean直接依赖
     * @param beanName   当前Bean的名称
     * @param beanClass  当前Bean的类型
     * @return true/false
     */
    private boolean areDirectlyDependentOn(String beanName,Class<?> beanClass){
        List<BeanDefinition> definitionList = applicationContext.getBeanDefinitions();
        for (BeanDefinition definition : definitionList) {

            PropertyValue[] propertyValues = definition.getPropertyValues();
            if(areDirectlyDependentOnByPropertyValue(beanName,beanClass,propertyValues)){
                return true;
            }
            SetterValue[] setterValues = definition.getSetterValues();
            if(areDirectlyDependentOnBySetterValues(beanClass,setterValues)){
                return true;
            }

            FactoryBean factoryBean = definition.getFactoryBean();
            if(areDirectlyDependentOnByFactoryBean(beanClass,factoryBean)){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前类型是否被其他Bean以属性的方式直接依赖
     * @param beanName       bean的名称
     * @param beanClass      bean的Class
     * @param propertyValues 属性的依赖
     * @return true/false
     */
    private boolean areDirectlyDependentOnByPropertyValue(String beanName,Class<?> beanClass,PropertyValue[] propertyValues){
        if(ContainerUtils.isEmptyArray(propertyValues)){
            return false;
        }
        for (PropertyValue propertyValue : propertyValues) {
            Object propertyValueValue = propertyValue.getValue();
            if(propertyValueValue instanceof BeanReference){
                BeanReference br = (BeanReference) propertyValueValue;
                if (br.isByName() && beanName.equals(br.getBeanName())){
                    return true;
                }
            }
            Class<?> fieldType = propertyValue.getField().getType();
            if(dependentOn(beanClass,fieldType)){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前类型是否被其他Bean以Setter方法的方式直接依赖
     * @param beanClass    待判断的类型
     * @param setterValues Setter方法的依赖
     * @return true/false
     */
    private boolean areDirectlyDependentOnBySetterValues(Class<?> beanClass,SetterValue[] setterValues){
        if(ContainerUtils.isEmptyArray(setterValues)){
            return false;
        }
        for (SetterValue setterValue : setterValues) {
            Class<?>[] parameterTypes = setterValue.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                if(dependentOn(beanClass,parameterType)){
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 判断当前类型是否被其他Bean以FactoryBean的方式直接依赖
     * @param notSureOfType 待判断的类型
     * @param factoryBean FactoryBean实例
     * @return true/false
     */
    private boolean areDirectlyDependentOnByFactoryBean(Class<?> notSureOfType,FactoryBean factoryBean){
        ResolvableType[] dependOnTypes = factoryBean.createDependOnTypes();
        if(ContainerUtils.isEmptyArray(dependOnTypes)){
            return false;
        }
        for (ResolvableType dependOnType : dependOnTypes) {
            if(dependentOn(notSureOfType,dependOnType.resolve())){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个类型是否强一致
     * @param notSureOfType 带判断的类型
     * @param referenceTypes 基准类型
     * @return true/false
     */
    private boolean dependentOn(Class<?> notSureOfType,Class<?> referenceTypes){

        if(referenceTypes.isInterface() || ClassUtils.isSimpleBaseType(referenceTypes)){
            return notSureOfType == referenceTypes;
        }

        if(referenceTypes == notSureOfType){
            return true;
        }

        Class<?> superclass = referenceTypes.getSuperclass();
        if(superclass == null){
            return false;
        }
        if(superclass == Object.class){
            return false;
        }
        return dependentOn(notSureOfType,superclass);
    }
}
