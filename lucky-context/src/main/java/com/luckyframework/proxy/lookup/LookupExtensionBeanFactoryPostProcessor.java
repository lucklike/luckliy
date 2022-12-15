package com.luckyframework.proxy.lookup;

import com.luckyframework.annotations.Lookup;
import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.BeanDefinitionBuilder;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;
import com.luckyframework.exception.LookupMethodCheckException;
import com.luckyframework.exception.LookupProxyObjectCreateException;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Lookup注解扩展实现类
 * 本类主要是实现针对{@link Lookup @Lookup}注解提供了两种类型的实现
 * 当组件方法上存在@Lookup注解时，并且该方法为无参且有返回值时，Lucky会自动为该组件
 * 生成一个代理对象，被代理的Lookup方法会被重写，用于返回容器中的某个Bean，当未配置
 * Lookup注解的value属性时，会默认返回与方法的返回值类型相匹配的Bean，如果配置了value
 * 属性，则会以配置的值为ID去查找并且返回Bean。<p/>
 * 当组件分别为抽象类型[抽象类/接口]和具体类型时，Lucky会有两种不同的代理对象生成策略
 * <p>
 * 1.当组件是抽象类型时，Lucky会为抽象组件生成一个能创建相应代理对象的BeanDefinition
 * 并把这些BeanDefinition注册到容器中。如果组件是接口则使用JDK方式代理，如果是抽象类
 * 则会通过Cglib方式来生成代理<p/>
 * <p>
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 06:25
 */
