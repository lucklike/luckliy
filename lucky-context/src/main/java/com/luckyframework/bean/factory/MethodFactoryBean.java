package com.luckyframework.bean.factory;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 工厂方法工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/1 下午9:59
 */
public class MethodFactoryBean extends AbstractFactoryBean {

    /** bean的Name*/
    private String beanName;
    /** 方法名*/
    private String methodName;
    /** 方法*/
    private Method factoryMethod;
    /** bean实例*/
    private Object bean;

    /**
     * 方法工厂构造器
     * @param beanName   beanName
     * @param methodName 工厂方法名称
     * @param parameters 工厂方法执行时的参数
     */
    public MethodFactoryBean(@NonNull String beanName, @NonNull String methodName, Object[] parameters) {
        this.beanName = beanName;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    /**
     * 方法工厂构造器
     * @param bean       bean实例
     * @param methodName 工厂方法名称
     * @param parameters 工厂方法执行时的参数
     */
    public MethodFactoryBean(@NonNull Object bean,@NonNull String methodName,Object[] parameters){
        this.bean = bean;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    /**
     * 方法工厂构造器
     * @param beanName    beanName
     * @param method      工厂方法实例
     * @param parameters  工厂方法执行时的参数
     */
    public MethodFactoryBean(@NonNull String beanName,@NonNull Method method,Object[] parameters) {
        this.beanName = beanName;
        this.factoryMethod = method;
        this.parameters = parameters;

    }

    /**
     * 方法工厂构造器
     * @param bean          bean实例
     * @param method        工厂方法实例
     * @param parameters    工厂方法执行时的参数
     */
    public MethodFactoryBean(@NonNull Object bean,@NonNull Method method,Object[] parameters){
        this.bean = bean;
        this.factoryMethod = method;
        this.parameters = parameters;
    }

    /**
     * 获取工厂Bean名称
     * @return 工厂Bean名称
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * 设置工厂Bean名称
     * @param beanName 工厂Bean名称
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * 获取工厂方法名称
     * @return 工厂方法名称
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * 设置工厂方法名称
     * @param methodName 工厂方法名称
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 获取工厂方法实例
     * @return 工厂方法实例
     */
    public Method getFactoryMethod() {
        return factoryMethod;
    }

    /**
     * 设置工厂方法实例
     * @param factoryMethod 工厂方法实例
     */
    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    /**
     * 获取工厂Bean实例
     * @return 工厂Bean实例
     */
    public Object getBean() {
        return bean;
    }

    /**
     * 设置工厂Bean实例
     * @param bean 工厂Bean实例
     */
    public void setBean(Object bean) {
        this.bean = bean;
    }

    @Override
    public Class<?>[] getParameterClasses(){
        return parameterValueToClasses(parameters);
    }

    @Override
    public ResolvableType[] getParameterResolvableTypes(){
        return parameterValueToResolvableTypes(parameters);
    }

    @Override
    public Object[] getRealParameterValues() {
        return getRealParameterValues(parameters);
    }

    @Override
    public Object createBean() {
        return MethodUtils.invoke(findBean(),findMethod(),getRealParameterValues());
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forMethodReturnType(findMethod(), beanFactory.getType(beanName));
    }

    @Override
    public ResolvableType[] createDependOnTypes() {
        Type[] genericParameterTypes = findMethod().getGenericParameterTypes();
        if(ContainerUtils.isEmptyArray(genericParameterTypes)){
            return super.createDependOnTypes();
        }
        ResolvableType[] resolvableTypes = new ResolvableType[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            resolvableTypes[i] = ResolvableType.forType(genericParameterTypes[i]);
        }
        return resolvableTypes;
    }

    public Method findMethod(){
        if(factoryMethod == null){
            factoryMethod = ClassUtils.findMethod(beanFactory.getType(beanName),methodName,getParameterResolvableTypes());
        }
        return factoryMethod;
    }

    public Object findBean(){
        if(bean == null){
            bean = beanFactory.getBean(beanName);
        }
        return bean;
    }
}
