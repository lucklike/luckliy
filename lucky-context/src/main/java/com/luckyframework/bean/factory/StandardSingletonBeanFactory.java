package com.luckyframework.bean.factory;

import com.luckyframework.bean.aware.Aware;
import com.luckyframework.bean.aware.BeanClassLoaderAware;
import com.luckyframework.bean.aware.BeanFactoryAware;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.BeanReferenceUtils;
import com.luckyframework.definition.PropertyValue;
import com.luckyframework.definition.SetterValue;
import com.luckyframework.exception.*;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *
 * 抽象的标准bean工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/10 下午4:57
 */
@SuppressWarnings("unchecked")
public class StandardSingletonBeanFactory extends AbstractBeanFactory implements BeanPostProcessorRegistry{

    private final static Logger log = LoggerFactory.getLogger(StandardSingletonBeanFactory.class);

    /** 单例池*/
    private final Map<String,Object> singletonObjects = new ConcurrentHashMap<>(256);
    /** 实例化但未初始化的早期对象*/
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    /** 单例对象工厂*/
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
    /** Names of beans that are currently in creation. */
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    /** Set of registered singletons, containing the bean names in registration order. */
    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);
    /** 所有的bean后置处理器*/
    private final List<BeanPostProcessor> beanPostProcessors =new ArrayList<>(20);
    /** 环境变量*/
    private final Environment environment;
    /** 所有单例bean的名称*/
    private List<String> singletonBeanNames;
    /** invokeAwareMethod消费者，封装外部传入的invokeAwareMethod逻辑*/
    private Consumer<Object> invokeAwareMethodConsumer;
    /** 依赖创建记录表*/
    private final Set<String> dependencyCreateRecord = new LinkedHashSet<>(32);


    public StandardSingletonBeanFactory(Environment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setInvokeAwareMethodConsumer(Consumer<Object> invokeAwareMethodConsumer) {
        this.invokeAwareMethodConsumer = invokeAwareMethodConsumer;
    }

    public boolean hasSingletonObject(String beanName){
        return singletonObjects.containsKey(beanName);
    }

    void addSingletonBeanToCache(String beanName,Object singletonBean){
        synchronized (singletonObjects){
            singletonObjects.put(beanName,singletonBean);
        }
    }

    /**
     * 获取按照优先级排序后的所有单例bean的名称
     */
    public String[] prioritizedSingletonBeans(){
        String[] singletonBeanNames = getSingletonBeanNames();
        List<TempPair<Integer,String>> sortList = new ArrayList<>();
        for (String singletonBeanName : singletonBeanNames) {
            sortList.add(TempPair.of(getBeanDefinition(singletonBeanName).getPriority(),singletonBeanName));
        }
        return sortList.stream().sorted(Comparator.comparing(TempPair::getOne))
                .map(TempPair::getTwo).distinct().toArray(String[]::new);
    }

    /**
     * 获取所有单例bean的名称
     * @return 所有单例bean的名称
     */
    public String[] getSingletonBeanNames(){
        if(singletonBeanNames == null){
            singletonBeanNames = new ArrayList<>(225);
            for (String definitionName : getBeanDefinitionNames()) {
                BeanDefinition definition = getBeanDefinition(definitionName);
                if(definition.isSingleton() ){
                    singletonBeanNames.add(definitionName);
                }
            }
        }
        return singletonBeanNames.toArray(EMPTY_STRING_ARRAY);
    }

    protected Object getSingletonObject(String beanName){
        return singletonObjects.get(beanName);
    }

    public Object doGetSingletonObject(String beanName){
        Object singletonObject = singletonObjects.get(beanName);
        if(singletonObject == null){
            singletonObject = earlySingletonObjects.get(beanName);
            if(singletonObject == null){
                ObjectFactory<?> objectFactory = singletonFactories.get(beanName);
                if(objectFactory != null){
                    singletonObject = objectFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
        return singletonObject;
    }

    public Object doGetBean(String beanName,boolean notResultReturnNull) {
        synchronized (singletonObjects) {
            BeanDefinition definition = getBeanDefinition(beanName);
            Assert.notNull(definition,"No bean definition information named '"+beanName+"' was found in the container.");
            if(!dependencyCreateRecord.contains(beanName)){
                createDependencyBeans(beanName,definition);
            }
            Object singletonObject = doGetSingletonObject(beanName);
            if(singletonObject == null){
                singletonObject = notResultReturnNull ? null : doCreateBean(beanName, definition);
            }
            return singletonObject == NULL_BEAN ? null : singletonObject;
        }
    }

    public void createDependencyBeans(String beanName,BeanDefinition definition){
        dependencyCreateRecord.add(beanName);
        String[] dependsOn = definition.getDependsOn();
        if (dependsOn.length != 0 && log.isDebugEnabled()) {
            log.debug("'{}' dependency on {}", beanName, Arrays.toString(dependsOn));
        }
        for (String depend : dependsOn) {
            doGetBean(depend, false);
        }
    }

    protected Object doCreateBean(String beanName, BeanDefinition definition){
        Object instance = createBeanInstance(beanName,definition);
        populateBean(beanName,instance);
        instance = initializeBean(beanName,definition,instance);
        return instance;
    }

    private Object initializeBean(String beanName, BeanDefinition definition, Object beanInstance){
        FactoryBean factoryBean = definition.getFactoryBean();
        beanInstance = applyPostProcessBeforeInitialization(beanName, factoryBean, beanInstance);
        doInit(beanName,beanInstance);
        beanInstance = applyPostProcessAfterInitialization(beanName, factoryBean, beanInstance);
        singletonsCurrentlyInCreation.remove(beanName);
        if(definition.isSingleton()){
            addSingletonObject(beanName, beanInstance);
        }
        return beanInstance;
    }

    // 实例化
    protected Object createBeanInstance(String beanName, BeanDefinition definition){
        Object beanInstance =  doGetBean(beanName,true);
        if(beanInstance == null){
            Assert.notNull(definition,"can not find the definition of bean '" + beanName +"'");
            //检查循环依赖
            if(isSingletonCurrentlyInCreation(beanName)){
                throw new BeanCurrentlyInCreationException("Error creating bean with name '"+beanName+"': Requested bean is currently in creation: Is there an unresolvable circular reference? '"+beanName+"' ↔ "+singletonsCurrentlyInCreation);
            }
            if(log.isDebugEnabled()){
                log.debug("Creating bean [{}] '{}'  type is '[{}]'", definition.getScope(), beanName, definition.getFactoryBean().getResolvableType());
            }
            addCurrentlyInCreationBeanName(beanName);
            try {
                beanInstance = doCreateBeanInstance(beanName, definition);
                if(definition.isSingleton()){
                    final Object sourceInstance = beanInstance;
                    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, definition, sourceInstance));
                }
            }catch (Exception e){
                throw new BeanCreationException("An exception occurred while creating a bean named '"+beanName+"'", e);
            }
        }
        return beanInstance;
    }

    protected Object doCreateBeanInstance(String beanName, BeanDefinition definition){
        FactoryBean factoryBean = definition.getFactoryBean();
        return getNotNullInstance(factoryBean.createBean());
    }

    protected Object getEarlyBeanReference(String beanName, BeanDefinition definition, Object beanInstance){
        Object exposedObject = beanInstance;
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            if(beanPostProcessor instanceof NeedEarlyBeanReferenceBeanPostProcessor){
                exposedObject = ((NeedEarlyBeanReferenceBeanPostProcessor)beanPostProcessor).getEarlyBeanReference(beanName,beanInstance);
            }
        }
        return exposedObject;
    }

    // 注入属性
    protected void populateBean(String beanName,Object instance){
        injectionField(beanName,instance);
        invokeSetMethod(beanName,instance);
        invokeAwareMethod(instance);
    }

    // 注入属性
    protected void injectionField(String beanName,Object instance){
        if(!ClassUtils.isJDKProxy(instance)){
            BeanDefinition definition = getBeanDefinition(beanName);
            PropertyValue[] propertyValues = definition.getPropertyValues();
            // 没有属性依赖直接返回
            if(ContainerUtils.isEmptyArray(propertyValues)){
                return;
            }
            Class<?> beanClass = instance.getClass();
            for (PropertyValue ref : propertyValues) {
                try {
                    Field field = ref.getField(beanClass);
                    Object refValue = ref.getValue();
                    Object[] refObjArray = new Object[1];
                    refObjArray[0] = refValue;
                    Object fieldRealValue = BeanReferenceUtils.getMayBeLazyRealParameterValues(this, environment, refObjArray)[0];
                    if(Modifier.isStatic(field.getModifiers())){
                        FieldUtils.setValue(beanClass,field,fieldRealValue);
                    }else{
                        FieldUtils.setValue(instance,field,fieldRealValue);
                    }
                }catch (Exception e){
                    throw new BeanCreationException("An exception occurred when injecting a value into the '"+ref.getName()+"' attribute of the bean named '"+beanName+"'",e);
                }
            }
        }
    }

    // 执行Setter方法
    protected void invokeSetMethod(String beanName, Object instance) {
        BeanDefinition definition = getBeanDefinition(beanName);
        SetterValue[] setterValues = definition.getSetterValues();
        if(ContainerUtils.isEmptyArray(setterValues)){
            return;
        }
        Class<?> beanClass = instance.getClass();
        for (SetterValue setterValue : setterValues) {
            try {
                Object[] parameterRefValues = setterValue.getParameterValues();
                Method setterMethod = setterValue.getMethod(beanClass);
                Object[] parameterRealValues = BeanReferenceUtils.getMayBeLazyRealParameterValues(this, environment, parameterRefValues);
                if(Modifier.isStatic(setterMethod.getModifiers())){
                    MethodUtils.invoke(beanClass,setterMethod,parameterRealValues);
                }else{
                    MethodUtils.invoke(instance,setterMethod,parameterRealValues);
                }
            }catch (Exception e){
                throw new BeanCreationException("Exception while injecting properties to bean '"+beanName+"' using setter method '"+setterValue.getMethodName()+"'!",e);
            }

        }
    }

    // 初始化,执行init方法和InitializingBean的afterPropertiesSet方法
    protected void doInit(String beanName, Object instance) {
        BeanDefinition beanDefinition = getBeanDefinition(beanName);
        if(beanDefinition.isProxyDefinition()) return;
        if(instance instanceof InitializingBean){
            try {
                ((InitializingBean)instance).afterPropertiesSet();
            } catch (Exception e) {
                throw new BeanCreationException("An exception occurred when using the 'InitializingBean#afterPropertiesSet()' method to initialize the bean named '"+beanName+"'",e);
            }
        }

        String[] initMethodNames = beanDefinition.getInitMethodNames();
        for (String initMethodName : initMethodNames) {
            try {
                MethodUtils.invoke(instance,initMethodName,new Object[0]);
            }catch (LuckyReflectionException e){
                throw new BeanCreationException("An exception occurs when the bean named '"+beanName+"' is initialized using the initialization method '"+initMethodName+"()' the beanDefinition.  ["+beanDefinition+"]",e);
            }

        }
    }

    // 销毁方法
    protected void doDestroy(){
        String[] prioritizedSingletonBeans = prioritizedSingletonBeans();
        for(int i = prioritizedSingletonBeans.length-1; i>=0 ;i--){
            String singletonBeanName = prioritizedSingletonBeans[i];
            BeanDefinition beanDefinition = getBeanDefinition(singletonBeanName);
            if(beanDefinition.isProxyDefinition()) break;
            Object bean = getSingletonObject(singletonBeanName);
            if(bean == null){
                continue;
            }
            if(bean instanceof DisposableBean){
                try {
                    ((DisposableBean)bean).destroy();
                } catch (Exception e) {
                    throw new BeanDisposableException("An exception occurred when using the 'DisposableBean#destroy()' destruction method of the bean named '"+singletonBeanName+"'.",e);
                }
            }
            if((bean instanceof Closeable) && !(bean instanceof BeanFactory)){
                try {
                    ((Closeable)bean).close();
                }catch (Exception e){
                    throw new BeanDisposableException("An exception occurred when using the 'Closeable#close()' destruction method of the bean named '"+singletonBeanName+"'.",e);
                }
            }

            String[] destroyMethodNames = getBeanDefinition(singletonBeanName).getDestroyMethodNames();
            for (String destroyMethodName : destroyMethodNames) {
                try {
                    MethodUtils.invoke(bean,destroyMethodName,new Object[0]);
                }catch (LuckyReflectionException e){
                    throw new BeanDisposableException("An exception occurred when using the destroy method '"+destroyMethodName+"()' in the bean definition. bean: '"+singletonBeanName+"'",e);
                }
            }
        }
    }

    // 添加单例到单例池
    public void addSingletonObject(String beanName,Object singletonObject){
        synchronized (singletonObjects){
            singletonObject = getNotNullInstance(singletonObject);
            this.singletonObjects.put(beanName,singletonObject);
            this.earlySingletonObjects.remove(beanName);
            this.singletonFactories.remove(beanName);
            this.registeredSingletons.add(beanName);
            this.singletonsCurrentlyInCreation.remove(beanName);
        }
    }

    // 添加一个单实例对象工厂
    public void addSingletonFactory(String beanName,ObjectFactory<?> singletonFactory){
        Assert.notNull(singletonFactory,"Singleton factory must not be null");
        synchronized (singletonObjects){
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
                this.registeredSingletons.add(beanName);
            }
        }
    }

    public boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }

    public void addCurrentlyInCreationBeanName(String beanName){
        this.singletonsCurrentlyInCreation.add(beanName);
    }

    public boolean removeCurrentlyInCreationBeanName(String beanName){
        return this.singletonsCurrentlyInCreation.remove(beanName);
    }


    private Object getNotNullInstance(Object instance){
        return instance == null ? NULL_BEAN : instance;
    }

    //---------------------------------------------------------------------
    //                      BeanFactory Methods
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        return this.doGetBean(name,false);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        BeanDefinition definition = getBeanDefinition(name);
        if(definition == null){
            throw new NoSuchBeanDefinitionException("No definition information found for bean name '"+name+"'");
        }
        BeanDefinition copy = definition.copy();
        AbstractFactoryBean copyFactoryBean = (AbstractFactoryBean) copy.getFactoryBean();
        Object[] parameters = copyFactoryBean.getParameters();
        if(parameters.length != args.length){
            throw new BeansException("An exception occurred when creating the bean using the constructor because the number of parameters you provided did not match the number of parameters required by the constructor.Expected :"+parameters.length+" Actual: "+args.length+"");
        }
        copyFactoryBean.setParameters(args);
        copy.setFactoryBean(copyFactoryBean);
        return doCreateBean(name,copy);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        return getBean(ResolvableType.forRawClass(requiredType),args);
    }

    @Override
    public <T> T getBean(ResolvableType requiredType, Object... args) throws BeansException {
        List<String> matchNames = new ArrayList<>();
        List<String> equalsNames = new ArrayList<>();
        String[] names = getBeanNamesByType(requiredType);
        for (String name : names) {
            ResolvableType beanResolvableType = getResolvableType(name);
            if(beanResolvableType.hasGenerics()){
                if(beanResolvableType.toString().equals(requiredType.toString())){
                    equalsNames.add(name);
                }
            }else{
                if(requiredType.getRawClass().isAssignableFrom(beanResolvableType.getRawClass())){
                    matchNames.add(name);
                }
            }
        }
        if(equalsNames.size() == 1){
            return (T) getBean(equalsNames.get(0),args);
        }

        if(matchNames.size() == 1){
            return (T) getBean(matchNames.get(0),args);
        }
        if(equalsNames.size()==0 && matchNames.size()==0){
            throw new NoSuchBeanDefinitionException(requiredType);
        }
        throw new NoUniqueBeanDefinitionException(requiredType,matchNames);
    }

    @Override
    public void invokeAwareMethod(Object instance) {
        if(invokeAwareMethodConsumer == null){
            if(instance instanceof Aware){
                if(instance instanceof BeanFactoryAware){
                    ((BeanFactoryAware)instance).setBeanFactory(this);
                }
                if(instance instanceof EnvironmentAware){
                    ((EnvironmentAware)instance).setEnvironment(environment);
                }
                if(instance instanceof BeanClassLoaderAware){
                    ((BeanClassLoaderAware)instance).setBeanClassLoader(instance.getClass().getClassLoader());
                }
            }
        }else{
            invokeAwareMethodConsumer.accept(instance);
        }

    }


    //---------------------------------------------------------------------
    //            BeanPostProcessorRegistry Methods
    //---------------------------------------------------------------------

    @Override
    public void registerBeanPostProcessor(BeanPostProcessor processor) {
        this.beanPostProcessors.add(processor);
    }

    @Override
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return beanPostProcessors;
    }

    private Object applyPostProcessBeforeInitialization(String beanName,FactoryBean factoryBean,Object bean){
        for (BeanPostProcessor processor : beanPostProcessors) {
            bean = processor.postProcessBeforeInitialization(beanName, factoryBean, bean);
            if(bean == null){
                return null;
            }
        }
        return bean;
    }

    protected Object applyPostProcessAfterInitialization(String beanName,FactoryBean factoryBean,Object bean){
        for (BeanPostProcessor processor : beanPostProcessors) {
            bean = processor.postProcessAfterInitialization(beanName, factoryBean, bean);
            if(bean == null){
                return null;
            }
        }
        return bean;
    }

    protected void clear(){
        earlySingletonObjects.clear();
        singletonObjects.clear();
        beanPostProcessors.clear();
        forTypeNamesMap.clear();
    }

    @Override
    public void close() throws IOException {
        doDestroy();
        clear();
    }

}
