package com.luckyframework.context;


import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.bean.BeanAnnotationManager;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.BeanPostProcessor;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.context.event.ApplicationEvent;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * 应用程序上下文工具类，为ApplicationContext的所有方法提供静态方法的实现
 */
@DisableProxy
public class ApplicationContextUtils implements ApplicationContextAware {
    
    private static ApplicationContext CONTEXT;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        CONTEXT = applicationContext;
    }

    public static BeanFactory getTargetBeanFactory() {
        return CONTEXT.getTargetBeanFactory();
    }

    public static Class<?> getType(String name) throws BeansException {
        return CONTEXT.getType(name);
    }

    public static ResolvableType getResolvableType(String beanName) {
        return CONTEXT.getResolvableType(beanName);
    }

    public static Object getBean(String name) throws BeansException {
        return CONTEXT.getBean(name);
    }

    public static <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return CONTEXT.getBean(name,requiredType);
    }

    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return CONTEXT.getBean(requiredType);
    }

    public static <T> T getBean(ResolvableType requiredType) throws BeansException {
        return CONTEXT.getBean(requiredType);
    }

    public static Object getBean(String name, Object... args) throws BeansException {
        return CONTEXT.getBean(name,args);
    }

    public static <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return CONTEXT.getBean(requiredType,args);
    }

    public static <T> T getBean(ResolvableType requiredType, Object... args) throws BeansException {
        return CONTEXT.getBean(requiredType,args);
    }

    public static boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return CONTEXT.isTypeMatch(name, typeToMatch);
    }

    public static boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return CONTEXT.isTypeMatch(name, typeToMatch);
    }

    public static boolean containsBean(String name) {
        return CONTEXT.containsBean(name);
    }

    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return CONTEXT.isSingleton(name);
    }

    public static boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return CONTEXT.isPrototype(name);
    }

    public static int getBeanDefinitionCount() {
        return CONTEXT.getBeanDefinitionCount();
    }

    public static String[] getBeanDefinitionNames() {
        return CONTEXT.getBeanDefinitionNames();
    }

    public static String[] getBeanNamesForType(Class<?> type) {
        return CONTEXT.getBeanNamesForType(type);
    }

    public static String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons) {
        return CONTEXT.getBeanNamesForType(type,includeNonSingletons);
    }

    public static String[] getBeanNamesForType(ResolvableType type) {
        return CONTEXT.getBeanNamesForType(type);
    }

    public static String getBeanNameForType(ResolvableType resolvableType) throws BeansException {
        return CONTEXT.getBeanNameForType(resolvableType);
    }

    public static String getBeanNameForType(Class<?> type) {
        return CONTEXT.getBeanNameForType(type);
    }

    public static String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons) {
        return CONTEXT.getBeanNamesForType(type,includeNonSingletons);
    }

    public static String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return CONTEXT.getBeanNamesForAnnotation(annotationType);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return CONTEXT.getBeansOfType(type);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons) throws BeansException {
        return CONTEXT.getBeansOfType(type,includeNonSingletons);
    }

    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        return CONTEXT.getBeansWithAnnotation(annotationType);
    }

    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return CONTEXT.findAnnotationOnBean(beanName,annotationType);
    }

    public static <T> List<T> getSortedBeansOfType(Class<T> type) {
        return CONTEXT.getSortedBeansOfType(type);
    }

    public static <T> List<T> getSortedBeansOfType(Class<T> type, boolean includeNonSingletons) {
        return CONTEXT.getSortedBeansOfType(type,includeNonSingletons);
    }

    public static boolean containsPlugin(String pluginName) {
        return CONTEXT.containsPlugin(pluginName);
    }

    public static AnnotationMetadata[] getPlugins() {
        return CONTEXT.getPlugins();
    }

    public static AnnotationMetadata[] getPluginsFroAnnotation(String annotationClassName) {
        return CONTEXT.getPluginsFroAnnotation(annotationClassName);
    }

    public static AnnotationMetadata[] getPluginsFroAnnotation(Class<? extends Annotation> annotationClass) {
        return CONTEXT.getPluginsFroAnnotation(annotationClass);
    }

    public static Resource[] getResources(String locationPattern) throws IOException {
        return CONTEXT.getResources(locationPattern);
    }

    public static Environment getEnvironment() {
        return CONTEXT.getEnvironment();
    }

    public static Resource getResource(String location) {
        return CONTEXT.getResource(location);
    }

    public static ClassLoader getClassLoader() {
        return CONTEXT.getClassLoader();
    }

    public static void publishEvent(ApplicationEvent event) {
        CONTEXT.publishEvent(event);
    }

    public static void publishEvent(Object event) {
        CONTEXT.publishEvent(event);
    }

    public static BeanDefinition getBeanDefinition(String beanName){
        return CONTEXT.getBeanDefinition(beanName);
    }

    public static <T> T luckyBeanInjection(String beanName,@NonNull T instance){
        propertyInjection(beanName,instance);
        instance = (T) applyPostProcessBeforeInitialization(beanName,null, instance);
        initializeBean(beanName,instance);
        instance = (T) applyPostProcessAfterInitialization(beanName, null, instance);
        return instance;
    }

    public static <T> T luckyBeanInjection(@NonNull T instance){
        return luckyBeanInjection(ScannerUtils.getScannerElementName(instance.getClass()),instance);
    }

    public static void propertyInjection(String beanName,Object instance){
        BeanAnnotationManager beanAnnotationManager = new BeanAnnotationManager(beanName,CONTEXT,CONTEXT.getEnvironment());
        beanAnnotationManager.injectionField(instance);
        beanAnnotationManager.invokeSetMethod(instance);
        invokeAwareMethod(instance);
    }

    public static void initializeBean(String beanName,Object instance){
        BeanAnnotationManager beanAnnotationManager = new BeanAnnotationManager(beanName,CONTEXT,CONTEXT.getEnvironment());
        beanAnnotationManager.initialize(instance);
    }

    public static void destroyBean(String beanName,Object instance){
        BeanAnnotationManager beanAnnotationManager = new BeanAnnotationManager(beanName,CONTEXT,CONTEXT.getEnvironment());
        beanAnnotationManager.destroy(instance);
    }

    public static void invokeAwareMethod(Object instance){
        CONTEXT.invokeAwareMethod(instance);
    }

    public static Object applyPostProcessBeforeInitialization(String beanName, FactoryBean factoryBean,  Object instance){
        List<BeanPostProcessor> beanPostProcessors = CONTEXT.getBeanPostProcessors();
        for (BeanPostProcessor processor : beanPostProcessors) {
            instance = processor.postProcessBeforeInitialization(beanName,factoryBean,instance);
            if(instance == null){
                return null;
            }
        }
        return instance;
    }

    public static Object applyPostProcessAfterInitialization(String beanName,FactoryBean factoryBean, Object instance){
        List<BeanPostProcessor> beanPostProcessors = CONTEXT.getBeanPostProcessors();
        for (BeanPostProcessor processor : beanPostProcessors) {
            instance = processor.postProcessAfterInitialization(beanName, factoryBean,instance);
            if(instance == null){
                return null;
            }
        }
        return instance;
    }

}
