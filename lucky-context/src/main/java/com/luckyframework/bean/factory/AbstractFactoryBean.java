package com.luckyframework.bean.factory;

import com.luckyframework.bean.aware.BeanFactoryAware;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.definition.BeanReferenceUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/29 下午10:56
 */
public abstract class AbstractFactoryBean implements FactoryBean, BeanFactoryAware, EnvironmentAware {

    protected Class<?>[] NULL_CLASS = new Class[0];
    protected ResolvableType[] NULL_RESOLVABLE_TYPE = new ResolvableType[0];
    /** 静态方法执行时的参数*/
    protected Object[] parameters;
    protected BeanFactory beanFactory;
    protected Environment environment;
    protected boolean invokeAwareMethod;

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isInvokeAwareMethod() {
        return invokeAwareMethod;
    }

    public void setInvokeAwareMethod(boolean invokeAwareMethod) {
        this.invokeAwareMethod = invokeAwareMethod;
    }

    /**
     * 将输入参数转化为对应的Class数组
     * @param inputParameter 输入参数
     * @return Class数组
     */
    @NonNull
    protected Class<?>[] parameterValueToClasses(Object[] inputParameter){
       if(inputParameter == null){
           return NULL_CLASS;
       }
        Class<?>[] parameterTypes = new Class<?>[inputParameter.length];
        int i = 0;
        for (Object param : inputParameter) {
            if(param instanceof BeanReference){
                BeanReference br = (BeanReference) param;
                if(br.isByName()){
                    parameterTypes[i++] = beanFactory.getType(br.getBeanName());
                }
                // byType和byValue
                else {
                    parameterTypes[i++] = br.getType().getRawClass();
                }
            }else{
                parameterTypes[i++] =  param.getClass();
            }
        }
        return parameterTypes;
    }

    /**
     * 将输入参数转化为对应的ResolvableType数组
     * @param inputParameter 输入参数
     * @return ResolvableType数组
     */
    @NonNull
    protected ResolvableType[] parameterValueToResolvableTypes(Object[] inputParameter){
        if(inputParameter == null){
            return NULL_RESOLVABLE_TYPE;
        }
        ResolvableType[] parameterTypes = new ResolvableType[inputParameter.length];
        int i = 0;
        for (Object param : inputParameter) {
            if(param instanceof BeanReference){
                BeanReference br = (BeanReference) param;
                if(br.isByName()){
                    parameterTypes[i++] = beanFactory.getResolvableType(br.getBeanName());
                }
                // byType和byValue
                else {
                    parameterTypes[i++] = br.getType();
                }
            }else{
                parameterTypes[i++] =  ResolvableType.forRawClass(param.getClass());
            }
        }
        return parameterTypes;
    }

    /**
     * 将输入参数转化为对应的真实值
     * @param inputParameter 输入参数
     * @return 真实值数组
     */
    @NonNull
    protected Object[] getRealParameterValues(Object[] inputParameter){
        return BeanReferenceUtils.getMayBeLazyRealParameterValues(beanFactory, environment, inputParameter);
    }

    public abstract Class<?>[] getParameterClasses();

    public abstract ResolvableType[] getParameterResolvableTypes();

    public abstract Object[] getRealParameterValues();




}
