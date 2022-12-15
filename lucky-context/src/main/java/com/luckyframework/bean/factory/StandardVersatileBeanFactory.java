package com.luckyframework.bean.factory;

import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.exception.BeanDefinitionRegisterException;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.proxy.scope.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StandardVersatileBeanFactory implements VersatileBeanFactory , AgentBeanFactory {

    protected final DefaultStandardListableBeanFactory workBeanFactory;

    public StandardVersatileBeanFactory(DefaultStandardListableBeanFactory workBeanFactory) {
        this.workBeanFactory = workBeanFactory;
    }


    //----------------------------------------------------------------------------------
    //                              Scope methods
    //----------------------------------------------------------------------------------

    public boolean isSingletonCurrentlyInCreation(String beanName){
        return this.workBeanFactory.isSingletonCurrentlyInCreation(beanName);
    }

    @Override
    public void registerScope(String scopeName, Scope scope) {
        this.workBeanFactory.registerScope(scopeName, scope);
    }

    @Override
    public boolean containsScope(String scopeName) {
        return  this.workBeanFactory.containsScope(scopeName);
    }

    @Override
    public Scope getScope(String scopeName) {
        return  this.workBeanFactory.getScope(scopeName);
    }

    @Override
    public boolean beanIsCache(String beanName) {
        return this.workBeanFactory.beanIsCache(beanName);
    }

    @Override
    public void addSingletonBean(String beanName, Object singletonObject) {
        this.workBeanFactory.addSingletonBean(beanName,singletonObject);
    }

    @Override
    public Class<?> getType(String name) throws BeansException {
        return this.workBeanFactory.getType(name);
    }

    @Override
    public ResolvableType getResolvableType(String beanName) {
        return this.workBeanFactory.getResolvableType(beanName);
    }

    @Override
    public Object getBean(String name) throws BeansException {
        BeanDefinition beanDefinition = getBeanDefinition(name);
        BeanScopePojo scopePojo = beanDefinition.getScope();
        String scopeName = scopePojo.getScope();
        Scope scope = getScope(scopeName);
        if(scope == null){
            throw new IllegalStateException("No Scope registered for scope name '"+scopeName+"'");
        }
        return scope.get(name, () -> this.workBeanFactory.getBean(name));
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return this.workBeanFactory.getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return this.workBeanFactory.getBean(requiredType);
    }

    @Override
    public <T> T getBean(ResolvableType requiredType) throws BeansException {
        return this.workBeanFactory.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return this.workBeanFactory.getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return this.workBeanFactory.getBean(requiredType,args);
    }

    @Override
    public <T> T getBean(ResolvableType requiredType, Object... args) throws BeansException {
        return this.workBeanFactory.getBean(requiredType,args);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return this.workBeanFactory.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return this.workBeanFactory.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean containsBean(String name) {
        return this.workBeanFactory.containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return this.workBeanFactory.isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return this.workBeanFactory.isPrototype(name);
    }

    @Override
    public void invokeAwareMethod(Object instance) {
        this.workBeanFactory.invokeAwareMethod(instance);

    }

    @Override
    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        this.workBeanFactory.registerBeanPostProcessor(processor);
    }

    @Override
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.workBeanFactory.getBeanPostProcessors();
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegisterException {
        this.workBeanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.workBeanFactory.getBeanDefinition(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.workBeanFactory.containsBeanDefinition(beanName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        this.workBeanFactory.removeBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.workBeanFactory.getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return this.workBeanFactory.getBeanDefinitionNames();
    }

    @Override
    public List<BeanDefinition> getBeanDefinitions() {
        return this.workBeanFactory.getBeanDefinitions();
    }

    @Override
    public String getBeanNameForType(ResolvableType resolvableType) throws BeansException {
        return this.workBeanFactory.getBeanNameForType(resolvableType);
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons) {
        return this.workBeanFactory.getBeanNamesForType(type,includeNonSingletons);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return this.workBeanFactory.getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons) throws BeansException {
        return this.workBeanFactory.getBeansOfType(type,includeNonSingletons);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        return this.workBeanFactory.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return this.workBeanFactory.findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    public void registerPlugin(String pluginName, AnnotationMetadata plugin) {
        this.workBeanFactory.registerPlugin(pluginName, plugin);
    }

    @Override
    public void removePlugin(String pluginName) {
        this.workBeanFactory.removePlugin(pluginName);
    }

    @Override
    public boolean containsPlugin(String pluginName) {
        return this.workBeanFactory.containsPlugin(pluginName);
    }

    @Override
    public AnnotationMetadata[] getPlugins() {
        return this.workBeanFactory.getPlugins();
    }

    @Override
    public AnnotationMetadata[] getPluginsFroAnnotation(@NonNull String annotationClassName) {
        return this.workBeanFactory.getPluginsFroAnnotation(annotationClassName);
    }

    @Override
    public Environment getEnvironment() {
        return this.workBeanFactory.getEnvironment();
    }

    @Override
    public String[] prioritizedSingletonBeans() {
        return this.workBeanFactory.prioritizedSingletonBeans();
    }

    @Override
    public void setInvokeAwareMethodConsumer(Consumer<Object> invokeAwareMethodConsumer) {
        this.workBeanFactory.setInvokeAwareMethodConsumer(invokeAwareMethodConsumer);
    }

    @Override
    public void clear() {
        this.workBeanFactory.clear();
    }

    @Override
    public void close() throws IOException {
        this.workBeanFactory.close();
    }

    @Override
    public BeanFactory getTargetBeanFactory() {
        return this.workBeanFactory;
    }
}
