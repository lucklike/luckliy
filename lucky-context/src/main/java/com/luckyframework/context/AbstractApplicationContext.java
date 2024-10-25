package com.luckyframework.context;

import com.luckyframework.annotations.ConditionContext;
import com.luckyframework.annotations.ConditionContextImpl;
import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.bean.aware.ApplicationEventPublisherAware;
import com.luckyframework.bean.aware.Aware;
import com.luckyframework.bean.aware.BeanFactoryAware;
import com.luckyframework.bean.aware.ClassLoaderAware;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.bean.aware.MessageSourceAware;
import com.luckyframework.bean.aware.ResourceLoaderAware;
import com.luckyframework.bean.factory.BeanDefinitionRegistryPostProcessor;
import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.BeanPostProcessor;
import com.luckyframework.bean.factory.DefaultStandardListableBeanFactory;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.context.event.ApplicationEvent;
import com.luckyframework.context.event.ApplicationEventMulticaster;
import com.luckyframework.context.event.ApplicationEventPublisher;
import com.luckyframework.context.event.ApplicationListener;
import com.luckyframework.context.event.ContextClosedEvent;
import com.luckyframework.context.event.ContextRefreshedEvent;
import com.luckyframework.context.event.EventListener;
import com.luckyframework.context.event.EventListenerCreateException;
import com.luckyframework.context.event.EventListenerMethodApplicationListener;
import com.luckyframework.context.event.PayloadApplicationEvent;
import com.luckyframework.context.event.SimpleApplicationEventMulticaster;
import com.luckyframework.context.message.DelegatingMessageSource;
import com.luckyframework.context.message.MessageSource;
import com.luckyframework.context.message.MessageSourceResolvable;
import com.luckyframework.context.message.NoSuchMessageException;
import com.luckyframework.definition.BaseBeanDefinition;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.BeanDefinitionRegistry;
import com.luckyframework.environment.EnvironmentExtension;
import com.luckyframework.environment.EnvironmentFactory;
import com.luckyframework.environment.EnvironmentPostProcessor;
import com.luckyframework.exception.BeanDefinitionRegisterException;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.order.OrderRelated;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.ScanElementClassifier;
import com.luckyframework.scanner.Scanner;
import com.luckyframework.scanner.ScannerUtils;
import com.luckyframework.spi.LuckyFactoryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基本的ApplicationContext，本类中抽象出了ApplicationContext对象的初始化流程<br/>
 * 1.初始化{@link ScanElementClassifier}实例<br/>
 * 2.初始化{@link ApplicationEventMulticaster}实例<br/>
 * 3.初始化{@link ConditionContext}实例<br/>
 * 4.初始化{@link DefaultStandardListableBeanFactory}实例<br/>
 * 5.添加内部组件的{@link BeanDefinition}<br/>
 * 6.初始化{@link BeanDefinitionRegistry},注册所需要{@link BeanDefinition}<br/>
 * 7.收集并执行所有{@link BeanDefinitionRegistryPostProcessor},并执行其{@link BeanDefinitionRegistryPostProcessor#postProcessorBeanDefinitionRegistry}方法
 * 8.收集并执行所有{@link BeanFactoryPostProcessor},并执行其{@link BeanFactoryPostProcessor#postProcessorBeanFactory}方法
 * 9.找到所有的{@link BeanPostProcessor},完成对{@link com.luckyframework.bean.factory.BeanPostProcessorRegistry BeanPostProcessorRegistry}的初始化<br/>
 * 10.初始化所有非懒加载的单例bean的实例<br/>
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/14 下午5:27
 */
public abstract class AbstractApplicationContext implements ApplicationContext{

    private static final Logger logger = LoggerFactory.getLogger(AbstractApplicationContext.class);

    public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";
    public static final String ENVIRONMENT_BEAN_NAME = "environment";
    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    protected ScanElementClassifier scannerClassifier;
    protected VersatileBeanFactory beanFactory;
    protected ConditionContext conditionContext;
    protected ApplicationEventMulticaster eventMulticaster;
    protected MessageSource messageSource;
    protected Environment environment;

    public ScanElementClassifier getScannerClassifier() {
        return scannerClassifier;
    }

    public ConditionContext getConditionContext() {
        return conditionContext;
    }

    public void init(){
        initConditionContext();
        addImportAnnotationScannerElement();
        initEnvironment();
        environmentExtension();
        initBeanFactory();
        setAwareConsumer();
        addInternalComponents();
    }

    public void refresh(){
        beanFactoryClear();
        conditionFilter();
        loadBeanDefinition();
        environmentAwareRunning();
        beanDefinitionRegistryPostProcessor();
        beanFactoryPostProcessor();
        registeredBeanPostProcessor();
        initMessageSource();
        initApplicationEventMulticaster();
        singletonBeanInitialization();
        publishContextRefreshEvent();

    }



    //----------------------------------------------------------------
    //                      init methods
    //----------------------------------------------------------------

    /**
     * 初始化条件上下文
     */
    public void initConditionContext(){
        conditionContext = new ConditionContextImpl(this, EnvironmentFactory.defaultEnvironment(),this,this);
    }

    /**
     * 添加由{@link com.luckyframework.annotations.Import @Import}注解导入的注解元素
     */
    protected abstract void addImportAnnotationScannerElement();

    /**
     * 初始化环境变量
     */
    protected abstract void initEnvironment();

    /**
     * 环境变量扩展
     */
    protected void environmentExtension() {
        List<EnvironmentExtension> extensions = LuckyFactoryLoader.loadFactories(EnvironmentExtension.class, ClassUtils.getDefaultClassLoader());
        for (EnvironmentExtension extension : extensions) {
            extension.extend(environment);
        }
    }

    /**
     * 初始化beanFactory
     */
    protected abstract void initBeanFactory();

    /**
     * 设置Aware接口方法的执行逻辑
     */
    protected void setAwareConsumer() {
        beanFactory.setInvokeAwareMethodConsumer(this::invokeAwareMethod);
    }

    /**
     * 添加内部组件
     */
    protected void addInternalComponents(){
        addInternalComponent(ApplicationContext.class.getName(), this, Ordered.LOWEST_PRECEDENCE - 10, ProxyMode.NO);
        addInternalComponent(ENVIRONMENT_BEAN_NAME, beanFactory.getEnvironment(),Ordered.LOWEST_PRECEDENCE - 11, ProxyMode.NO);
    }



    //----------------------------------------------------------------
    //                      refresh methods
    //----------------------------------------------------------------


    /**
     * 清理beanFactory
     */
    protected void beanFactoryClear() {
        beanFactory.clear();
    }

    /**
     * 对所有扫描元素进行条件过滤
     */
    protected void conditionFilter(){
        scannerClassifier.conditionFilter(conditionContext);
    }

    /***
     * 加载所有bean定义信息
     */
    public abstract void loadBeanDefinition();

    /**
     * 给组件设置环境变量
     */
    protected void environmentAwareRunning() {
        invokeEnvironmentAwareMethod(beanFactory);
    }

    /**
     * 执行{@link BeanDefinitionRegistryPostProcessor#postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry)} 方法
     */
    protected void beanDefinitionRegistryPostProcessor() {
        invokeBeanDefinitionRegistryPostProcessor(beanFactory);
    }

    /**
     * 执行{@link BeanFactoryPostProcessor#postProcessorBeanFactory(VersatileBeanFactory)}方法
     */
    protected void beanFactoryPostProcessor() {
        invokeBeanFactoryPostProcessor(beanFactory);
    }

    /**
     * 发布容器刷新事件
     */
    protected void publishContextRefreshEvent() {
        publishEvent(new ContextRefreshedEvent(this));
    }

    /**
     * 初始化事件多播器
     */
    @SuppressWarnings("all")
    public void initApplicationEventMulticaster() {
        // 1.使用BeanFactory初始化默认的事件多播期
        createApplicationEventMulticaster();
        List<AnnotationMetadata> components = scannerClassifier.getComponents();
        for (AnnotationMetadata component : components) {
            String componentName = ScannerUtils.getScannerElementName(component);

            // 收集显示ApplicationListener组件（实现了ApplicationListener的组件）
            if(beanFactory.isTypeMatch(componentName, ApplicationListener.class)){
                eventMulticaster.addApplicationListenerBean(componentName);
            }

            // 收集隐示ApplicationListener组件 (没有实现ApplicationListener，但是组件内部存在被@EventListener注解标注的方法，这些方法会被转化为EventListenerMethodApplicationListener)
            else{
                Class<?> listenerType = getType(componentName);
                List<Method> eventListenerMethodList = ClassUtils.getMethodByStrengthenAnnotation(listenerType, EventListener.class);
                if(ContainerUtils.isEmptyCollection(eventListenerMethodList)){
                    continue;
                }
                Object listenerBean = beanFactory.getBean(componentName);
                Integer classOrder = OrderRelated.getOrder(listenerBean);
                for (Method eventListenerMethod : eventListenerMethodList) {
                    Integer order = OrderRelated.isOrderMethod(eventListenerMethod)
                            ? OrderRelated.getOrder(eventListenerMethod)
                            : classOrder;
                    EventListener annotation = AnnotatedElementUtils.findMergedAnnotation(eventListenerMethod, EventListener.class);
                    Class<?>[] value = annotation.value();
                    String condition = annotation.condition();
                    ApplicationListener listener = new EventListenerMethodApplicationListener(beanFactory, listenerBean, eventListenerMethod, condition, order);

                    List<ResolvableType> eventTypeList = getEventTypes(eventListenerMethod, value);
                    for (ResolvableType eventType : eventTypeList) {
                        eventMulticaster.addApplicationListener(listener, eventType);
                    }
                }
            }
        }
    }

    private void createApplicationEventMulticaster(){
        if(beanFactory.containsBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)){
            this.eventMulticaster =  beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        }
        else{
            this.eventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        }
    }

    private List<ResolvableType> getEventTypes(Method eventListenerMethod, Class<?>[] annotationClasses) {
        List<ResolvableType> eventTypes = new ArrayList<>();
        Set<Class<?>> annotationClassSet = new HashSet<>(Arrays.asList(annotationClasses));
        int a = annotationClassSet.size();
        int p = eventListenerMethod.getParameterCount();
        if(p != 0 && p != 1){
            throw new EventListenerCreateException("An exception occurred when creating note EventListenerMethodApplicationListener. The target method can only have one parameter or no parameter. The error location:'"+eventListenerMethod+"'");
        }

        if(p ==1){
            // 1个方法参数，0个注解参数 -> 以方法参数为准
            if(a == 0){
                eventTypes.add(getEventType(eventListenerMethod.getGenericParameterTypes()[0]));
            }
            // 1个方法参数，多个注解参数 -> 以注解参数为准，但是要检查参数类型与注解类型的兼容性
            else{
                Class<?> paramClass = eventListenerMethod.getParameters()[0].getType();
                for (Class<?> annotationClass : annotationClassSet) {
                    if(!paramClass.isAssignableFrom(annotationClass)){
                        throw new EventListenerCreateException("An exception occurred when creating EventListenerMethodApplicationListener, and the parameter type '"+paramClass.getName()+"' is incompatible with the annotation type '"+annotationClass.getName()+"'! Error location:'"+eventListenerMethod+"'");
                    }else{
                        eventTypes.add(getEventType(annotationClass));
                    }
                }
            }
        }
        // 0个方法参数，多个注解参数 -> 以注解参数为准
        else{
            for (Class<?> annotationClass : annotationClassSet) {
                eventTypes.add(getEventType(annotationClass));
            }
        }
        return eventTypes;
    }

    private ResolvableType getEventType(Type type){
        ResolvableType eventType = ResolvableType.forType(type);
        Class<?> rawClass = eventType.getRawClass();
        if(ApplicationEvent.class.isAssignableFrom(rawClass)){
            return eventType;
        }
        return ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, eventType);
    }


    /**
     * 收集并执行所有的{@link BeanDefinitionRegistryPostProcessor#postProcessorBeanDefinitionRegistry}方法
     * @param beanFactory bean工厂
     */
    protected void invokeBeanDefinitionRegistryPostProcessor(VersatileBeanFactory beanFactory){
        String[] registryPostProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class);
        List<BeanDefinitionRegistryPostProcessor> registryPostProcessorList = new ArrayList<>(registryPostProcessorNames.length);
        for (String registryPostProcessorName : registryPostProcessorNames) {
            registryPostProcessorList.add(beanFactory.getBean(registryPostProcessorName,BeanDefinitionRegistryPostProcessor.class));
        }

        AnnotationAwareOrderComparator.sort(registryPostProcessorList);

        for (BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor : registryPostProcessorList) {
            invokeAwareMethod(beanDefinitionRegistryPostProcessor);
            beanDefinitionRegistryPostProcessor.postProcessorBeanDefinitionRegistry(beanFactory);
        }

    }

    /**
     * 收集并执行所有的{@link BeanFactoryPostProcessor#postProcessorBeanFactory}方法
     * @param beanFactory bean工厂
     */
    protected void invokeBeanFactoryPostProcessor(VersatileBeanFactory beanFactory){
        String[] beanFactoryPostProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class);
        List<BeanFactoryPostProcessor> beanFactoryPostProcessorList = new ArrayList<>(beanFactoryPostProcessorNames.length);
        for (String beanFactoryPostProcessorName : beanFactoryPostProcessorNames) {
            beanFactoryPostProcessorList.add(beanFactory.getBean(beanFactoryPostProcessorName,BeanFactoryPostProcessor.class));
        }

        AnnotationAwareOrderComparator.sort(beanFactoryPostProcessorList);

        for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorList) {
            invokeAwareMethod(beanFactoryPostProcessor);
            beanFactoryPostProcessor.postProcessorBeanFactory(beanFactory);
        }

    }

    protected void invokeEnvironmentAwareMethod(VersatileBeanFactory beanFactory){
        String[] environmentPostProcessorNames = beanFactory.getBeanNamesForType(EnvironmentPostProcessor.class);
        List<EnvironmentPostProcessor> environmentPostProcessorList = new ArrayList<>(environmentPostProcessorNames.length);
        for (String environmentPostProcessorName : environmentPostProcessorNames) {
            environmentPostProcessorList.add(beanFactory.getBean(environmentPostProcessorName, EnvironmentPostProcessor.class));
        }

        AnnotationAwareOrderComparator.sort(environmentPostProcessorList);
        for (EnvironmentPostProcessor environmentPostProcessor : environmentPostProcessorList) {
            invokeAwareMethod(environmentPostProcessor);
            environmentPostProcessor.postProcessorEnvironment(beanFactory.getEnvironment());
        }
    }

    /**
     * 初始化{@link MessageSource}
     */
    protected void initMessageSource() {
        if(beanFactory.containsBean(MESSAGE_SOURCE_BEAN_NAME)){
            this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Using MessageSource [" + this.messageSource + "]");
            }
        }else{
            this.messageSource = new DelegatingMessageSource();
            addInternalComponent(MESSAGE_SOURCE_BEAN_NAME, this.messageSource, Ordered.LOWEST_PRECEDENCE - 12, ProxyMode.NO);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
            }
        }
    }


    /**
     * 获取并注册容器中所有的BeanPostProcessor,排序后依次注册
     */
    protected void registeredBeanPostProcessor() {
        String[] beanPostProcessorNames = getBeanNamesForType(BeanPostProcessor.class);
        List<BeanPostProcessor> sortList = new ArrayList<>();
        for (String beanPostProcessorName : beanPostProcessorNames) {
            sortList.add((BeanPostProcessor) getBean(beanPostProcessorName));
        }
        AnnotationAwareOrderComparator.sort(sortList);
        sortList.forEach(this::registerBeanPostProcessor);
    }

    /**
     * 按照顺序实例化所有的单例bean
     */
    protected void singletonBeanInitialization() {
        for (String singletonBeanName : beanFactory.prioritizedSingletonBeans()) {
            if(!getBeanDefinition(singletonBeanName).isLazyInit()){
                getBean(singletonBeanName);
            }
        }
    }


    public void addInternalComponent(String beanName, Object internalComponent, int priority, ProxyMode proxyMode){
        Class<?> internalComponentClass = internalComponent.getClass();
        BaseBeanDefinition definition = new BaseBeanDefinition();
        FunctionalFactoryBean factoryBean = () -> TempPair.of(internalComponent,ResolvableType.forRawClass(internalComponentClass));
        definition.setFactoryBean(factoryBean);
        definition.setPriority(priority);
        definition.setProxyMode(proxyMode);
        definition.setRole(BeanDefinition.INTERNAL_USE_BEAN);
        registerBeanDefinition(beanName,definition);
    }

    @Override
    public BeanFactory getTargetBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegisterException {
        this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanFactory.getBeanDefinition(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanFactory.containsBeanDefinition(beanName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        this.beanFactory.removeBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beanFactory.getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return this.beanFactory.getBeanDefinitionNames();
    }

    @Override
    public List<BeanDefinition> getBeanDefinitions() {
        return this.beanFactory.getBeanDefinitions();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons) {
        return this.beanFactory.getBeanNamesForType(type, includeNonSingletons);
    }

    @Override
    public String getBeanNameForType(ResolvableType resolvableType) throws BeansException {
        return this.beanFactory.getBeanNameForType(resolvableType);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        return this.beanFactory.getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons) throws BeansException {
        return this.beanFactory.getBeansOfType(type, includeNonSingletons);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        return this.beanFactory.getBeansWithAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return this.beanFactory.findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    public Class<?> getType(String name) throws BeansException {
        return this.beanFactory.getType(name);
    }

    @Override
    public ResolvableType getResolvableType(String beanName) {
        return this.beanFactory.getResolvableType(beanName);
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return this.beanFactory.getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return this.beanFactory.getBean(name, requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return this.beanFactory.getBean(requiredType);
    }

    @Override
    public <T> T getBean(ResolvableType requiredType) throws BeansException {
        return this.beanFactory.getBean(requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return this.beanFactory.getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return this.beanFactory.getBean(requiredType, args);
    }

    @Override
    public <T> T getBean(ResolvableType requiredType, Object... args) throws BeansException {
        return this.beanFactory.getBean(requiredType, args);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return this.beanFactory.isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return this.beanFactory.isTypeMatch(name,typeToMatch);
    }

    @Override
    public boolean containsBean(String name) {
        return this.beanFactory.containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return this.beanFactory.isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        return this.beanFactory.isPrototype(name);
    }

    @Override
    public void invokeAwareMethod(Object instance) {
        if(instance instanceof Aware){
            if(instance instanceof EnvironmentAware){
                Environment environment = this.environment != null ? this.environment : EnvironmentFactory.defaultEnvironment();
                ((EnvironmentAware)instance).setEnvironment(environment);
            }
            if(instance instanceof BeanFactoryAware){
                ((BeanFactoryAware)instance).setBeanFactory(this.beanFactory);
            }

            if(instance instanceof ApplicationEventPublisherAware){
                ApplicationEventPublisher eventPublisher = this.beanFactory == null ? null : this;
                ((ApplicationEventPublisherAware)instance).setApplicationEventPublisher(eventPublisher);
            }

            if(instance instanceof ApplicationContextAware){
                ApplicationContext context = this.beanFactory == null ? null : this;
                ((ApplicationContextAware)instance).setApplicationContext(context);
            }

            if(instance instanceof ResourceLoaderAware){
                ((ResourceLoaderAware)instance).setResourceLoader(Scanner.PM);
            }

            if(instance instanceof ClassLoaderAware){
                ((ClassLoaderAware)instance).setClassLoader(this.getClassLoader());
            }

            if(instance instanceof MessageSourceAware){
                initMessageSource();
                ((MessageSourceAware)instance).setMessageSource(this.messageSource);
            }
        }
    }

    @Override
    public String[] prioritizedSingletonBeans() {
        return beanFactory.prioritizedSingletonBeans();
    }

    @Override
    public void close() throws IOException {
        publishEvent(new ContextClosedEvent(this));
        this.beanFactory.close();
    }

    @Override
    public Environment getEnvironment() {
        return this.beanFactory == null ? EnvironmentFactory.defaultEnvironment() : this.beanFactory.getEnvironment();
    }

    @Override
    public void registerPlugin(String pluginName, AnnotationMetadata plugin) {
        this.beanFactory.registerPlugin(pluginName, plugin);
    }

    @Override
    public boolean containsPlugin(String pluginName) {
        return this.beanFactory.containsPlugin(pluginName);
    }

    @Override
    public AnnotationMetadata[] getPlugins() {
        return this.beanFactory.getPlugins();
    }

    @Override
    public AnnotationMetadata[] getPluginsFroAnnotation(@NonNull String annotationClassName) {
        return this.beanFactory.getPluginsFroAnnotation(annotationClassName);
    }

    @Override
    public void removePlugin(String pluginName) {
        this.beanFactory.removePlugin(pluginName);
    }

    @Override
    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        this.beanFactory.registerBeanPostProcessor(processor);
    }

    @Override
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanFactory.getBeanPostProcessors();
    }

    @Override
    public void publishEvent(Object event) {
        publishEvent(event, null);
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, null);
    }

    protected void publishEvent(Object event, ResolvableType eventType){
        Assert.notNull(event, "Event must not be null");

        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent) event;
        }
        else {
            applicationEvent = new PayloadApplicationEvent<>(this, event);
            if (eventType == null) {
                eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
            }
        }
        this.eventMulticaster.multicastEvent(applicationEvent, eventType);

    }

    @Override
    public boolean beanIsCache(String beanName) {
        return beanFactory.beanIsCache(beanName);
    }

    @Override
    public void addSingletonBean(String beanName, Object singletonObject) {
        this.beanFactory.addSingletonBean(beanName,singletonObject);
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return this.messageSource.getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return this.messageSource.getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return this.messageSource.getMessage(resolvable, locale);
    }
}
