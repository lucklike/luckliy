package com.luckyframework.bean.factory;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.proxy.scope.Scope;
import com.luckyframework.proxy.scope.ScopeRegistry;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的标准的ListableBeanFactory
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/10 下午11:44
 */
public class DefaultStandardListableBeanFactory extends StandardSingletonBeanFactory implements ListableBeanFactory, ScopeRegistry {

    // 注解与名称的映射
    private final Map<Class<? extends Annotation>,String[]> forAnnotationNamesMap = new ConcurrentHashMap<>(16);
    private final Map<String, Scope> scopes = new ConcurrentHashMap<>();

    public DefaultStandardListableBeanFactory(Environment environment) {
        super(environment);
    }

    @Override
    public boolean beanIsCache(String beanName) {
        return hasSingletonObject(beanName);
    }

    @Override
    public void addSingletonBean(String beanName, Object singletonObject) {
        if(!beanIsCache(beanName) && isSingleton(beanName)){
            addSingletonBeanToCache(beanName,singletonObject);
        }
    }

    @Override
    public String getBeanNameForType(ResolvableType resolvableType) throws BeansException {
        String[] names = getBeanNamesForType(resolvableType);
        if(ContainerUtils.isEmptyArray(names)){
            return null;
        }
        if(names.length==1){
            return names[0];
        }
        List<String> primaryNameList = new ArrayList<>();
        for (String name : names) {
            if(getBeanDefinition(name).isPrimary()){
                primaryNameList.add(name);
            }
        }
        if(primaryNameList.size() == 1){
            return primaryNameList.get(0);
        }
        if(ContainerUtils.isEmptyCollection(primaryNameList)){
            throw new BeansException("An exception occurred while accurately locating the bean using the type. Multiple matching beans were found :("+ Arrays.toString(names) +"), but none was [@Primary]");
        }
        throw new BeansException("An exception occurred while using type to locate beans accurately. Multiple matching beans were found :("+  Arrays.toString(names) +"), but there were multiple beans from [@primary] :("+primaryNameList+").");
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons) {
        if(includeNonSingletons){
            return getBeanNamesByType(type);
        }
        List<String> typeNames =new ArrayList<>();
        String[] definitionNames = getBeanDefinitionNames();
        for (String name : definitionNames) {
            if(getBeanDefinition(name).isSingleton()&&isTypeMatch(name,type)){
                typeNames.add(name);
            }
        }
        return typeNames.toArray(new String[]{});
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        String[] forAnnotationNames = forAnnotationNamesMap.get(annotationType);
        if(forAnnotationNames == null){
            List<String> typeNames =new ArrayList<>();
            String[] definitionNames = getBeanDefinitionNames();
            for (String name : definitionNames) {
                if(AnnotationUtils.strengthenIsExist(getType(name),annotationType)){
                    typeNames.add(name);
                }
            }
            forAnnotationNames = typeNames.toArray(new String[0]);
            forAnnotationNamesMap.put(annotationType,forAnnotationNames);
        }
        return forAnnotationNames;
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons) throws BeansException {
        if(includeNonSingletons){
            Map<String,T> matchMap = new HashMap<>();
            String[] definitionNames = getBeanDefinitionNames();
            for (String name : definitionNames) {
                if(isTypeMatch(name,type)){
                    matchMap.put(name,getBean(name,type));
                }
            }
            return matchMap;
        }
        Map<String,T> matchMap = new HashMap<>();
        String[] definitionNames = getBeanDefinitionNames();
        for (String name : definitionNames) {
            if(getBeanDefinition(name).isSingleton() && isTypeMatch(name,type)){
                matchMap.put(name,getBean(name,type));
            }
        }
        return matchMap;
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        Map<String, Object> matchMap = new HashMap<>();
        String[] definitionNames = getBeanDefinitionNames();
        for (String name : definitionNames) {
            if(AnnotationUtils.strengthenIsExist(getType(name),annotationType)){
                matchMap.put(name,getBean(name));
            }
        }
        return matchMap;
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return AnnotatedElementUtils.findMergedAnnotation(getType(beanName),annotationType);
    }

    public void clear(){
        forAnnotationNamesMap.clear();
        super.clear();
    }

    @Override
    public void close() throws IOException {
        super.close();
        forAnnotationNamesMap.clear();
    }

    @Override
    public void registerScope(String scopeName, Scope scope) {
        this.scopes.put(scopeName, scope);
    }

    @Override
    public boolean containsScope(String scopeName) {
        return scopes.containsKey(scopeName);
    }

    @Override
    public Scope getScope(String scopeName) {
        return scopes.get(scopeName);
    }
}