public class LookupExtensionBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private VersatileBeanFactory versatileBeanFactory;

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {

        this.versatileBeanFactory = listableBeanFactory;

        // 对Bean对象执行Lookup注解代理操作
        String[] beanDefinitionNames = listableBeanFactory.getBeanDefinitionNames();
        for (String definitionName : beanDefinitionNames) {
            BeanDefinition definition = listableBeanFactory.getBeanDefinition(definitionName);
            ResolvableType resolvableType = definition.getResolvableType();
            Class<?> beanClass = resolvableType.getRawClass();
            if(isNeedLookupProxy(beanClass)){
                ProxyObjectCreator pluginCreat = new LookupCglibProxyObjectCreator(beanClass);
                FunctionalFactoryBean factoryBean = () -> TempPair.of(pluginCreat.createProxyObject(), resolvableType);
                definition.setFactoryBean(factoryBean);
            }
        }


        // 对插件执行Lookup注解代理操作
        AnnotationMetadata[] plugins = listableBeanFactory.getPlugins();
        for (AnnotationMetadata plugin : plugins) {
            if(isNeedLookupProxy(plugin)){
                String beanName = ScannerUtils.getScannerElementName(plugin);
                try {
                    listableBeanFactory.registerBeanDefinition(beanName,createLookupBeanDefinition(plugin));
                } catch (ClassNotFoundException e) {
                    throw new LookupProxyObjectCreateException(e,"Failed to create Lookup proxy object for class "+plugin.getClassName()+".");
                }
            }
        }


    }

    /**
     * 为需要进行Lookup代理的抽象组件生成{@link BeanDefinition}对象,这些{@link BeanDefinition}
     * 的ProxyMode属性均为{@link ProxyMode#NO}
     * @param annotationMetadata 注解元素
     * @return 能生成代理对象的{@{@link BeanDefinition}}
     * @throws ClassNotFoundException
     */
    private BeanDefinition createLookupBeanDefinition(AnnotationMetadata annotationMetadata) throws ClassNotFoundException {
        final Class<?> pluginClass = ClassUtils.forName(annotationMetadata.getClassName(),ClassUtils.getDefaultClassLoader());
        ProxyObjectCreator pluginCreat = pluginClass.isInterface()
                ? new LookupJdkProxyObjectCreator(pluginClass)
                : new LookupCglibProxyObjectCreator(pluginClass);
        FunctionalFactoryBean factoryBean = () -> TempPair.of(pluginCreat.createProxyObject(), ResolvableType.forRawClass(pluginClass));
        return BeanDefinitionBuilder.builder(factoryBean,annotationMetadata);
    }

    /**
     * 判断注解元素是否需要Lookup代理(是否有方法被@Lookup注解标注)
     * @param beanClass Bean的Class
     * @return 是否需要代理
     */
    private boolean isNeedLookupProxy(Class<?> beanClass){
        List<Method> lookupMethods = ClassUtils.getMethodByStrengthenAnnotation(beanClass, Lookup.class);
        return !ContainerUtils.isEmptyCollection(lookupMethods);
    }

    /**
     * 判断注解元素是否需要Lookup代理(是否有方法被@Lookup注解标注)
     * @param annotationMetadata 注解元素
     * @return 是否需要代理
     */
    private boolean isNeedLookupProxy(AnnotationMetadata annotationMetadata){
        Set<MethodMetadata> annotatedMethods = annotationMetadata.getAnnotatedMethods(Lookup.class.getName());
        return !ContainerUtils.isEmptyCollection(annotatedMethods);
    }

    /**
     * 代理对象创建者接口，用于生成一个代理对象
     * @see LookupCglibProxyObjectCreator
     * @see LookupJdkProxyObjectCreator
     */
    interface ProxyObjectCreator{
        /** 创建一个代理对象*/
        Object createProxyObject();
    }

    /**
     * Lookup方法校验，必须是无参且有返回值的
     * @param method 带校验的方法
     */
    private void checkLookupMethod(Method method){
        if(method.getParameterCount() != 0 || method.getReturnType() == void.class){
            throw new LookupMethodCheckException("The Lookup method "+method+" fails to verify, the Lookup method must be parameterless, and the return value of the method must be non-void.");
        }
    }

    /**
     * 返回容器中的Lookup对象
     * @param lookupMethod Lookup方法
     * @return 容器对象
     */
    private Object returnLookupObject(Method lookupMethod){
        checkLookupMethod(lookupMethod);
        Lookup lookup = AnnotationUtils.findMergedAnnotation(lookupMethod, Lookup.class);
        String beanName = lookup.value();
        return StringUtils.hasText(beanName)
                ? versatileBeanFactory.getBean(beanName)
                : versatileBeanFactory.getBean(lookupMethod.getReturnType());
    }

    /**
     * 使用Cglib方式的代理对象创建者实现
     */
     class LookupCglibProxyObjectCreator implements MethodInterceptor, ProxyObjectCreator{
        private final Class<?> pluginClass;

        LookupCglibProxyObjectCreator(Class<?> pluginClass) {
            this.pluginClass = pluginClass;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            return AnnotationUtils.isAnnotated(method, Lookup.class)
                    ? returnLookupObject(method)
                    : methodProxy.invokeSuper(proxy,args);
        }

        @Override
        public Object createProxyObject(){
            return ProxyFactory.getCglibProxyObject(pluginClass,new BeanFactoryCglibObjectCreator(pluginClass,versatileBeanFactory, versatileBeanFactory.getEnvironment()),this);
        }
    }

    /**
     * 使用JDK方式的代理对象创建者实现
     */
     class LookupJdkProxyObjectCreator implements InvocationHandler,ProxyObjectCreator{

        private final Class<?> pluginClass;

        LookupJdkProxyObjectCreator(Class<?> pluginClass) {
            this.pluginClass = pluginClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return AnnotationUtils.isAnnotated(method, Lookup.class)
                    ? returnLookupObject(method)
                    : null;
        }


        @Override
        public Object createProxyObject() {
            Class<?>[] interfacesClasses = new Class[1];
            interfacesClasses[0] = pluginClass;
            return ProxyFactory.getJdkProxyObject(pluginClass.getClassLoader(),interfacesClasses,this);
        }
    }
}
