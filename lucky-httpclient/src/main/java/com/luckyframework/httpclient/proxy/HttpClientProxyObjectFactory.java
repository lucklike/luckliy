package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.generalapi.describe.DescribeFunction;
import com.luckyframework.httpclient.proxy.annotations.ConvertProhibition;
import com.luckyframework.httpclient.proxy.annotations.DomainNameMeta;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.SSLMeta;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.creator.AbstractObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Generate;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.creator.ReflectObjectCreator;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.exeception.AsyncExecutorNotFountException;
import com.luckyframework.httpclient.proxy.exeception.RequestConstructionException;
import com.luckyframework.httpclient.proxy.handle.DefaultHttpExceptionHandle;
import com.luckyframework.httpclient.proxy.handle.ExceptionHandleCreateException;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformerChain;
import com.luckyframework.httpclient.proxy.logging.LoggerHandler;
import com.luckyframework.httpclient.proxy.logging.NotRecordLog;
import com.luckyframework.httpclient.proxy.mock.MockContext;
import com.luckyframework.httpclient.proxy.mock.MockMeta;
import com.luckyframework.httpclient.proxy.mock.MockResponseFactory;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.Plugin;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;
import com.luckyframework.httpclient.proxy.retry.RetryActuator;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.MutableMapParamWrapper;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.StaticMethodEntry;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.ssl.HostnameVerifierBuilder;
import com.luckyframework.httpclient.proxy.ssl.SSLAnnotationContext;
import com.luckyframework.httpclient.proxy.ssl.SSLSocketFactoryBuilder;
import com.luckyframework.httpclient.proxy.url.AnnotationRequest;
import com.luckyframework.httpclient.proxy.url.DomainNameContext;
import com.luckyframework.httpclient.proxy.url.DomainNameGetter;
import com.luckyframework.httpclient.proxy.url.HttpRequestContext;
import com.luckyframework.httpclient.proxy.url.URLGetter;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_EXE_TIME_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$IS_MOCK$__;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$MOCK_RESPONSE_FACTORY$__;


/**
 * Http客户端代理对象生成工厂<br/>
 * <p>
 * 初始化时就会在SpEL运行时环境中导入{@link CommonFunctions}类、{@link DescribeFunction}<br/>
 * 其中的内置函数可以在SpEL表达式中直接使用<br/><br/>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:57
 */
