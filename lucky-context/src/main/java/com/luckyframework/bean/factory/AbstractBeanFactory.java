package com.luckyframework.bean.factory;

import com.luckyframework.definition.AbstractBeanDefinitionRegistry;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.exception.NoUniqueBeanDefinitionException;
import org.springframework.core.ResolvableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 抽象的实现了BeanFactory的基类
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/9 下午11:59
 */
@SuppressWarnings("unchecked")
public abstract class AbstractBeanFactory extends AbstractBeanDefinitionRegistry implements BeanFactory {

    /** bean类型与名称的映射*/
    protected final Map<ResolvableType,String[]> forTypeNamesMap = new ConcurrentHashMap<>(30);


    //-----------------------------------------------------------
    //                   BeanFactory methods
    //-----------------------------------------------------------

    @Override
    public Class<?> getType(String name) throws BeansException {
        return getResolvableType(name).getRawClass();
    }

    @Override
    public ResolvableType getResolvableType(String beanName) {
        return getBeanDefinition(beanName).getResolvableType();
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition beanDefinition = super.getBeanDefinition(beanName);
        FactoryBean factoryBean = beanDefinition.getFactoryBean();
        if(factoryBean instanceof AbstractFactoryBean){
            AbstractFactoryBean abstractFactoryBean = (AbstractFactoryBean) factoryBean;
            if(!abstractFactoryBean.isInvokeAwareMethod()){
                invokeAwareMethod(factoryBean);
                abstractFactoryBean.setInvokeAwareMethod(true);
            }
        }
        return beanDefinition;
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name,ResolvableType.forRawClass(typeToMatch));
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(getResolvableType(name), typeToMatch);
    }

    @Override
    public boolean containsBean(String name) {
        return containsBeanDefinition(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return getBeanDefinition(name).isSingleton();
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return getBeanDefinition(name).isPrototype();
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        if(isTypeMatch(name,requiredType)){
            return (T) getBean(name);
        }
        throw new BeansException("The types are incompatible, and the bean instance corresponding to '"+name+"' cannot be converted to the '"+requiredType+"' type.");
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBean(ResolvableType.forRawClass(requiredType));
    }

    @Override
    public <T> T getBean(ResolvableType requiredType) throws BeansException {
        List<String> beanNamesByType = Stream.of(getBeanNamesByType(requiredType))
                .filter(name -> !BeanFactoryPostProcessor.isTempTargetBeanName(this, name))
                .collect(Collectors.toList());
        if(beanNamesByType.size() == 0){
            return null;
        }
        if(beanNamesByType.size() == 1){
            return (T)getBean(beanNamesByType.get(0));
        }

        //如果匹配到多个bean，则找出其中被@Primary注解标注的
        List<String> primaryBeanNames = new ArrayList<>();
        for (String beanName : beanNamesByType) {
            if(getBeanDefinition(beanName).isPrimary()){
                primaryBeanNames.add(beanName);
            }
        }
        if(primaryBeanNames.size() == 1){
            return (T)getBean(primaryBeanNames.get(0));
        }

        throw new NoUniqueBeanDefinitionException(requiredType, beanNamesByType);
    }

    protected String[] getBeanNamesByType(ResolvableType requiredType){
        String[] forTypeNames = forTypeNamesMap.get(requiredType);
        if(forTypeNames == null){
            List<String> forTypeNameList = new ArrayList<>();
            String[] beanDefinitionNames = getBeanDefinitionNames();
            for (String definitionName : beanDefinitionNames) {
                if(getBeanDefinition(definitionName).isAutowireCandidate()){
                    if(isTypeMatch(definitionName,requiredType)){
                        forTypeNameList.add(definitionName);
                    }
                }
            }
            forTypeNames = forTypeNameList.toArray(EMPTY_STRING_ARRAY);
            forTypeNamesMap.put(requiredType,forTypeNames);
        }
        return forTypeNames;
    }


    protected boolean isTypeMatch(ResolvableType beanResolvableType, ResolvableType typeToMatch){
        return com.luckyframework.reflect.ClassUtils.compatibleOrNot(typeToMatch, beanResolvableType);
    }
}
