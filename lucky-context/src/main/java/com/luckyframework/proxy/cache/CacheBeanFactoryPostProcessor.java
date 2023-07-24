package com.luckyframework.proxy.cache;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.cache.Cache;
import com.luckyframework.cache.CacheManager;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.BeanFactoryCglibObjectCreator;
import com.luckyframework.expression.BeanFactoryEvaluationContextFactory;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.proxy.cache.annotations.*;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.proxy.scope.NonSupportAopScopeProxyBeanDefinition;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.ParameterUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ResolvableType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.luckyframework.definition.BeanDefinition.TARGET_TEMP_BEAN;
import static com.luckyframework.proxy.cache.CacheManagerConfiguration.CONCURRENT;
import static com.luckyframework.proxy.cache.CacheManagerConfiguration.DEFAULT_CACHE_MANGER;

/**
 * 缓存Bean工厂处理器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 12:59
 */
public class CacheBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public final CacheOperation DEFAULT_OPERATION = new CacheOperation(CONCURRENT, DEFAULT_CACHE_MANGER, new String[0], "", "", new String[0]);
    public final KeyGenerator DEFAULT_KEY_GENERATOR = new SimpleKeyGenerator();
    public final static String CACHE_TARGET_BEAN_NAME_PREFIX = "cacheTarget.";

    private VersatileBeanFactory versatileBeanFactory;

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        versatileBeanFactory = listableBeanFactory;

        String[] beanDefinitionNames = listableBeanFactory.getBeanDefinitionNames();
        for (String definitionName : beanDefinitionNames) {

            if(BeanFactoryPostProcessor.isTempTargetBeanName(listableBeanFactory, definitionName)){
                continue;
            }

            BeanDefinition beanDefinition = listableBeanFactory.getBeanDefinition(definitionName);
            ResolvableType resolvableType = beanDefinition.getResolvableType();
            Class<?> beanClass = resolvableType.getRawClass();

            if(isCacheClass(beanClass)){
                String cacheTargetBeanName = getCacheTargetBeanName(definitionName);

                BeanDefinition cacheProxyDefinition = beanDefinition.copy();
                FunctionalFactoryBean factoryBean = () -> TempPair.of(new CglibCacheMethodInterceptor(cacheTargetBeanName, beanClass).createProxyObject(), resolvableType);
                cacheProxyDefinition.setFactoryBean(factoryBean);
                cacheProxyDefinition.setScope(BeanScopePojo.DEF_SINGLETON);
                cacheProxyDefinition.setProxyDefinition(true);
                listableBeanFactory.removeBeanDefinition(definitionName);
                listableBeanFactory.registerBeanDefinition(definitionName, new NonSupportAopScopeProxyBeanDefinition(cacheProxyDefinition));

                beanDefinition.setRole(TARGET_TEMP_BEAN);
                listableBeanFactory.registerBeanDefinition(cacheTargetBeanName, beanDefinition);
            }
        }

    }

    /**
     * 是否需要执行缓存代理
     * @param aClass bean的Class
     * @return 是否需要执行缓存代理
     */
    private boolean isCacheClass(Class<?> aClass){
        return AnnotationUtils.isAnnotated(aClass, CacheMetadata.class) || !ClassUtils.getMethodByStrengthenAnnotation(aClass, CacheMetadata.class).isEmpty();
    }

    /**
     * 使用固定前缀为原有的BeanDefinition生成新的名称
     * @param targetBeanName 真实BeanDefinition的名称
     * @return 代理BeanDefinition的bean名称
     */
    private String getCacheTargetBeanName(String targetBeanName){
        return TEMP_BEAN_NAME_PREFIX + CACHE_TARGET_BEAN_NAME_PREFIX + targetBeanName;
    }



    // Cglib方法拦截器
    class CglibCacheMethodInterceptor implements MethodInterceptor{

        /** 真实Bean的名称*/
        private final String targetBeanName;
        /** 真实Bean的类型*/
        private final Class<?> targetClass;

        /** 方法缓存操作缓存，由于提高程序的运行速度*/
        private final Map<Method, CacheOperationChain> operationsMap = new ConcurrentHashMap<>(225);

        public CglibCacheMethodInterceptor(String targetBeanName, Class<?> targetClass) {
            this.targetBeanName = targetBeanName;
            this.targetClass = targetClass;
        }

        /**
         * 获取支持缓存操作的代理实例
         * @return  缓存代理实例
         */
        public Object createProxyObject(){
            return ProxyFactory.getCglibProxyObject(targetClass,new BeanFactoryCglibObjectCreator(targetClass,versatileBeanFactory, versatileBeanFactory.getEnvironment()),this);
        }

        /**
         * 方法增强逻辑
         * @param proxy         代理对象
         * @param method        执行方法
         * @param args          方法参数
         * @param methodProxy   代理方法
         * @return  方法执行结果
         * @throws Throwable 执行出现异常时抛出的异常
         */
        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object targetObject = versatileBeanFactory.getBean(targetBeanName);

            // 判断类或者方法是否被缓存元注解标注，被标注的方法才会走代理逻辑
            if(AnnotationUtils.isAnnotated(targetClass, CacheMetadata.class) || AnnotationUtils.isAnnotated(method, CacheMetadata.class)){

                // 初始化方法执行上下文
                CacheMethodExecuteContext executeContext = new CacheMethodExecuteContext(targetObject, method, args);
                // 获取缓存操作链
                CacheOperationChain cacheOperationChain = getCacheOperations(method);
                // 执行缓存移除操作
                cacheOperationChain.beforeInvocationCacheEvict(executeContext);
                // 执行缓存获取操作
                cacheOperationChain.cacheGet(executeContext, () -> methodProxy.invoke(targetObject, args));
                // 执行缓存移除操作
                cacheOperationChain.afterInvocationCacheEvict(executeContext);
                // 执行缓存添加操作
                cacheOperationChain.cachePut(executeContext);
                return executeContext.getResult();
            }
            return methodProxy.invoke(targetObject, args);
        }

        /**
         * 获取缓存操作
         * @param method 当前执行的缓存方法
         * @return 缓存操作对象
         */
        private CacheOperationChain getCacheOperations(Method method){
            synchronized (operationsMap){
                CacheOperationChain operations = operationsMap.get(method);
                if(operations == null){
                    operations = new CacheOperationChain(targetClass, method);
                    operationsMap.put(method, operations);
                }
                return operations;
            }
        }
    }


    /**
     * 缓存方法执行上下文，封装了执行对象、执行方法、执行参数以及执行结果相关的信息
     */
    static class CacheMethodExecuteContext {
        /** 方法名*/
        private final String methodName;
        /** 方法实例*/
        private final Method method;
        /** 真实对象实例*/
        private final Object target;
        /** 真实对象类型*/
        private final Class<?> targetClass;
        /** 真实对象全类名*/
        private final String targetClassName;
        /** 真实对象简单类名*/
        private final String targetClassSimpleName;
        /** 方法执行参数*/
        private final Object[] args;
        /** 方法执行结果*/
        private Object result;
        /** 结果命中缓存*/
        private Cache<Object, Object> resultHitCache;
        /** 设计到的缓存对象*/
        private Cache<Object, Object>[] caches;

        public CacheMethodExecuteContext(Object target, Method method, Object[] args){
            this.target = target;
            this.targetClass = target.getClass();
            this.targetClassName = targetClass.getName();
            this.targetClassSimpleName = targetClass.getSimpleName();
            this.args = args;
            this.method = method;
            this.methodName = method.getName();
        }

        public String getMethodName() {
            return methodName;
        }

        public Method getMethod() {
            return method;
        }

        public Object getTarget() {
            return target;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        public String getTargetClassSimpleName() {
            return targetClassSimpleName;
        }

        public Object[] getArgs() {
            return args;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public Cache<Object, Object>[] getCaches() {
            return caches;
        }

        public void setCaches(Cache<Object, Object>[] caches) {
            this.caches = caches;
        }

        public Cache<Object, Object> getResultHitCache() {
            return resultHitCache;
        }

        public void setResultHitCache(Cache<Object, Object> resultHitCache) {
            this.resultHitCache = resultHitCache;
        }
    }

    /**
     * 缓存操作对象
     */
    class CacheOperationChain {

        private final List<CacheEvictOperation> evictOperations = new ArrayList<>();
        private final List<CachePutOperation> putOperations = new ArrayList<>();
        private final List<CacheableOperation> cacheableOperations = new ArrayList<>();
        private boolean isSynced = false;

        public CacheOperationChain(Class<?> targetClass, Method method){
            initOperations(targetClass);
            initOperations(method);

            CacheOperation defaultOperation = getDefaultOperation(AnnotationUtils.findMergedAnnotation(targetClass, CacheConfig.class));
            evictOperations.forEach((op) -> operationsDefaultSetting(op, defaultOperation));
            putOperations.forEach((op) -> operationsDefaultSetting(op, defaultOperation));
            cacheableOperations.forEach((op) -> operationsDefaultSetting(op, defaultOperation));

            for (CacheableOperation operation : cacheableOperations) {
                if(operation.isSync()){
                    isSynced = true;
                    break;
                }
            }
        }

        private CacheOperation getDefaultOperation(CacheConfig config){
            if(config == null){
                return DEFAULT_OPERATION;
            }
            return new CacheOperation(
                    StringUtils.getDefaultValueIfNonText(config.cacheType(), CONCURRENT),
                    StringUtils.getDefaultValueIfNonText(config.cacheManager(), DEFAULT_CACHE_MANGER),
                    config.cacheNames(),
                    config.keyGenerator(),
                    "",
                    config.spelImport());
        }

        private void initOperations(AnnotatedElement annotatedElement){
            AnnotationUtils.findAllMergedAnnotations(annotatedElement, CacheEvict.class).forEach((ann) -> evictOperations.add(new CacheEvictOperation(ann)));
            AnnotationUtils.findAllMergedAnnotations(annotatedElement, CachePut.class).forEach((ann) -> putOperations.add(new CachePutOperation(ann)));
            AnnotationUtils.findAllMergedAnnotations(annotatedElement, Cacheable.class).forEach((ann) -> cacheableOperations.add(new CacheableOperation(ann)));

            AnnotationUtils.findAllMergedAnnotations(annotatedElement, CacheEvicts.class).forEach((anns) -> Stream.of(anns.value()).forEach((ann) -> evictOperations.add(new CacheEvictOperation(ann))));
            AnnotationUtils.findAllMergedAnnotations(annotatedElement, CachePuts.class).forEach((anns) -> Stream.of(anns.value()).forEach((ann) -> putOperations.add(new CachePutOperation(ann))));
            AnnotationUtils.findAllMergedAnnotations(annotatedElement, Cacheables.class).forEach((anns) -> Stream.of(anns.value()).forEach((ann) -> cacheableOperations.add(new CacheableOperation(ann))));
        }

        private void operationsDefaultSetting(CacheOperation operation, CacheOperation defaultOperation){
            operation.setCacheManager(StringUtils.getDefaultValueIfNonText(operation.getCacheManager(), defaultOperation.getCacheManager()));
            operation.setCacheType(StringUtils.getDefaultValueIfNonText(operation.getCacheType(), defaultOperation.getCacheType()));
            operation.setKeyGenerator(StringUtils.getDefaultValueIfNonText(operation.getKeyGenerator(), defaultOperation.getKeyGenerator()));
            operation.addCacheNames(defaultOperation.getCacheNames());
            operation.addSpelImport(defaultOperation.getSpelImport());
        }

        public void beforeInvocationCacheEvict(CacheMethodExecuteContext executeContext){
            cacheEvict(evictOperations.stream().filter((eop) -> eop.beforeInvocation).collect(Collectors.toList()), executeContext);
        }

        public void afterInvocationCacheEvict(CacheMethodExecuteContext executeContext){
            cacheEvict(evictOperations.stream().filter((eop) -> !eop.beforeInvocation).collect(Collectors.toList()), executeContext);
        }

        private void cacheEvict(List<CacheOperation> evictOperations, CacheMethodExecuteContext executeContext){
            evictOperations.forEach((op) -> op.operation(executeContext));
        }

        public void cacheGet(CacheMethodExecuteContext executeContext, CacheSupplier cacheSupplier) throws Throwable{
            if(isSynced){
                syncCacheGet(executeContext, cacheSupplier);
            }else{
                doCacheGet(executeContext, cacheSupplier);
            }
        }

        public void doCacheGet(CacheMethodExecuteContext executeContext, CacheSupplier cacheSupplier) throws Throwable {
            Object cacheValue = null;
            for (CacheableOperation cacheableOperation : cacheableOperations) {
                cacheValue = cacheableOperation.getCacheValue(executeContext);
                if(cacheValue != null){
                    break;
                }
            }
            if(cacheValue == null){
                cacheValue = cacheSupplier.queryCache();
            }
            executeContext.setResult(cacheValue);

            for (CacheableOperation cacheableOperation : cacheableOperations) {
                cacheableOperation.operation(executeContext);
            }
        }

        public synchronized void syncCacheGet(CacheMethodExecuteContext executeContext, CacheSupplier cacheSupplier) throws Throwable{
            doCacheGet(executeContext, cacheSupplier);
        }

        public void cachePut(CacheMethodExecuteContext executeContext){
            putOperations.forEach((pop) -> pop.operation(executeContext));
        }

    }

    @SuppressWarnings("all")
    class CacheOperation {
        private final LocalVariableTableParameterNameDiscoverer paramTables = new LocalVariableTableParameterNameDiscoverer();
        private String cacheType;
        private String cacheManager;
        private String keyGenerator;
        private String keyExpression;
        private Set<String> cacheNames = new HashSet<>();
        private List<String> spelImport = new ArrayList<>();

        private SpELRuntime spELRuntime = new SpELRuntime(new BeanFactoryEvaluationContextFactory(versatileBeanFactory));

        public CacheOperation(String cacheType, String cacheManager, String[] cacheNames, String keyGenerator, String keyExpression, String[] spelImport) {
            this.cacheType = cacheType;
            this.cacheManager = cacheManager;
            this.keyGenerator = keyGenerator;
            this.keyExpression = keyExpression;
            this.cacheNames.addAll(Arrays.asList(cacheNames));
            this.spelImport.addAll(Arrays.asList(spelImport));
        }

        public String getCacheType() {
            return cacheType;
        }

        public String getCacheManager() {
            return cacheManager;
        }

        public String[] getCacheNames() {
            return cacheNames.toArray(new String[0]);
        }

        public String getKeyGenerator() {
            return keyGenerator;
        }

        public String getKeyExpression() {
            return keyExpression;
        }

        public void setCacheType(String cacheType) {
            this.cacheType = cacheType;
        }

        public void setCacheManager(String cacheManager) {
            this.cacheManager = cacheManager;
        }

        public void setCacheNames(String... cacheNames) {
            this.cacheNames.clear();;
            this.cacheNames.addAll(Arrays.asList(cacheNames));
        }

        public void addCacheNames(String... cacheNames) {
            this.cacheNames.addAll(Arrays.asList(cacheNames));
        }

        public void setSpelImport(String... spelImport) {
            this.spelImport.clear();
            for (String importPackage : spelImport) {
                if(!this.spelImport.contains(importPackage)){
                    this.spelImport.add(importPackage);
                }
            }
        }

        public void addSpelImport(String... spelImport) {
            for (String importPackage : spelImport) {
                if(!this.spelImport.contains(importPackage)){
                    this.spelImport.add(importPackage);
                }
            }
        }

        public void setKeyGenerator(String keyGenerator) {
            this.keyGenerator = keyGenerator;
        }

        public void setKeyExpression(String keyExpression) {
            this.keyExpression = keyExpression;
        }

        public String[] getSpelImport() {
            return spelImport.toArray(new String[0]);
        }

        public Cache<Object, Object>[] getCacheInstance(){
            CacheManager cacheManager = getCacheManagerInstance();
            if(ContainerUtils.isEmptyCollection(cacheNames)){
                return new Cache[]{cacheManager.ifNotExistsCreated(cacheManager.getDefaultCacheSpace(), cacheType)};
            }
            Cache[] caches = new Cache[cacheNames.size()];
            int index = 0;
            for (String cacheName : cacheNames) {
                caches[index++] = cacheManager.ifNotExistsCreated(cacheName, cacheType);
            }
            return caches;
        }

        public KeyGenerator getKeyGeneratorInstance(){
            return StringUtils.hasText(keyGenerator)
                    ? versatileBeanFactory.getBean(keyGenerator, KeyGenerator.class)
                    : DEFAULT_KEY_GENERATOR;
        }

        public CacheManager getCacheManagerInstance(){
            return versatileBeanFactory.getBean(cacheManager, CacheManager.class);
        }

        public Object getSpELExpressionValue(CacheMethodExecuteContext executeContext, String spELExpression){
            executeContext.setCaches(getCacheInstance());

            Map<String, Object> paramMap = new HashMap<>();
            Method method = executeContext.getMethod();

            if(method.getParameterCount() != 0){

                Object[] args = executeContext.getArgs();
                Parameter[] parameters = method.getParameters();
                String[] parameterNames = paramTables.getParameterNames(method);

                for (int i = 0; i < parameters.length; i++) {
                    paramMap.put(ParameterUtils.getParamName(parameters[i], parameterNames[i]), args[i]);
                    paramMap.put("p"+i, args[i]);
                    paramMap.put("a"+i, args[i]);
                }
            }
            return spELRuntime.getValueForType(new ParamWrapper(spELExpression).importPackage(getSpelImport()).setRootObject(executeContext).setVariables(paramMap));
        }

        public Object getKeyObject(CacheMethodExecuteContext executeContext){
            return StringUtils.hasText(keyExpression)
                    ? getSpELExpressionValue(executeContext, keyExpression)
                    : getKeyGeneratorInstance().generate(executeContext.getTarget(), executeContext.getMethod(), executeContext.args);
        }

        public Object getCacheKey(CacheMethodExecuteContext executeContext){
            return StringUtils.hasText(keyExpression)
                   ? getSpELExpressionValue(executeContext, keyExpression)
                   : getKeyGeneratorInstance().generate(executeContext.getTarget(), executeContext.getMethod(), executeContext.getArgs());
        }

        public Object getCacheValue(CacheMethodExecuteContext executeContext){
            return null;
        }

        public void operation(CacheMethodExecuteContext executeContext){

        }


    }

    class CacheableOperation extends CacheOperation {

        private final String condition;
        private final String unless;
        private final boolean sync;

        public CacheableOperation(Cacheable cacheable){
            super(cacheable.cacheType(), cacheable.cacheManager(), cacheable.cacheNames(), cacheable.keyGenerator(), cacheable.key(), cacheable.spelImport());
            this.condition = cacheable.condition();
            this.unless = cacheable.unless();
            this.sync = cacheable.sync();
        }

        public String getCondition() {
            return condition;
        }

        public String getUnless() {
            return unless;
        }

        public boolean isSync() {
            return sync;
        }

        public boolean getConditionResult(CacheMethodExecuteContext executeContext){
            return !StringUtils.hasText(condition) || (boolean) getSpELExpressionValue(executeContext, condition);
        }

        public boolean getUnlessResult(CacheMethodExecuteContext executeContext){
            return StringUtils.hasText(unless) && (boolean) getSpELExpressionValue(executeContext, unless);
        }

        @Override
        public void operation(CacheMethodExecuteContext executeContext) {
            // 满足条件，执行缓存操作
            if(getConditionResult(executeContext) && !getUnlessResult(executeContext)){
                Object cacheKey = getCacheKey(executeContext);
                Cache<Object, Object>[] caches = getCacheInstance();
                for (Cache<Object, Object> cache : caches) {
                    if(!cache.equals(executeContext.getResultHitCache())){
                        cache.put(cacheKey, executeContext.getResult());
                    }
                }
            }
        }

        @Override
        public Object getCacheValue(CacheMethodExecuteContext executeContext) {
            Object cacheKey = getCacheKey(executeContext);
            Cache<Object, Object>[] caches = getCacheInstance();
            Object cacheValue = null;
            for (Cache<Object, Object> cache : caches) {
                cacheValue = cache.get(cacheKey);
                if(cacheValue != null){
                    executeContext.setResultHitCache(cache);
                    break;
                }
            }
            return cacheValue;
        }
    }

    class CachePutOperation extends CacheOperation {
        private final String condition;
        private final String unless;

        public CachePutOperation(CachePut cachePut) {
            super(cachePut.cacheType(), cachePut.cacheManager(), cachePut.cacheNames(), cachePut.keyGenerator(), cachePut.key(), cachePut.spelImport());
            this.condition = cachePut.condition();
            this.unless = cachePut.unless();
        }

        public String getCondition() {
            return condition;
        }

        public String getUnless() {
            return unless;
        }

        public boolean getConditionResult(CacheMethodExecuteContext executeContext){
            return !StringUtils.hasText(condition) || (boolean) getSpELExpressionValue(executeContext, condition);
        }

        public boolean getUnlessResult(CacheMethodExecuteContext executeContext){
            return StringUtils.hasText(unless) && (boolean) getSpELExpressionValue(executeContext, unless);
        }

        @Override
        public void operation(CacheMethodExecuteContext executeContext) {
            // 满足条件，执行缓存操作
            if(getConditionResult(executeContext) && !getUnlessResult(executeContext)){
                Object cacheKey = getCacheKey(executeContext);
                Cache<Object, Object>[] caches = getCacheInstance();
                for (Cache<Object, Object> cache : caches) {
                    cache.put(cacheKey, executeContext.getResult());
                }
            }
        }
    }

    class CacheEvictOperation extends CacheOperation {

        private final String condition;
        private final boolean allEntries;
        private final boolean beforeInvocation;

        public CacheEvictOperation(CacheEvict cacheEvict) {
            super(null, cacheEvict.cacheManager(), cacheEvict.cacheNames(), cacheEvict.keyGenerator(), cacheEvict.key(), cacheEvict.spelImport());
            this.condition = cacheEvict.condition();
            this.allEntries = cacheEvict.allEntries();
            this.beforeInvocation = cacheEvict.beforeInvocation();
        }

        public String getCondition() {
            return condition;
        }

        public boolean isAllEntries() {
            return allEntries;
        }

        public boolean isBeforeInvocation() {
            return beforeInvocation;
        }

        public boolean getConditionResult(CacheMethodExecuteContext executeContext){
            return !StringUtils.hasText(condition) || (boolean) getSpELExpressionValue(executeContext, condition);
        }

        @Override
        public Cache<Object, Object>[] getCacheInstance() {
            CacheManager cacheManager = getCacheManagerInstance();
            for (String cacheName : getCacheNames()) {
                if(!cacheManager.hasCached(cacheName)){
                    throw new IllegalArgumentException("CacheEvictDefinition: The cache space named '"+cacheName+"' does not exist.");
                }
            }
            return super.getCacheInstance();
        }

        @Override
        public void operation(CacheMethodExecuteContext executeContext) {
           if(getConditionResult(executeContext)){
               Cache<Object, Object>[] caches = getCacheInstance();
               if(isAllEntries()){
                   for (Cache<Object, Object> cache : caches) {
                       cache.clear();
                   }
               } else {
                   Object cacheKey = getCacheKey(executeContext);
                   for (Cache<Object, Object> cache : caches) {
                       cache.remove(cacheKey);
                   }
               }
           }
        }
    }

    interface CacheSupplier {
        Object queryCache() throws Throwable;
    }

}