public class HttpClientProxyObjectFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpClientProxyObjectFactory.class);

    /**
     * 不需要自动关闭的资源类型
     */
    private static final Set<Type> notAutoCloseResourceTypes = new HashSet<Type>() {{
        add(InputStream.class);
        add(InputStreamSource.class);
        add(MultipartFile.class);
    }};

    /**
     * JDK代理对象缓存
     */
    private final Map<Class<?>, ProxyObjectMetaWrap> jdkProxyObjectCache = new ConcurrentHashMap<>(16);

    /**
     * Cglib代理对象缓存
     */
    private final Map<Class<?>, ProxyObjectMetaWrap> cglibProxyObjectCache = new ConcurrentHashMap<>(16);

    /**
     * 全局SpEL变量
     */
    private final SpELVariate globalSpELVar = new SpELVariate();

    /**
     * 对象创建器
     */
    private AbstractObjectCreator objectCreator = new ReflectObjectCreator();

    /**
     * SpEL转换器
     */
    private SpELConvert spELConverter = new SpELConvert();

    /**
     * 通用连接超时时间
     */
    private Integer connectionTimeout;

    /**
     * 通用读超时时间
     */
    private Integer readTimeout;

    /**
     * 通用写超时时间
     */
    private Integer writeTimeout;

    /**
     * 通用域名认证器
     */
    private HostnameVerifier hostnameVerifier;

    /**
     * 通用SSLSocketFactory
     */
    private SSLSocketFactory sslSocketFactory;

    /**
     * 公共请求头参数
     */
    private final Map<String, Object> headers = new ConcurrentHashMap<>();

    /**
     * 公共路径请求参数
     */
    private final Map<String, Object> pathParams = new ConcurrentHashMap<>();

    /**
     * 公共URL请求参数
     */
    private final Map<String, Object> queryParams = new ConcurrentHashMap<>();

    /**
     * 拦截器执行器集合
     */
    private final List<InterceptorPerformer> interceptorPerformerList = new ArrayList<>();

    /**
     * 拦截器执行器工厂集合
     */
    private final List<Generate<InterceptorPerformer>> performerGenerateList = new ArrayList<>();

    /**
     * 备选的用与执行异步Http任务的线程池懒加载对象集合
     */
    private final Map<String, LazyValue<Executor>> alternativeAsyncExecutorMap = new ConcurrentHashMap<>();

    /**
     * {@link KeyStoreInfo}缓存
     */
    private final Map<String, KeyStoreInfo> keyStoreInfoMap = new ConcurrentHashMap<>();

    /**
     * 用于执行异步Http任务的线程池懒加载对象
     */
    private LazyValue<Executor> lazyAsyncExecutor = LazyValue.of(() -> {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("http-task-");
        executor.setConcurrencyLimit(getDefaultExecutorConcurrency());
        return executor;
    });

    /**
     * 默认执行器的并发数
     */
    private int defaultExecutorConcurrency = -1;

    /**
     * Http请求异步模型，默认使用Java线程模型
     */
    private Model asyncModel = Model.JAVA_THREAD;

    /**
     * Http请求执行器
     */
    private HttpExecutor httpExecutor = new JdkHttpExecutor();

    /**
     * 异常处理器
     */
    private HttpExceptionHandle exceptionHandle = new DefaultHttpExceptionHandle();

    /**
     * 异常处理器生成器
     */
    private Generate<HttpExceptionHandle> exceptionHandleGenerate;

    /**
     * 响应转换器
     */
    private ResponseConvert responseConvert;

    /**
     * 响应转换器生成器
     */
    private Generate<ResponseConvert> responseConvertGenerate;

    /**
     * 日志处理器
     */
    private LoggerHandler loggerHandler;

    /**
     * 全局生效的插件
     */
    private List<ProxyPlugin> plugins = new ArrayList<>();

    public static Set<Type> getNotAutoCloseResourceTypes() {
        return notAutoCloseResourceTypes;
    }

    public static void addNotAutoCloseResourceTypes(Class<?> clazz) {
        notAutoCloseResourceTypes.add(clazz);
    }

    //------------------------------------------------------------------------------------------------
    //                                construction methods
    //------------------------------------------------------------------------------------------------


    public HttpClientProxyObjectFactory(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
        importCommonFunction();
    }

    public HttpClientProxyObjectFactory() {
        importCommonFunction();
    }

    private void importCommonFunction() {
        addSpringElFunctionClass(CommonFunctions.class);
        addSpringElFunctionClass(DescribeFunction.class);
    }

    //------------------------------------------------------------------------------------------------
    //                                      Spring El Runtime
    //------------------------------------------------------------------------------------------------

    /**
     * 获取SpEL转换器{@link SpELConvert}
     *
     * @return SpEL转换器
     */
    public SpELConvert getSpELConverter() {
        return this.spELConverter;
    }

    /**
     * 设置SpEL转换器{@link SpELConvert}
     *
     * @param spELConverter SpEL转换器
     */
    public void setSpELConverter(SpELConvert spELConverter) {
        this.spELConverter = spELConverter;
    }

    /**
     * 向SpEL运行时环境新增一个Root变量<br/>
     * {@code 使用方式： #{name}}
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void addSpringElRootVariable(String name, Object value) {
        this.globalSpELVar.addRootVariable(name, value);
    }

    /**
     * 移除SpEL运行时环境中的一组Root变量
     *
     * @param names 要移除的变量名集合
     */
    public void removeSpringElRootVariables(String... names) {
        for (String name : names) {
            this.globalSpELVar.removeRootVariable(name);
        }
    }

    /**
     * 向SpEL运行时环境新增一组Root变量<br/>
     * {@code 使用方式： #{name}}
     *
     * @param confMap 变量名和变量值所组成的Map
     */
    public void addSpringElRootVariables(Map<String, Object> confMap) {
        this.globalSpELVar.addRootVariables(confMap);
    }

    /**
     * 向SpEL运行时环境中新增一个函数
     *
     * @param name   函数名
     * @param method 函数方法
     */
    public void addSpringElFunction(String name, Method method) {
        addSpringElVariable(name, method);
    }

    /**
     * 向SpEL运行时环境中新增一个函数
     *
     * @param method 函数方法
     */
    public void addSpringElFunction(Method method) {
        addSpringElVariable(FunctionAlias.MethodNameUtils.getMethodName(method), method);
    }

    /**
     * 向SpEL运行时环境中新增一个函数
     *
     * @param staticMethodEntry 函数方法实体
     */
    public void addSpringElFunction(StaticMethodEntry staticMethodEntry) {
        Method method = staticMethodEntry.getMethodInstance();
        addSpringElVariable(staticMethodEntry.getName(method), method);
    }

    /**
     * 向SpEL运行时环境中新增一个函数
     *
     * @param alias      函数别名
     * @param clazz      方法所在的Class
     * @param methodName 方法名
     * @param paramTypes 参数列表参数类型数组
     */
    public void addSpringElFunction(String alias, Class<?> clazz, String methodName, Class<?>... paramTypes) {
        addSpringElFunction(StaticMethodEntry.create(alias, clazz, methodName, paramTypes));
    }

    /**
     * 向SpEL运行时环境中新增一个函数
     *
     * @param clazz      方法所在的Class
     * @param methodName 方法名
     * @param paramTypes 参数列表参数类型数组
     */
    public void addSpringElFunction(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        addSpringElFunction(StaticMethodEntry.create(clazz, methodName, paramTypes));
    }

    /**
     * 向SpEL运行时环境中新增一个函数集合，Class中的变量不会被加载
     *
     * @param classStaticElement 静态方法Class实体
     */
    public void addSpringElFunctionClass(ClassStaticElement classStaticElement) {
        addSpringElVariables(classStaticElement.getAllStaticMethods());
    }

    /**
     * 解析SpEL表达式
     *
     * @param expression SpELL表达式
     * @param clazz      目标类型
     * @param <T>        目标类型泛型
     * @return 解析结果
     */
    public <T> T parseExpression(String expression, Class<T> clazz) {
        MutableMapParamWrapper finalVar = new MutableMapParamWrapper();
        finalVar.coverMerge(getGlobalSpELVar());
        finalVar.setExpression(expression).setExpectedResultType(clazz);
        return getSpELConverter().parseExpression(finalVar);
    }

    /**
     * 向SpEL运行时环境中新增一个函数集合，Class中的变量不会被加载
     * <pre>
     *     1.类中不可以有同名的静态方法，如果存在同名的方法请使用{@link FunctionAlias @FunctionAlias}来取别名
     *     2.被{@link FunctionFilter @FunctionFilter}注解标注的方法将会被过滤掉
     *     3.可以使用<b>namespace</b>参数来指定方法前缀，如果传入的参数为空或空字符，则会检测
     *     类上使用有标注{@link Namespace @Namespace}注解，如果有则会使用注解中得前缀
     *
     * 在SpEL运行时环境使用函数的方式为：
     * {@code
     *  使用方式为：
     *  #${namespace}_${methodname}(...args)
     *
     *  // 以导入一个Utils类类举例说明
     *  public class Utils {
     *
     *      @FunctionAlias("add")
     *      public static int sum(int a, int b) {
     *          return a + b;
     *      }
     *
     *      public static int sub(int a, int b) {
     *          return a - b;
     *      }
     *
     *  }
     *
     *  // 导入
     *  addSpringElFunctionClass("util", Utils.class);
     *
     *  // 使用导入的函数
     *  @Get("http://localhost:8080/num?sum=#{#util_add(base, 1)}&sub=#{#util_sub(base, 2)}")
     *  String httpRequest(int base);
     *
     *  // 使用 -> 对应的URL为 http://localhost:8080/num?sum=6&sub=3
     *  httpRequest(5)
     * }
     * </pre>
     *
     * @param namespace     命名空间
     * @param functionClass 方法所在的Class
     */
    public void addSpringElFunctionClass(String namespace, Class<?> functionClass) {
        addSpringElFunctionClass(ClassStaticElement.create(namespace, functionClass));
        this.getGlobalSpELVar().addHook(namespace, functionClass);
    }

    /**
     * 向SpEL运行时环境中新增一个函数集合，Class中的变量不会被加载
     * <pre>
     *     1.类中不可以有同名的静态方法，如果存在同名的方法请使用{@link FunctionAlias @FunctionAlias}来取别名
     *     2.被{@link FunctionFilter @FunctionFilter}注解标注的方法将会被过滤掉
     *     3.可以在类上使用{@link Namespace @Namespace}注解来为该类中给所有方法名上拼接一个固定前缀
     * </pre>
     *
     * @param functionClass 方法所在的Class
     */
    public void addSpringElFunctionClass(Class<?> functionClass) {
        addSpringElFunctionClass(ClassStaticElement.create(functionClass));
        this.getGlobalSpELVar().addHook(functionClass);
    }

    /**
     * 向SpEL运行时环境中新增一个普通变量<br/>
     * {@code 使用方式： #{#name}}
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void addSpringElVariable(String name, Object value) {
        this.globalSpELVar.addVariable(name, value);
    }

    /**
     * 移除SpEL运行时环境中的一组普通变量
     *
     * @param names 要移除的变量名集合
     */
    public void removeSpringElVariables(String... names) {
        for (String name : names) {
            this.globalSpELVar.removeVariable(name);
        }
    }

    /**
     * 向SpEL运行时环境新增一组普通变量<br/>
     * {@code 使用方式： #{#name}}
     *
     * @param confMap 变量名和变量值所组成的Map
     */
    public void addSpringElVariables(Map<String, Object> confMap) {
        this.globalSpELVar.addVariables(confMap);
    }

    /**
     * 向SpEL运行时环境中导入一些公共包
     *
     * @param packageNames 包名集合
     */
    public void importPackage(String... packageNames) {
        this.globalSpELVar.addPackages(packageNames);
    }

    /**
     * 向SpEL运行时环境中导入一些公共包，参数列表中的类所在的包会被导入
     *
     * @param classes Class集合
     */
    public void importPackage(Class<?>... classes) {
        this.globalSpELVar.addPackagesByClasses(classes);
    }

    /**
     * 获取全局SpEL运行时参数
     *
     * @return 全局SpEL运行时参数
     */
    public SpELVariate getGlobalSpELVar() {
        return globalSpELVar;
    }

    /**
     * 获取默认执行器的最大并发数
     *
     * @return 默认执行器的最大并发数
     */
    public int getDefaultExecutorConcurrency() {
        return defaultExecutorConcurrency;
    }

    /**
     * 设置默认执行器的最大并发数
     *
     * @param defaultExecutorConcurrency 默认执行器的最大并发数
     */
    public void setDefaultExecutorConcurrency(int defaultExecutorConcurrency) {
        this.defaultExecutorConcurrency = defaultExecutorConcurrency;
    }

    /**
     * 获取用于执行异步HTTP任务的默认{@link Executor}
     *
     * @return 用于执行异步HTTP任务的默认Executor
     */
    public Executor getAsyncExecutor() {
        return lazyAsyncExecutor.getValue();
    }

    /**
     * 设置用于执行异步HTTP任务的默认{@link Executor}
     *
     * @param asyncExecutor 用于执行异步HTTP任务的默认{@link Executor}
     */
    public void setAsyncExecutor(Executor asyncExecutor) {
        this.lazyAsyncExecutor = LazyValue.of(asyncExecutor);
    }

    /**
     * 设置用于执行异步HTTP任务的默认{@link Supplier Supplier&lt;Executor&gt;}
     *
     * @param asyncExecutorSupplier 用于执行异步HTTP任务的默认{@link Supplier Supplier&lt;Executor&gt;}
     */
    public void setAsyncExecutor(Supplier<Executor> asyncExecutorSupplier) {
        this.lazyAsyncExecutor = LazyValue.of(asyncExecutorSupplier);
    }

    /**
     * 获取Http异步模型
     *
     * @return 异步模型
     */
    public Model getAsyncModel() {
        return this.asyncModel;
    }

    /**
     * 设置Http异步模型
     *
     * @param asyncModel 异步模型
     */
    public void setAsyncModel(Model asyncModel) {
        this.asyncModel = asyncModel == Model.USE_COMMON ? Model.JAVA_THREAD : asyncModel;
    }

    /**
     * 添加一个备选的用于执行异步HTTP任务的默认{@link Executor}
     *
     * @param poolName            名称
     * @param alternativeExecutor 用于执行异步HTTP任务的默认{@link Executor}
     */
    public void addAlternativeAsyncExecutor(String poolName, Executor alternativeExecutor) {
        this.alternativeAsyncExecutorMap.put(poolName, LazyValue.of(alternativeExecutor));
    }

    /**
     * 添加一个备选的用于执行异步HTTP任务的默认{@link Supplier Supplier&lt;Executor&gt;}
     *
     * @param poolName                    名称
     * @param alternativeExecutorSupplier 用于执行异步HTTP任务的默认{@link Supplier Supplier&lt;Executor&gt;}
     */
    public void addAlternativeAsyncExecutor(String poolName, Supplier<Executor> alternativeExecutorSupplier) {
        this.alternativeAsyncExecutorMap.put(poolName, LazyValue.of(alternativeExecutorSupplier));
    }

    /**
     * 根据线程池名称获取一个备用的线程池LazyValue对象
     *
     * @param poolName 线程池名称
     * @return 备用线程池的LazyValue对象
     */
    public LazyValue<Executor> getAlternativeAsyncExecutor(String poolName) {
        if (alternativeAsyncExecutorMap.containsKey(poolName)) {
            return alternativeAsyncExecutorMap.get(poolName);
        }
        throw new AsyncExecutorNotFountException("Cannot find alternative async executor with name '{}'", poolName);
    }

    public void addKeyStoreInfo(@NonNull String id, @NonNull KeyStoreInfo keyStoreInfo) {
        if (keyStoreInfoMap.containsKey(id)) {
            throw new LuckyRuntimeException("KeyStoreInfo with id '{}' already exists");
        }
        keyStoreInfoMap.put(id, keyStoreInfo);
    }

    public KeyStoreInfo getKeyStoreInfo(@NonNull String id) {
        return keyStoreInfoMap.get(id);
    }

    /**
     * 获取对象创建器
     *
     * @return 对象创建器
     */
    public ObjectCreator getObjectCreator() {
        return this.objectCreator;
    }

    /**
     * 设置对象创建器
     *
     * @param objectCreator 对象创建器
     */
    public void setObjectCreator(AbstractObjectCreator objectCreator) {
        this.objectCreator = objectCreator;
    }


    //------------------------------------------------------------------------------------------------
    //                                     Timeout Setting
    //------------------------------------------------------------------------------------------------

    /**
     * 获取通用的链接超时时间
     *
     * @return 通用的链接超时时间
     */
    public Integer getConnectionTimeout() {
        return this.connectionTimeout;
    }

    /**
     * 设置通用的链接超时时间
     *
     * @param connectionTimeout 通用的链接超时时间
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * 获取通用的读超时时间
     *
     * @return 通用的读超时时间
     */
    public Integer getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * 设置通用的读超时时间
     *
     * @param readTimeout 通用的读超时时间
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * 获取通用的写超时时间
     *
     * @return 通用的写超时时间
     */
    public Integer getWriteTimeout() {
        return this.writeTimeout;
    }

    /**
     * 设置通用的写超时时间
     *
     * @param writeTimeout 通用的写超时时间
     */
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    //------------------------------------------------------------------------------------------------
    //                                     SSL Setting
    //------------------------------------------------------------------------------------------------

    /**
     * 获取通用的{@link HostnameVerifier}
     *
     * @return 通用的HostnameVerifier
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * 设置通用的{@link HostnameVerifier}
     *
     * @param hostnameVerifier 通用的{@link HostnameVerifier}
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * 获取通用的{@link SSLSocketFactory}
     *
     * @return 通用的SSLSocketFactory
     */
    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * 设置通用的{@link SSLSocketFactory}
     *
     * @param sslSocketFactory 通用的{@link SSLSocketFactory}
     */
    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    //------------------------------------------------------------------------------------------------
    //                                      HttpExecutor
    //------------------------------------------------------------------------------------------------

    /**
     * 获取HTTP执行器{@link HttpExecutor}
     *
     * @return HTTP执行器
     */
    public HttpExecutor getHttpExecutor() {
        return this.httpExecutor;
    }

    /**
     * 设置HTTP执行器{@link HttpExecutor}
     *
     * @param httpExecutor HTTP执行器{@link HttpExecutor}
     */
    public void setHttpExecutor(HttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    //------------------------------------------------------------------------------------------------
    //                                      ExceptionHandle
    //------------------------------------------------------------------------------------------------

    /**
     * 获取通用的异常处理器{@link HttpExceptionHandle}
     *
     * @return 异常处理器
     */
    public HttpExceptionHandle getExceptionHandle() {
        return exceptionHandle;
    }

    /**
     * 设置通用的异常处理器{@link HttpExceptionHandle}
     *
     * @param exceptionHandle 通用的异常处理器{@link HttpExceptionHandle}
     */
    public void setExceptionHandle(HttpExceptionHandle exceptionHandle) {
        this.exceptionHandle = exceptionHandle;
    }

    /**
     * 设置通用的异常处理器生成器{@link Generate Generate&lt;HttpExceptionHandle&gt;}
     *
     * @param exceptionHandleGenerate 通用的异常处理器生成器{@link Generate Generate&lt;HttpExceptionHandle&gt;}
     */
    public void setExceptionHandle(Generate<HttpExceptionHandle> exceptionHandleGenerate) {
        this.exceptionHandleGenerate = exceptionHandleGenerate;
    }

    /**
     * 设置通用的异常处理器
     *
     * @param exceptionHandleClass 异常处理器Class
     * @param exceptionHandleMsg   用于创建异常处理器的额外信息
     * @param scope                异常处理器的作用域{@link Scope}
     * @param handleConsumer       异常处理器创建后的回调函数
     * @param <T>                  异常处理器的类型
     */
    public <T extends HttpExceptionHandle> void setExceptionHandle(Class<T> exceptionHandleClass, String exceptionHandleMsg, Scope scope, Consumer<T> handleConsumer) {
        setExceptionHandle(context -> objectCreator.newObject(exceptionHandleClass, exceptionHandleMsg, context, scope, handleConsumer));
    }

    /**
     * 设置通用的异常处理器
     *
     * @param exceptionHandleClass 异常处理器Class
     * @param scope                异常处理器的作用域{@link Scope}
     * @param handleConsumer       异常处理器创建后的回调函数
     * @param <T>                  异常处理器的类型
     */
    public <T extends HttpExceptionHandle> void setExceptionHandle(Class<T> exceptionHandleClass, Scope scope, Consumer<T> handleConsumer) {
        setExceptionHandle(context -> objectCreator.newObject(exceptionHandleClass, "", context, scope, handleConsumer));
    }

    /**
     * 设置通用的异常处理器
     *
     * @param exceptionHandleClass 异常处理器Class
     * @param scope                异常处理器的作用域{@link Scope}
     * @param <T>                  异常处理器的类型
     */
    public <T extends HttpExceptionHandle> void setExceptionHandle(Class<? extends HttpExceptionHandle> exceptionHandleClass, Scope scope) {
        setExceptionHandle(exceptionHandleClass, "", scope, h -> {
        });
    }

    /**
     * 获取异常处理器{@link HttpExceptionHandle}实例
     * <pre>
     *     1.检查方法的参数列表中是否存在类型为{@link HttpExceptionHandle}的参数，如果存在则直接返回参数列表中第一个匹配的参数值
     *     2.查找方法、类、父类标注的{@link ExceptionHandleMeta @ExceptionHandleMeta}系列注解，从中注解中获取异常处理器{@link HttpExceptionHandle}实例并返回
     *     3.检查是否配置了全局异常处理器{@link #exceptionHandleGenerate}，如果配置了则使用该全局异常处理器
     *     4.上述步骤均未找到时则使用默认的异常处理器{@link DefaultHttpExceptionHandle}
     * </pre>
     *
     * @param methodContext 当前方法上下文
     * @return 异常处理器HttpExceptionHandle
     */
    private HttpExceptionHandle getHttpExceptionHandle(MethodContext methodContext) {
        // 尝从方法参数列表中获取
        for (Object arg : methodContext.getArguments()) {
            if (arg instanceof HttpExceptionHandle) {
                return (HttpExceptionHandle) arg;
            }
        }

        // 尝试从注解中获取
        ExceptionHandleMeta handleMetaAnn = methodContext.getSameAnnotationCombined(ExceptionHandleMeta.class);
        if (handleMetaAnn != null) {
            return createExceptionHandle(methodContext, handleMetaAnn);
        }

        // 尝试从全局异常处理器生成器中获取
        if (this.exceptionHandleGenerate != null) {
            return this.exceptionHandleGenerate.create(methodContext);
        }

        // 返回默认的异常处理器
        return getExceptionHandle();
    }

    private HttpExceptionHandle createExceptionHandle(MethodContext methodContext, ExceptionHandleMeta exceptionHandleMetaAnn) {
        ObjectGenerate handle = exceptionHandleMetaAnn.handle();
        if (HttpExceptionHandle.class != handle.clazz()) {
            try {
                return methodContext.generateObject(handle);
            } catch (Exception e) {
                throw new ExceptionHandleCreateException("An exception occurs when you create an exception processor instance using ‘handle’ configuration：{}", exceptionHandleMetaAnn);
            }
        }
        if (HttpExceptionHandle.class != exceptionHandleMetaAnn.clazz()) {
            try {
                return methodContext.generateObject(exceptionHandleMetaAnn.clazz(), Scope.SINGLETON);
            } catch (Exception e) {
                throw new ExceptionHandleCreateException("An exception occurs when you create an exception processor instance using ‘clazz’ configuration：{}", exceptionHandleMetaAnn);
            }

        }
        throw new ExceptionHandleCreateException("No available configuration found in the exception handler annotation：{}", exceptionHandleMetaAnn);
    }

    //------------------------------------------------------------------------------------------------
    //                                  Interceptor Setting
    //------------------------------------------------------------------------------------------------


    /**
     * 获取全局生效的拦截器执行器
     *
     * @return 全局生效的拦截器执行器
     */
    public List<InterceptorPerformer> getInterceptorPerformerList() {
        return interceptorPerformerList;
    }

    /**
     * 获取全局生效的拦截器执行器的生成器对象
     *
     * @return 全局生效的拦截器执行器的生成器对象
     */
    public List<Generate<InterceptorPerformer>> getPerformerGenerateList() {
        return performerGenerateList;
    }

    /**
     * 新增一组拦截器执行器{@link InterceptorPerformer}
     *
     * @param interceptorPerformers 拦截器执行器数组{@link InterceptorPerformer}
     */
    public void addInterceptorPerformers(InterceptorPerformer... interceptorPerformers) {
        this.interceptorPerformerList.addAll(Arrays.asList(interceptorPerformers));
    }

    /**
     * 新增一组拦截器执行器{@link InterceptorPerformer}
     *
     * @param interceptorPerformers 拦截器执行器集合{@link InterceptorPerformer}
     */
    public void addInterceptorPerformers(Collection<InterceptorPerformer> interceptorPerformers) {
        this.interceptorPerformerList.addAll(interceptorPerformers);
    }

    /**
     * 新增一组单例的拦截器对象
     *
     * @param interceptors 单例拦截器数组
     */
    public void addInterceptors(Interceptor... interceptors) {
        Stream.of(interceptors).forEach(inter -> this.interceptorPerformerList.add(new InterceptorPerformer(inter)));
    }

    /**
     * 新增一个单例的拦截器对象，并指定优先级
     *
     * @param interceptor 单例拦截器数组
     * @param priority    优先级，数值越高优先级越低
     */
    public void addInterceptor(Interceptor interceptor, Integer priority) {
        this.interceptorPerformerList.add(new InterceptorPerformer(interceptor, priority));
    }

    /**
     * 新增一个拦截器执行器生成器对象{@link Generate Generate&lt;InterceptorPerformer&gt;}
     *
     * @param performerGenerate 拦截器执行器生成器对象{@link Generate Generate&lt;InterceptorPerformer&gt;}
     */
    public void addInterceptor(Generate<InterceptorPerformer> performerGenerate) {
        this.performerGenerateList.add(performerGenerate);
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass    拦截器Class
     * @param interceptorMsg      用于创建拦截器的额外信息
     * @param scope               拦截器的作用域{@link Scope}
     * @param interceptorConsumer 拦截器对象创建后的回调函数
     * @param priority            拦截器的优先级，数值越高优先级越低
     * @param <T>                 拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, String interceptorMsg, Scope scope, Consumer<T> interceptorConsumer, Integer priority) {
        addInterceptor(_c -> new InterceptorPerformer(context -> objectCreator.newObject(interceptorClass, interceptorMsg, context, scope, interceptorConsumer), priority));
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass 拦截器Class
     * @param interceptorMsg   用于创建拦截器的额外信息
     * @param scope            拦截器的作用域{@link Scope}
     * @param priority         拦截器的优先级，数值越高优先级越低
     * @param <T>              拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, String interceptorMsg, Scope scope, Integer priority) {
        addInterceptor(_c -> new InterceptorPerformer(context -> objectCreator.newObject(interceptorClass, interceptorMsg, context, scope, i -> {
        }), priority));
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass    拦截器Class
     * @param scope               拦截器的作用域{@link Scope}
     * @param interceptorConsumer 拦截器对象创建后的回调函数
     * @param priority            拦截器的优先级，数值越高优先级越低
     * @param <T>                 拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Consumer<T> interceptorConsumer, Integer priority) {
        addInterceptor(interceptorClass, "", scope, interceptorConsumer, priority);
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass 拦截器Class
     * @param scope            拦截器的作用域{@link Scope}
     * @param priority         拦截器的优先级，数值越高优先级越低
     * @param <T>              拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Integer priority) {
        addInterceptor(interceptorClass, "", scope, i -> {
        }, priority);
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass    拦截器Class
     * @param scope               拦截器的作用域{@link Scope}
     * @param interceptorConsumer 拦截器对象创建后的回调函数
     * @param <T>                 拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope, Consumer<T> interceptorConsumer) {
        addInterceptor(interceptorClass, scope, interceptorConsumer, null);
    }

    /**
     * 新增一个拦截器
     *
     * @param interceptorClass 拦截器Class
     * @param scope            拦截器的作用域{@link Scope}
     * @param <T>              拦截器的类型
     */
    public <T extends Interceptor> void addInterceptor(Class<T> interceptorClass, Scope scope) {
        addInterceptor(interceptorClass, scope, i -> {
        }, null);
    }

    //------------------------------------------------------------------------------------------------
    //                                ResponseConvert Setting
    //------------------------------------------------------------------------------------------------

    /**
     * 获取响应转换器
     * <pre>
     *     1.如果配置了响应转换器生成器对象{@link #responseConvertGenerate}，则优先使用生成器对象创建响应转换器
     *     2.返回用户配置的转换器{@link #responseConvert}
     *     3.未做任何配置时返回null
     * </pre>
     *
     * @param context 上下文对象
     * @return 响应转换器
     */
    public ResponseConvert getResponseConvert(Context context) {
        if (this.responseConvertGenerate != null) {
            return responseConvertGenerate.create(context);
        }
        return getResponseConvert();
    }

    /**
     * 获取用户配置的响应转换器
     *
     * @return 用户配置的响应转换器
     */
    public ResponseConvert getResponseConvert() {
        return responseConvert;
    }

    /**
     * 设置一个响应转换器，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvert 响应转换器
     */
    public void setResponseConvert(ResponseConvert responseConvert) {
        this.responseConvert = responseConvert;
    }

    /**
     * 设置一个响应转换器生成器对象，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvertGenerate 响应转换器生成器对象
     */
    public void setResponseConvert(Generate<ResponseConvert> responseConvertGenerate) {
        this.responseConvertGenerate = responseConvertGenerate;
    }

    /**
     * 设置一个响应转换器生成器对象，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvertClass 响应转换器Class
     * @param responseConvertMsg   创建响应转换器时的额外信息
     * @param scope                响应转换器的作用域
     * @param convertConsumer      响应转化器初始化器
     * @param <T>                  响应转换器的类型
     */
    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, String responseConvertMsg, Scope scope, Consumer<T> convertConsumer) {
        setResponseConvert(context -> objectCreator.newObject(responseConvertClass, responseConvertMsg, context, scope, convertConsumer));
    }

    /**
     * 设置一个响应转换器生成器对象，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvertClass 响应转换器Class
     * @param responseConvertMsg   创建响应转换器时的额外信息
     * @param scope                响应转换器的作用域
     * @param <T>                  响应转换器的类型
     */
    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, String responseConvertMsg, Scope scope) {
        setResponseConvert(context -> objectCreator.newObject(responseConvertClass, responseConvertMsg, context, scope, c -> {
        }));
    }

    /**
     * 设置一个响应转换器生成器对象，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvertClass 响应转换器Class
     * @param scope                响应转换器的作用域
     * @param convertConsumer      响应转化器初始化器
     * @param <T>                  响应转换器的类型
     */
    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, Scope scope, Consumer<T> convertConsumer) {
        setResponseConvert(responseConvertClass, "", scope, convertConsumer);
    }

    /**
     * 设置一个响应转换器生成器对象，此转换器将作为默认转换器在全局生效
     *
     * @param responseConvertClass 响应转换器Class
     * @param scope                响应转换器的作用域
     * @param <T>                  响应转换器的类型
     */
    public <T extends ResponseConvert> void setResponseConvert(Class<T> responseConvertClass, Scope scope) {
        setResponseConvert(responseConvertClass, "", scope, c -> {
        });
    }

    //------------------------------------------------------------------------------------------------
    //                                common http parameter setter
    //------------------------------------------------------------------------------------------------


    /**
     * 设置全局请求头参数
     *
     * @param headerMap 全局请求头参数
     */
    public void setHeaders(Map<String, Object> headerMap) {
        this.headers.putAll(headerMap);
    }

    /**
     * 设置全局请求头参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void addHeader(String name, Object value) {
        this.headers.put(name, value);
    }

    public Map<String, Object> createProxyClassHeaders(Class<?> proxyClass) {
        return createProxyClassMap(this.headers, proxyClass);
    }

    /**
     * 为某个代理类设置专用的请求头参数
     *
     * @param proxyClass        代理类的Class
     * @param proxyClassHeaders 请求头参数
     */
    public void setProxyClassHeaders(Class<?> proxyClass, Map<String, Object> proxyClassHeaders) {
        this.headers.put(proxyClass.getName(), proxyClassHeaders);
    }

    /**
     * 设置全局路径参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void addPathParameter(String name, Object value) {
        this.pathParams.put(name, value);
    }

    /**
     * 设置全局路径参数
     *
     * @param pathMap 全局路径参数
     */
    public void setPathParameters(Map<String, Object> pathMap) {
        this.pathParams.putAll(pathMap);
    }

    public Map<String, Object> createProxyClassPathParameters(Class<?> proxyClass) {
        return createProxyClassMap(this.pathParams, proxyClass);
    }

    /**
     * 为某个代理类设置专用的路径参数
     *
     * @param proxyClass               代理类的Class
     * @param proxyClassPathParameters 路径参数
     */
    public void setProxyClassPathParameters(Class<?> proxyClass, Map<String, Object> proxyClassPathParameters) {
        this.pathParams.put(proxyClass.getName(), proxyClassPathParameters);
    }

    /**
     * 设置全局Query参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void addQueryParameter(String name, Object value) {
        this.queryParams.put(name, value);
    }

    /**
     * 设置全局Query参数
     *
     * @param queryMap Query参数
     */
    public void setQueryParameters(Map<String, Object> queryMap) {
        this.queryParams.putAll(queryMap);
    }

    public Map<String, Object> createProxyClassQueryParameter(Class<?> proxyClass) {
        return createProxyClassMap(this.queryParams, proxyClass);
    }

    /**
     * 为某个代理类设置专用的Query参数
     *
     * @param proxyClass                代理类的Class
     * @param proxyClassQueryParameters Query参数
     */
    public void setProxyClassQueryParameter(Class<?> proxyClass, Map<String, Object> proxyClassQueryParameters) {
        this.queryParams.put(proxyClass.getName(), proxyClassQueryParameters);
    }

    private HttpClientProxyObjectFactory getHttpProxyFactory() {
        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createProxyClassMap(Map<String, Object> sourceMap, Class<?> proxyClass) {
        String proxyClassName = proxyClass.getName();
        Object proxyClassObject = sourceMap.get(proxyClassName);
        if (proxyClassObject instanceof Map) {
            return (Map<String, Object>) proxyClassObject;
        }
        Map<String, Object> proxyClassMap = new ConcurrentHashMap<>();
        sourceMap.put(proxyClassName, proxyClassMap);
        return proxyClassMap;
    }

    //------------------------------------------------------------------------------------------------
    //                               Logger Handler
    //------------------------------------------------------------------------------------------------

    /**
     * 获取日志处理器
     *
     * @return 日志处理器
     */
    public LoggerHandler getLoggerHandler() {
        if (loggerHandler == null) {
            loggerHandler = NotRecordLog.INSTANCE;
        }
        return loggerHandler;
    }

    /**
     * 设置日志处理器
     *
     * @param loggerHandler 日志处理器
     */
    public void setLoggerHandler(LoggerHandler loggerHandler) {
        this.loggerHandler = loggerHandler;
    }

    //------------------------------------------------------------------------------------------------
    //                                Plugin Method
    //------------------------------------------------------------------------------------------------

    /**
     * 获取所有的插件
     *
     * @return 所有的插件
     */
    public List<ProxyPlugin> getPlugins() {
        return plugins;
    }

    /**
     * 设置插件集合
     *
     * @param plugins 插件集合
     */
    public void setPlugins(List<ProxyPlugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * 添加一个插件
     *
     * @param plugin 插件
     */
    public void addPlugin(ProxyPlugin plugin) {
        this.plugins.add(plugin);
    }

    /**
     * 添加一个插件Class，使用反射创建对象
     *
     * @param pluginClass 插件Class
     */
    public void addPlugin(Class<? extends ProxyPlugin> pluginClass) {
        this.plugins.add(ClassUtils.newObject(pluginClass));
    }

    //------------------------------------------------------------------------------------------------
    //                                Generate proxy object
    //------------------------------------------------------------------------------------------------

    /**
     * 获取一个声明式HTTP接口的代理对象
     * <pre>
     *     1.当proxyClass为接口时，使用JDK动态代理
     *     2.当proxyClass为非接口时，使用Cglib动态代理
     * </pre>
     *
     * @param targetClass     声明式HTTP接口的Class
     * @param <T>声明式HTTP接口的类型
     * @return 明式HTTP接口的代理对象
     */
    public <T> T getProxyObject(@NonNull Class<T> targetClass) {
        return targetClass.isInterface() ? getJdkProxyObject(targetClass) : getCglibProxyObject(targetClass);
    }

    /**
     * 【Cglib动态代理】<br/>
     * 获取一个声明式HTTP接口的代理对象
     *
     * @param targetClass 声明式HTTP接口的Class
     * @param <T>         声明式HTTP接口的类型
     * @return 明式HTTP接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getCglibProxyObject(@NonNull Class<T> targetClass) {
        return (T) this.cglibProxyObjectCache.computeIfAbsent(targetClass, _k -> {
            ProxyObjectMetaWrap proxyObjectMetaWrap = new ProxyObjectMetaWrap(targetClass);
            Object cglibProxyObject = ProxyFactory.getCglibProxyObject(targetClass, Enhancer::create, new CglibHttpRequestMethodInterceptor(proxyObjectMetaWrap));
            proxyObjectMetaWrap.setProxyAndInit(cglibProxyObject);
            return proxyObjectMetaWrap;
        }).getProxyObject();
    }

    /**
     * 【JDK动态代理】<br/>
     * 获取一个声明式HTTP接口的代理对象
     *
     * @param targetClass 声明式HTTP接口的Class
     * @param <T>         声明式HTTP接口的类型
     * @return 明式HTTP接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getJdkProxyObject(@NonNull Class<T> targetClass) {
        return (T) this.jdkProxyObjectCache.computeIfAbsent(targetClass, _k -> {
            ProxyObjectMetaWrap proxyObjectMetaWrap = new ProxyObjectMetaWrap(targetClass);
            Object jdkProxyObject = ProxyFactory.getJdkProxyObject(targetClass.getClassLoader(), new Class[]{targetClass}, new JdkHttpRequestInvocationHandler(proxyObjectMetaWrap));
            proxyObjectMetaWrap.setProxyAndInit(jdkProxyObject);
            return proxyObjectMetaWrap;
        }).getProxyObject();
    }

    /**
     * 关闭用于执行异步HTTP任务的线程池资源
     */
    public void shutdown() {
        shutdownAllExecutor(false);
    }

    /**
     * 关闭用于执行异步HTTP任务的线程池资源
     */
    public void shutdownNow() {
        shutdownAllExecutor(true);
    }

    /**
     * 关闭所有线程池
     *
     * @param isShutdownNow 是否使用shutdownNow方法进行资源关闭
     */
    private void shutdownAllExecutor(boolean isShutdownNow) {
        // 关闭默认的线程池
        shutdownLazyExecutor("lucky-client-default-async-executor", lazyAsyncExecutor, isShutdownNow);

        // 关闭所有备用线程池
        this.alternativeAsyncExecutorMap.forEach((k, v) -> shutdownLazyExecutor(k, v, isShutdownNow));

        // 关闭方法定制的线程池
        shutdownMethodExecutor(this.jdkProxyObjectCache.values(), isShutdownNow);
        shutdownMethodExecutor(this.cglibProxyObjectCache.values(), isShutdownNow);
    }

    private void shutdownMethodExecutor(Collection<ProxyObjectMetaWrap> proxyObjectMetaWraps, boolean isShutdownNow) {
        for (ProxyObjectMetaWrap proxyObjectWrap : proxyObjectMetaWraps) {
            for (MethodMetaContext metaContext : proxyObjectWrap.methodMetaContextMap.values()) {
                Executor executor = metaContext.getAsyncTaskExecutor() == null ? null : metaContext.getAsyncTaskExecutor().getExecutor();
                shutdownExecutor(
                        "[method-pool]-" + metaContext.getCurrentAnnotatedElement().getName(),
                        executor,
                        isShutdownNow
                );
            }
        }
    }

    /**
     * 关闭某一个用于执行异步HTTP任务的线程池资源
     *
     * @param name          线程池名称
     * @param lazyExecutor  线程池懒加载对象
     * @param isShutdownNow 是否使用shutdownNow方法进行资源关闭
     */
    private void shutdownLazyExecutor(String name, LazyValue<Executor> lazyExecutor, boolean isShutdownNow) {
        if (lazyExecutor.isInit()) {
            shutdownExecutor(name, lazyExecutor.getValue(), isShutdownNow);
        }
    }

    /**
     * 关闭某一个用于执行异步HTTP任务的线程池资源
     *
     * @param name          线程池名称
     * @param executor      线程池对象
     * @param isShutdownNow 是否使用shutdownNow方法进行资源关闭
     */
    private void shutdownExecutor(String name, Executor executor, boolean isShutdownNow) {
        if (executor instanceof ExecutorService) {
            ExecutorService executorService = (ExecutorService) executor;
            if (!executorService.isShutdown()) {
                if (isShutdownNow) {
                    executorService.shutdownNow();
                } else {
                    executorService.shutdown();
                }
                log.info("Shutting down lucky-client async http task executor '{}'", name);
            }
        }
    }

    //------------------------------------------------------------------------------------------------
    //                                   retry mechanism
    //------------------------------------------------------------------------------------------------


    /**
     * 使用重试机制执一个HTTP请求并返回响应结果
     *
     * @param context 当前方法上下文
     * @param task    HTTP任务
     * @return 响应对象Response
     * @throws Exception 执行过程中可能出现Exception异常
     */
    private Response retryExecute(MethodContext context, Callable<Response> task) throws Throwable {
        // 获取重试执行器，并尝试以重试的方式运行任务，并记录执行时间
        RetryActuator retryActuator = context.getRetryActuator();
        long startTime = System.currentTimeMillis();
        Response response = retryActuator.retryExecute(task, context);
        context.getContextVar().addRootVariable($_EXE_TIME_$, System.currentTimeMillis() - startTime);
        return response;
    }


    //------------------------------------------------------------------------------------------------
    //                               Cglib/Jdk method interceptor
    //------------------------------------------------------------------------------------------------

    /**
     * Cglib方法拦截器
     */
    class CglibHttpRequestMethodInterceptor extends HttpRequestProxy implements MethodInterceptor {

        CglibHttpRequestMethodInterceptor(ProxyObjectMetaWrap proxyObjectMetaWrap) {
            super(proxyObjectMetaWrap);
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            return methodProxy(proxy, method, args, methodProxy);
        }
    }

    /**
     * JDK方法拦截器
     */
    class JdkHttpRequestInvocationHandler extends HttpRequestProxy implements InvocationHandler {

        JdkHttpRequestInvocationHandler(ProxyObjectMetaWrap proxyObjectMetaWrap) {
            super(proxyObjectMetaWrap);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return methodProxy(proxy, method, args, null);
        }
    }

    //------------------------------------------------------------------------------------------------
    //                               Proxy Object Meta Wrap
    //------------------------------------------------------------------------------------------------

    /**
     * 代理对象元信息包装器
     */
    class ProxyObjectMetaWrap {

        /**
         * 代理类的Class
         */
        private final Class<?> targetClass;

        /**
         * 代理类的继承结构
         */
        private final Set<String> targetClassInheritanceStructure;

        /**
         * 被代理的接口Class上下文
         */
        private final ClassContext classContext;

        /**
         * 方法元信息上下文【缓存】
         */
        private final Map<Method, MethodMetaContext> methodMetaContextMap = new ConcurrentHashMap<>(16);

        /**
         * 代理对象
         */
        private Object proxyObject;

        /**
         * Class元信息构造器
         *
         * @param targetClass 被代理类的Class
         */
        public ProxyObjectMetaWrap(Class<?> targetClass) {
            this.targetClass = targetClass;
            this.targetClassInheritanceStructure = ClassUtils.getInheritanceStructure(targetClass);
            this.classContext = new ClassContext(targetClass);
            this.classContext.setHttpProxyFactory(getHttpProxyFactory());
        }

        /**
         * 设置代理对象并初始化
         *
         * @param proxy 代理对象
         */
        public void setProxyAndInit(Object proxy) {
            this.proxyObject = proxy;
            this.classContext.setProxyObject(proxy);
            this.classContext.setContextVar();
        }

        /**
         * 获取代理对象
         *
         * @return 代理对象
         */
        public Object getProxyObject() {
            return proxyObject;
        }

        /**
         * 获取被代理类的Class
         *
         * @return 被代理类的Class
         */
        public Class<?> getTargetClass() {
            return targetClass;
        }

        /**
         * 获取类的继承结构
         *
         * @return 继承结构
         */
        public Set<String> getTargetClassInheritanceStructure() {
            return targetClassInheritanceStructure;
        }

        /**
         * 判断某个类是否为该类的家族成员
         *
         * @param className 带判断的Class
         * @return 是否为该类的家族成员
         */
        public boolean isFamilyMember(String className) {
            return targetClassInheritanceStructure.contains(className);
        }

        /**
         * 获取类上下文对象
         *
         * @return 类上下文对象
         */
        public ClassContext getClassContext() {
            return classContext;
        }

        /**
         * 创建方法元信息上下文
         *
         * @param method 方法实例
         * @return 方法元信息上下文
         * @throws IOException 创建失败会抛出该异常
         */
        public MethodMetaContext createMethodMeta(Method method) throws IOException {
            MethodMetaContext methodMeta = methodMetaContextMap.get(method);
            if (methodMeta == null) {
                methodMeta = new MethodMetaContext(method);
                methodMeta.setParentContext(classContext);
                methodMeta.setContextVar();
                methodMetaContextMap.put(method, methodMeta);
            }
            return methodMeta;
        }

        /**
         * 创建方法上下文
         *
         * @param method 方法实例
         * @param args   方法参数
         * @return 方法上下文
         * @throws IOException 创建失败会抛出该异常
         */
        public MethodContext createMethodContext(Method method, Object[] args) throws IOException {
            return new MethodContext(createMethodMeta(method), args);
        }
    }


    //------------------------------------------------------------------------------------------------
    //                           request encapsulation and execution
    //------------------------------------------------------------------------------------------------

    /**
     * HTTP请求代理类
     */
    class HttpRequestProxy {

        /**
         * 类元信息
         */
        private final ProxyObjectMetaWrap proxyObjectMetaWrap;

        /**
         * 插件缓存
         */
        private final Map<Method, List<ProxyPlugin>> pluginCache = new ConcurrentHashMap<>(16);

        /**
         * 公共请求头参数【缓存】
         */
        private Map<String, Object> commonHeaderParams;

        /**
         * 公共URL参数【缓存】
         */
        private Map<String, Object> commonQueryParams;

        /**
         * 公共路径参数【缓存】
         */
        private Map<String, Object> commonPathParams;

        /**
         * 构造方法，使用一个接口Class来初始化请求代理器
         *
         * @param proxyObjectMetaWrap 被代理类的元信息
         */
        HttpRequestProxy(ProxyObjectMetaWrap proxyObjectMetaWrap) {
            this.proxyObjectMetaWrap = proxyObjectMetaWrap;
        }


        //----------------------------------------------------------------
        //                     Proxy Method
        //----------------------------------------------------------------


        /**
         * 方法代理，当接口方被调用时执行的就是这部分的代码
         *
         * @param proxy       代理对象
         * @param method      接口方法
         * @param args        执行方法时的参数列表
         * @param methodProxy 接口方法代理
         * @return 方法执行结果，即Http请求的结果
         * @throws IOException 执行时可能会发生IO异常
         */
        public Object methodProxy(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            ExecuteMeta exeMeta = new ExecuteMeta(
                    proxyObjectMetaWrap.createMethodMeta(method),
                    proxyObjectMetaWrap.getTargetClass(),
                    proxy,
                    method,
                    methodProxy,
                    args,
                    meta -> this.doMethodProxy(meta.getProxy(), meta.getMethod(), meta.getArgs(), meta.getMethodProxy())
            );
            List<ProxyPlugin> proxyPlugins = getProxyPlugins(exeMeta);
            ProxyDecorator decorator = new ProxyDecorator(proxyPlugins, exeMeta);
            return decorator.proceed();
        }

        /**
         * 获取所有的代理插件集合
         *
         * @param exeMeta 执行元数据
         * @return 作用与当前方法的所有插件
         */
        private synchronized List<ProxyPlugin> getProxyPlugins(ExecuteMeta exeMeta) {
            // 先查询缓存
            Method method = exeMeta.getMethod();
            List<ProxyPlugin> proxyPlugins = pluginCache.get(method);
            if (proxyPlugins != null) {
                return proxyPlugins;
            }

            // 缓存中没有时则创建
            Map<String, ProxyPlugin> proxyPluginMap = new LinkedHashMap<>(16);

            // 注册全局生效的插件
            for (ProxyPlugin plugin : getPlugins()) {
                if (plugin.match(exeMeta)) {
                    proxyPluginMap.put(plugin.uniqueIdentification(), plugin);
                }
            }

            // 注册由注解注入的插件
            MethodMetaContext methodMeta = exeMeta.getMetaContext();
            List<Plugin> pluginAnnList = methodMeta.findNestCombinationAnnotationsCheckParent(Plugin.class);
            for (Plugin pluginAnn : pluginAnnList) {
                Class<? extends Annotation> prohibition = pluginAnn.prohibition();
                if (methodMeta.isAnnotatedCheckParent(prohibition)) {
                    continue;
                }
                ProxyPlugin plugin = methodMeta.generateObject(pluginAnn.plugin(), pluginAnn.pluginClass(), ProxyPlugin.class);
                String pluginId = plugin.uniqueIdentification();
                if (plugin.match(exeMeta)) {
                    proxyPluginMap.put(pluginId, plugin);
                }
            }

            // 插件Map转List
            proxyPlugins = proxyPluginMap.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(proxyPluginMap.values());

            pluginCache.put(method, proxyPlugins);
            return proxyPlugins;
        }


        /**
         * 方法代理，当接口方被调用时执行的就是这部分的代码
         *
         * @param proxy  代理对象
         * @param method 接口方法
         * @param args   执行方法时的参数列表
         * @return 方法执行结果，即Http请求的结果
         * @throws IOException 执行时可能会发生IO异常
         */
        private Object doMethodProxy(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            // 接口的default方法
            if (method.isDefault()) {
                return MethodUtils.invokeDefault(proxy, method, args);
            }

            // hashCode方法
            if (ReflectionUtils.isHashCodeMethod(method)) {
                return proxy.getClass().hashCode();
            }

            // toString方法
            if (ReflectionUtils.isToStringMethod(method)) {
                return proxyObjectMetaWrap.getTargetClass().getName() + proxy.getClass().getSimpleName();
            }

            // 非抽象方法
            if (!Modifier.isAbstract(method.getModifiers())) {
                return methodProxy != null ? methodProxy.invokeSuper(proxy, args) : MethodUtils.invoke(proxy, method, args);
            }

            // 除去上述特殊方法，其他方法均会被代理
            MethodContext methodContext = proxyObjectMetaWrap.createMethodContext(method, args);

            try {
                return methodContext.isImmediateExecutionWrapperMethod()
                        ? invokeWrapperMethod(methodContext)
                        : invokeHttpProxyMethod(methodContext);
            } finally {
                methodContext.destroy();
            }
        }

        /**
         * 执行Wrapper方法
         *
         * @param methodContext 方法上下文
         * @return 执行结果
         */
        private Object invokeWrapperMethod(MethodContext methodContext) {
            // 执行被@Async注解标注或者在当前上下文中存在__$async$__且值为TRUE的void方法
            if (methodContext.isAsyncMethod()) {
                methodContext.getAsyncTaskExecutor().execute(methodContext::invokeWrapperMethod);
                return null;
            }

            // 执行返回值类型为Future的方法
            if (methodContext.isFutureMethod()) {
                CompletableFuture<?> completableFuture = methodContext.getAsyncTaskExecutor().supplyAsync(methodContext::invokeWrapperMethod);
                return ListenableFuture.class.isAssignableFrom(methodContext.getReturnType())
                        ? new CompletableToListenableFutureAdapter<>(completableFuture)
                        : completableFuture;
            }
            // 执行非异步方法
            Object wrapResult = methodContext.invokeWrapperMethod();
            return methodContext.isOptionalMethod()
                    ? Optional.ofNullable(wrapResult)
                    : wrapResult;
        }

        /**
         * 构建并执行HTTP请求
         * <pre>
         *     1.构建HTTP请求实例{@link Request}
         *     2.设置公共的请求参数
         *     3.解析{@link StaticParam @StaticParam}系列注解获取配置的静态请求参数
         *     4.解析{@link DynamicParam @DynamicParam}系列注解获取配置的动态请求参数
         *     5.设置SSL相关的请求参数
         *     6.获取异常处理器实例
         *     7.获取所有的拦截器实例并组装成一个拦截器链
         *     8.执行HTTP请求获取响应对象
         *
         * </pre>
         *
         * @param methodContext 方法上下文
         * @return 方法执行结果，即Http请求的结果
         */
        private Object invokeHttpProxyMethod(MethodContext methodContext) {
            Request request;
            HttpExceptionHandle exceptionHandle;
            InterceptorPerformerChain interceptorChain;
            try {
                // 获取基本请求体
                request = createBaseRequest(methodContext);
                // 将请求信息添加到SpEL上下文中
                methodContext.setRequestVar(request);
                // 公共参数设置
                commonParamSetting(request);
                // 加载静态参数
                methodContext.loadStaticParams(request);
                // 加载动态参数
                methodContext.loadDynamicParams(request);
                // SSL相关参数的配置
                sslSetting(request, methodContext);
                // 获取异常处理器
                exceptionHandle = getHttpExceptionHandle(methodContext);
                // 获取拦截器链
                interceptorChain = methodContext.getInterceptorChain();

            } catch (Exception e) {
                ClassContext classContext = methodContext.lookupContext(ClassContext.class);
                Class<?> clazz = classContext.getCurrentAnnotatedElement();
                Method method = methodContext.getCurrentAnnotatedElement();
                throw new RequestConstructionException(e, "Failed to create a request instance for the proxy method ['{}#{}()']", clazz.getName(), method.getName()).printException(log);
            }

            // 执行被@Async注解标注或者在当前上下文中存在__$async$__且值为TRUE的void方法
            if (methodContext.isAsyncMethod()) {
                methodContext.getAsyncTaskExecutor().execute(() -> executeRequest(request, methodContext, interceptorChain, exceptionHandle));
                return null;
            }

            // 执行返回值类型为Future的方法
            if (methodContext.isFutureMethod()) {
                CompletableFuture<?> completableFuture = methodContext.getAsyncTaskExecutor().supplyAsync(() -> executeRequest(request, methodContext, interceptorChain, exceptionHandle));
                return ListenableFuture.class.isAssignableFrom(methodContext.getReturnType())
                        ? new CompletableToListenableFutureAdapter<>(completableFuture)
                        : completableFuture;
            }

            // 执行非异步方法
            Object executeResult = executeRequest(request, methodContext, interceptorChain, exceptionHandle);
            return methodContext.isOptionalMethod()
                    ? Optional.ofNullable(executeResult)
                    : executeResult;
        }

        //----------------------------------------------------------------
        //         request instance creation and parameter setting
        //----------------------------------------------------------------

        /**
         * 创建一个基本的请求实例
         * <pre>
         *     1.首先会尝试从方法参数列表中获取{@link Request}对象，如果能获取到则直接返回。
         *     2.其次会解析类和方法上的注解获取{@link Request}对象
         * </pre>
         *
         * @param methodContext 方法上下文
         * @return 基本的请求实例
         */
        private Request createBaseRequest(MethodContext methodContext) throws Exception {
            // 首先尝试从方法参数列表中获取Request对象
            Request methodArgRequest = getMethodArgRequest(methodContext);
            if (methodArgRequest != null) {
                return methodArgRequest;
            }
            // 获取接口Class中配置的域名
            String domainName = getDomainName(methodContext);
            // 获取方法中配置的Url信息
            TempPair<String, RequestMethod> httpRequestInfo = getHttpRequestInfo(methodContext);
            // 构建Request对象
            return AnnotationRequest.create(domainName, httpRequestInfo.getOne(), httpRequestInfo.getTwo());
        }

        /**
         * 从方法参数中获取{@link Request}对象
         *
         * @param methodContext 方法上下文
         * @return 方法参数中Request对象
         */
        private Request getMethodArgRequest(MethodContext methodContext) {
            for (Object arg : methodContext.getArguments()) {
                if (arg instanceof Request) {
                    return (Request) arg;
                }
            }
            return null;
        }

        /**
         * 获取通过{@link DomainNameMeta}注解配置在接口上的域名
         *
         * @param context 方法上下文
         * @return 配置在接口上的域名
         */
        private String getDomainName(MethodContext context) throws Exception {
            // 构建域名注解上下文
            DomainNameMeta domainMetaAnn = context.getMergedAnnotationCheckParent(DomainNameMeta.class);
            if (domainMetaAnn == null) {
                return DomainNameMeta.EMPTY;
            }
            DomainNameContext domainNameContext = new DomainNameContext(context, domainMetaAnn);

            // 获取域名获取器的创建信息并创建实例
            DomainNameGetter domainNameGetter = context.generateObject(domainMetaAnn.getter());

            // 通过域名获取器获取域名信息
            return domainNameGetter.getDomainName(domainNameContext);
        }

        /**
         * 获取通过{@link HttpRequest}注解配置在方法上的URL和HTTP请求方法
         *
         * @param context 方法上下文
         * @return 配置在方法上的URL和HTTP请求方法
         */
        private TempPair<String, RequestMethod> getHttpRequestInfo(MethodContext context) throws Exception {
            HttpRequest httpReqAnn = context.getMergedAnnotationCheckParent(HttpRequest.class);
            if (httpReqAnn == null) {
                throw new RequestConstructionException("The current method is not an HTTP proxy method: {}", context.getCurrentAnnotatedElement());
            }
            HttpRequestContext httpRequestContext = new HttpRequestContext(context, httpReqAnn);
            URLGetter urlGetter = context.generateObject(httpReqAnn.urlGetter());
            String resourceURI = urlGetter.getUrl(httpRequestContext);

            return TempPair.of(resourceURI, httpReqAnn.method());
        }

        /**
         * 公共参数设置
         * <pre>
         *     1.设置SSL相关的公共配置
         *     2.设置公共的超时时间
         *     3.设置公共的请求头参数
         *     4.设置公共的Query参数
         *     5.设置公共的Path路径参数
         *     6.设置公共的Form表单参数
         *     7.设置公共的文件参数
         * </pre>
         *
         * @param request 请求实例
         */
        private void commonParamSetting(Request request) {
            commonSSLSetting(request);
            commonTimeoutSetting(request);
            commonHeadersSetting(request);
            commonQueryParamsSetting(request);
            commonPathParamsSetting(request);
        }


        private void commonSSLSetting(Request request) {
            HostnameVerifier verifier = getHostnameVerifier();
            SSLSocketFactory socketFactory = getSslSocketFactory();
            if (verifier != null) {
                request.setHostnameVerifier(verifier);
            }
            if (socketFactory != null) {
                request.setSSLSocketFactory(socketFactory);
            }
        }

        private void commonTimeoutSetting(Request request) {
            Integer connectionTimeout = getConnectionTimeout();
            Integer readTimeout = getReadTimeout();
            Integer writeTimeout = getWriteTimeout();

            if (connectionTimeout != null && connectionTimeout > 0) {
                request.setConnectTimeout(connectionTimeout);
            }

            if (readTimeout != null && readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }

            if (writeTimeout != null && writeTimeout > 0) {
                request.setWriterTimeout(writeTimeout);
            }
        }

        private void commonHeadersSetting(Request request) {
            Map<String, Object> headerParams = getCommonHeaderParams();
            headerParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addHeader(n, ve));
                } else {
                    request.addHeader(n, v);
                }
            });
        }

        private void commonQueryParamsSetting(Request request) {
            Map<String, Object> queryParams = getCommonQueryParams();
            queryParams.forEach((n, v) -> {
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(ve -> request.addQueryParameter(n, ve));
                } else {
                    request.addQueryParameter(n, v);
                }
            });
        }

        private void commonPathParamsSetting(Request request) {
            request.setPathParameter(getCommonPathParams());
        }

        private Map<String, Object> getCommonPathParams() {
            if (commonPathParams == null) {
                commonPathParams = getCommonMapParam(pathParams);
            }
            return commonPathParams;
        }

        private Map<String, Object> getCommonQueryParams() {
            if (commonQueryParams == null) {
                commonQueryParams = getCommonMapParam(queryParams);
            }
            return commonQueryParams;
        }

        private Map<String, Object> getCommonHeaderParams() {
            if (commonHeaderParams == null) {
                commonHeaderParams = getCommonMapParam(headers);
            }
            return commonHeaderParams;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getCommonMapParam(Map<String, Object> mapParam) {
            Map<String, Object> realMapParam = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : mapParam.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (proxyObjectMetaWrap.isFamilyMember(key) && (value instanceof Map)) {
                    realMapParam.putAll((Map<? extends String, Object>) value);
                } else {
                    try {
                        Class.forName(key);
                    } catch (ClassNotFoundException e) {
                        if ((value instanceof Map)) {
                            realMapParam.put(key, new ArrayList<>(((Map<String, Object>) value).values()));
                        } else {
                            realMapParam.put(key, value);
                        }
                    }

                }
            }
            return realMapParam;
        }


        /**
         * SSL认证相关的设置
         *
         * @param request       请求实例
         * @param methodContext 当前方法执行环境上下文
         */
        private void sslSetting(Request request, MethodContext methodContext) {
            SSLMeta sslMetaAnn = methodContext.getSameAnnotationCombined(SSLMeta.class);
            if (sslMetaAnn != null) {
                HostnameVerifierBuilder hostnameVerifierBuilder = methodContext.generateObject(sslMetaAnn.hostnameVerifierBuilder());
                SSLSocketFactoryBuilder sslSocketFactoryBuilder = methodContext.generateObject(sslMetaAnn.sslSocketFactoryBuilder());
                SSLAnnotationContext context = new SSLAnnotationContext(methodContext, sslMetaAnn);
                request.setHostnameVerifier(hostnameVerifierBuilder.getHostnameVerifier(context));
                request.setSSLSocketFactory(sslSocketFactoryBuilder.getSSLSocketFactory(context));
            }
        }


        //----------------------------------------------------------------
        //               Extension component acquisition
        //----------------------------------------------------------------


        /**
         * 执行HTTP请求并将响应结果转化为方法的返回值类型，出现异常时使用异常处理器处理异常
         * <pre>
         *     1.将本次请求相关的信息以变量的形式添加到SpEL运行时环境中
         *     2.执行所有拦截器的{@link Interceptor#beforeExecute}方法
         *     3.使用重试机制执行HTTP请求并得到响应
         *     4.将响应相关的信息以变量的形式添加到SpEL运行时环境中
         *     5.执行所有拦截器的{@link Interceptor#afterExecute}方法
         *     6.将响应结果转化为方法的返回值类型
         *          a.如果是void方法直接返回null
         *          b.如果方法被{@link ConvertProhibition @ConvertProhibition}注解标注，则表示禁止使用转换器，直接调用{@link Response#getEntity(Type)}方法得到并返回结果
         *          c.查找方法、类、父类标注的{@link ResultConvertMeta @ResultConvert}注解，从中注解中获取相应的转化器{@link ResponseConvert}实例对响应结果进行转换
         *          d.如果方法、类、父类上不存在{@link ResultConvertMeta @ResultConvert}注解，则直接调用{@link Response#getEntity(Type)}方法得到并返回结果
         *     7.使用异常处理器{@link HttpExceptionHandle}进行异常处理
         *     8.自动释放资源
         * </pre>
         *
         * @param request       请求实例
         * @param methodContext 方法上下文
         * @param handle        异常处理器
         * @return 请求转换结果
         */
        private Object executeRequest(Request request, MethodContext methodContext, InterceptorPerformerChain interceptorChain, HttpExceptionHandle handle) {
            Response response = null;
            try {
                // 执行REQUEST Hook
                methodContext.useHook(Lifecycle.REQUEST);

                // 如果是Wrapper方法则直接执行
                if (methodContext.isReqCreatCompleteExecutionWrapperMethod()) {
                    return methodContext.invokeWrapperMethod();
                }

                // 执行拦截器的前置处理逻辑
                interceptorChain.beforeExecute(request, methodContext);

                // 获取日志处理器
                LoggerHandler logger = getLoggerHandler();

                // 记录请求日志
                logger.recordRequestLog(methodContext, request);

                // 使用重试机制执行HTTP请求
                response = retryExecute(methodContext, () -> doExecuteRequest(request, methodContext));

                // 记录元响应日志
                logger.recordMetaResponseLog(methodContext, response);

                // 执行拦截器的后置处理逻辑
                response = interceptorChain.afterExecute(response, methodContext);

                // 设置响应变量
                methodContext.setResponseVar(response);

                // 记录最终响应日志
                logger.recordFinalResponseLog(methodContext, response);

                // 是否配置了禁用转换器
                if (methodContext.isConvertProhibition()) {
                    // 默认结果处理方法
                    return methodContext.handleResultAndReturn(response.getEntity(methodContext.getResultType()));
                }

                // 如果存在ResponseConvert优先使用该转换器转换结果
                ResultConvertMeta resultConvertMetaAnn = methodContext.getSameAnnotationCombined(ResultConvertMeta.class);
                ResponseConvert convert = resultConvertMetaAnn == null ? getResponseConvert(methodContext) : methodContext.generateObject(resultConvertMetaAnn.convert());
                if (convert != null) {
                    return methodContext.handleResultAndReturn(convert.convert(response, new ConvertContext(methodContext, resultConvertMetaAnn)));
                }
                return methodContext.handleResultAndReturn(response.getEntity(methodContext.getResultType()));
            } catch (Throwable throwable) {
                methodContext.setThrowableVar(throwable);
                return handle.exceptionHandler(methodContext, request, throwable);
            } finally {
                if (methodContext.needAutoCloseResource()) {
                    if (response != null) {
                        response.closeResource();
                    }
                }
            }
        }
    }

    /**
     * 执行HTTP请求返回响应结果，这里可以扩展Mock相关的功能
     *
     * @param request       请求实例
     * @param methodContext 方法上下文
     * @return 响应结果
     */
    private Response doExecuteRequest(Request request, MethodContext methodContext) {
        long startTime = System.currentTimeMillis();
        // 检查是否有Mock相关的配置，如果有，优先使用Mock的执行逻辑
        // 首先尝试从环境变量中获取
        MockResponseFactory mockRespFactory = methodContext.getVar(__$MOCK_RESPONSE_FACTORY$__, MockResponseFactory.class);
        if (mockRespFactory != null) {
            return mockRespFactory.createMockResponse(request, new MockContext(methodContext, null));
        }

        // 其次尝试从注解中获取
        MockMeta mockAnn = methodContext.getSameAnnotationCombined(MockMeta.class);
        if (mockAnn != null && (!StringUtils.hasText(mockAnn.enable()) || methodContext.parseExpression(mockAnn.enable(), boolean.class))) {
            methodContext.getContextVar().addVariable(__$IS_MOCK$__, true);
            MockResponseFactory mockResponseFactory = methodContext.generateObject(mockAnn.mock());
            return mockResponseFactory.createMockResponse(request, new MockContext(methodContext, mockAnn));
        }

        // 没有Mock配置时执行真正的请求
        return methodContext.getHttpExecutor().execute(request);
    }
}
