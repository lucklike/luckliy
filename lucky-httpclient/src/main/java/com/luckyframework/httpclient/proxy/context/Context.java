package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpExec;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.spel.ContextSpELExecution;
import com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.ProperSourcesParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.LazyValue;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT_ANNOTATED_ELEMENT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.HTTP_EXECUTOR;

/**
 * 上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 19:21
 */
@SuppressWarnings("all")
public abstract class Context extends DefaultSpELVarManager implements ContextSpELExecution {

    /**
     * IF表达式正则
     */
    Pattern IF_PATTERN = Pattern.compile("^@if\\s*\\([\\S\\s]*?\\)\\s*:");

    /**
     * SpEL表达式中访问普通变量时需要带上的前缀
     */
    String SPEL_VARIABLE_PREFIX = "#";

    /**
     * 当前正在执行的代理对象
     */
    private Object proxyObject;

    /**
     * 父上下文
     */
    private Context parentContext;

    /**
     * HTTP代理对象工厂
     */
    private HttpClientProxyObjectFactory httpProxyFactory;

    /**
     * HTTP执行器
     */
    private HttpExecutor httpExecutor;

    /**
     * 当前注解元素
     */
    private final AnnotatedElement currentAnnotatedElement;

    /**
     * 合并注解缓存
     */
    private final Map<Class<? extends Annotation>, Annotation> mergedAnnotationMap = new ConcurrentHashMap<>(8);

    /**
     * 组合注解缓存
     */
    private final Map<Class<? extends Annotation>, Annotation> combinedAnnotationMap = new ConcurrentHashMap<>(8);

    /**
     * 同名组合注解缓存
     */
    private final Map<Class<? extends Annotation>, Annotation> sameSombinedAnnotationMap = new ConcurrentHashMap<>(8);

    /**
     * 上下文构造器
     *
     * @param currentAnnotatedElement 注解元素
     */
    public Context(AnnotatedElement currentAnnotatedElement) {
        this.currentAnnotatedElement = currentAnnotatedElement;
    }

    /**
     * 获取当前正在执行的代理对象<br/>
     * 优先从本类中获取，本类中获取不到时会在父上下文中获取
     *
     * @return 当前正在执行的代理对象
     */
    public Object getProxyObject() {
        if (proxyObject != null) {
            return proxyObject;
        }
        return parentContext == null ? null : parentContext.getProxyObject();
    }

    /**
     * 为当前上线文对象设置代理对象
     *
     * @param proxyObject 代理对象
     */
    public void setProxyObject(Object proxyObject) {
        this.proxyObject = proxyObject;
    }

    /**
     * 获取当前上下文的父上下文实例
     *
     * @return 当前上下文的父上下文实例
     */
    public Context getParentContext() {
        return parentContext;
    }

    /**
     * 为当前上下文设置父上下文实例
     *
     * @param parentContext 父上下文实例
     */
    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }

    /**
     * 获取Http客户端代理对象工厂
     *
     * @return Http客户端代理对象工厂
     */
    public HttpClientProxyObjectFactory getHttpProxyFactory() {
        return httpProxyFactory == null ? (parentContext == null ? null : parentContext.getHttpProxyFactory()) : httpProxyFactory;
    }

    /**
     * 获取SpEL转换器
     *
     * @return SpEL转换器
     */
    public SpELConvert getSpELConverter() {
        HttpClientProxyObjectFactory factory = getHttpProxyFactory();
        return factory == null ? null : factory.getSpELConverter();
    }

    /**
     * 获取Http执行器
     * <pre>
     *     1.从SpEL环境变量中取变量名为`HTTP_EXECUTOR`的执行器变量
     *     2.查找{@link HttpExec @HttpExec}注解，取注解中配置的执行器
     *     3.使用默认的执行器
     * </pre>
     *
     * @return Http执行器
     */
    public synchronized HttpExecutor getHttpExecutor() {
        if (httpExecutor == null) {
            HttpExecutor spelExecutor = getVar(HTTP_EXECUTOR, HttpExecutor.class);
            if (spelExecutor != null) {
                httpExecutor = spelExecutor;
            } else {
                HttpExec execAnn = getMergedAnnotationCheckParent(HttpExec.class);
                if (execAnn != null && execAnn.exec().clazz() != HttpExecutor.class) {
                    httpExecutor = generateObject(execAnn.exec());
                } else {
                    httpExecutor = getHttpProxyFactory().getHttpExecutor();
                }
            }
        }
        return httpExecutor;
    }

    /**
     * 设置Http客户端代理对象工厂
     *
     * @param httpProxyFactory Http客户端代理对象工厂
     */
    public void setHttpProxyFactory(HttpClientProxyObjectFactory httpProxyFactory) {
        this.httpProxyFactory = httpProxyFactory;
    }

    /**
     * 从当前上下文中获取<b>合并注解</b>信息
     *
     * @param annotationClass 注解类型
     * @param <A>             注解类型
     * @return 注解实例
     */
    public <A extends Annotation> A getMergedAnnotation(Class<A> annotationClass) {
        return (A) this.mergedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.findMergedAnnotation(this.currentAnnotatedElement, annotationClass));
    }

    /**
     * 从当前上下文中获取<b>合并注解</b>信息，本上下文中获取不到时会尝试从父上下文中获取
     *
     * @param annotationClass 注解类型
     * @param <A>             注解类型
     * @return 注解实例
     */
    public <A extends Annotation> A getMergedAnnotationCheckParent(Class<A> annotationClass) {
        A mergedAnn = getMergedAnnotation(annotationClass);
        if (mergedAnn == null && parentContext != null) {
            mergedAnn = parentContext.getMergedAnnotationCheckParent(annotationClass);
        }
        return mergedAnn;
    }

    /**
     * 获取组合注解
     *
     * @param annotationClass 注解Class
     * @return 组合注解
     */
    public Annotation getCombinedAnnotation(Class<? extends Annotation> annotationClass) {
        return this.combinedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.getCombinationAnnotation(this.currentAnnotatedElement, annotationClass));
    }

    /**
     * 获取注解元素上指定注解对应的组合注解实例,如果是Repeatable注解会被展开
     *
     * @param annotationType 注解类型
     * @param <A>            注解类型
     * @return 注解元素上所有指定类型注解对应的组合注解实例
     */
    public <A extends Annotation> List<A> getCombinedAnnotations(Class<A> annotationClass) {
        return AnnotationUtils.getCombinationAnnotations(this.currentAnnotatedElement, annotationClass);
    }

    /**
     * 获取组合注解，本上下文中获取不到时会尝试从父上下文中获取
     *
     * @param annotationClass 注解Class
     * @return 组合注解
     */
    public Annotation getCombinedAnnotationCheckParent(Class<? extends Annotation> annotationClass) {
        Annotation combinedAnn = getCombinedAnnotation(annotationClass);
        if (combinedAnn == null && parentContext != null) {
            combinedAnn = parentContext.getCombinedAnnotationCheckParent(annotationClass);
        }
        return combinedAnn;
    }

    /**
     * 获取同名的注解组合
     *
     * @param annotationClass 注解Class
     * @param <A>             注解类型
     * @return 同名的注解组合
     */
    public <A extends Annotation> A getSameAnnotationCombined(Class<A> annotationClass) {
        return (A) this.sameSombinedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.sameAnnotationCombined(this.currentAnnotatedElement, annotationClass));
    }

    /**
     * 获取注解属性，并转为对应的类型
     *
     * @param annotation    注解实例
     * @param attributeName 注解属性名
     * @param type          转换的类型
     * @param <T>           转换的类型
     * @return 注解属性值值
     */
    public <T> T getAnnotationAttribute(Annotation annotation, String attributeName, Class<T> type) {
        Object attributeValue = getAnnotationAttribute(annotation, attributeName);
        if (attributeValue == null) {
            return null;
        }
        if (type.isAssignableFrom(attributeValue.getClass())) {
            return (T) attributeValue;
        }
        return ConversionUtils.conversion(attributeValue, type);
    }

    /**
     * 获取注解属性
     *
     * @param annotation    注解实例
     * @param attributeName 注解属性名
     * @return 注解属性值
     */
    public Object getAnnotationAttribute(Annotation annotation, String attributeName) {
        return annotation == null ? null : AnnotationUtils.getValue(annotation, attributeName);
    }

    /**
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @param ignoreSourceAnn 是否忽略元注解类型的注解实例
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> findNestCombinationAnnotations(Class<A> annotationClass, boolean ignoreSourceAnn) {
        return findNestCombinationAnnotations(this.currentAnnotatedElement, annotationClass, ignoreSourceAnn);
    }

    /**
     * 【不忽略元注解类型的注解实例】
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> findNestCombinationAnnotations(Class<A> annotationClass) {
        return findNestCombinationAnnotations(annotationClass, false);
    }

    /**
     * 【忽略元注解类型的注解实例】
     * 查找当前方法实例上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotationClass 注解类型
     * @return 找到的所有组合注解实例
     */
    public <A extends Annotation> List<A> getNestCombinationAnnotationsIgnoreSource(Class<A> annotationClass) {
        return findNestCombinationAnnotations(annotationClass, true);
    }

    /**
     * 获取当前注解元素
     *
     * @return 当前注解元素
     */
    public AnnotatedElement getCurrentAnnotatedElement() {
        return currentAnnotatedElement;
    }

    /**
     * 检测当前注解元素是否被某个注解标注
     *
     * @param annotationClass 注解Class
     * @return true[被标注]/false[为被标注]
     */
    public boolean isAnnotated(Class<? extends Annotation> annotationClass) {
        return AnnotationUtils.isAnnotated(getCurrentAnnotatedElement(), annotationClass);
    }

    /**
     * 检查上下文链中的注解元素是否被某个注解标注
     *
     * @param annotationClass 注解Class
     * @return true[被标注]/false[为被标注]
     */
    public boolean isAnnotatedCheckParent(Class<? extends Annotation> annotationClass) {
        if (isAnnotated(annotationClass)) {
            return true;
        }
        return parentContext != null && parentContext.isAnnotatedCheckParent(annotationClass);
    }

    /**
     * 注解类型转换，将某个未知类型的注解实例转化为某个具体类型
     *
     * @param annotation           未知类型的注解实例
     * @param resultAnnotationType 目标类型
     * @param <A>                  目标类型泛型
     * @return 转化后的目标注解实例
     */
    public <A extends Annotation> A toAnnotation(Annotation annotation, @NonNull Class<A> resultAnnotationType) {
        return AnnotationUtils.toAnnotation(annotation, resultAnnotationType);
    }

    /**
     * 在上下文链中找到某个具体类型的上下文
     *
     * @param contentType 目标上下文类型
     * @param <C>         目标上下文类型
     * @return 目标上下文实例
     */
    public <C extends Context> C lookupContext(Class<C> contentType) {
        Context temp = this;
        while (temp.getClass() != contentType) {
            temp = temp.getParentContext();
        }
        return (C) temp;
    }

    /**
     * 对象实例生成
     *
     * @param objectGenerate 对象生成器注解实例
     * @param <T>            对象类型
     * @return 对象实例
     */
    public <T> T generateObject(ObjectGenerate objectGenerate) {
        return (T) getHttpProxyFactory().getObjectCreator().newObject(objectGenerate, this);
    }

    /**
     * 对象实例生成
     *
     * @param clazz    对象Class
     * @param msg      创建对象的额外信息
     * @param scope    对象的作用域
     * @param consumer 对象的初始化方法
     * @param <T>      对象类型
     * @return 对象实例
     */
    public <T> T generateObject(Class<T> clazz, String msg, Scope scope, Consumer<T> consumer) {
        return (T) getHttpProxyFactory().getObjectCreator().newObject(clazz, msg, this, scope, consumer);
    }

    /**
     * 对象实例生成
     *
     * @param clazz 对象Class
     * @param msg   创建对象的额外信息
     * @param scope 对象的作用域
     * @param <T>   对象类型
     * @return 对象实例
     */
    public <T> T generateObject(Class<T> clazz, String msg, Scope scope) {
        return (T) getHttpProxyFactory().getObjectCreator().newObject(clazz, msg, this, scope);
    }

    /**
     * 获取SpEL转化器
     *
     * @return SpEL转化器
     */
    public SpELConvert getSpELConvert() {
        return getHttpProxyFactory().getSpELConverter();
    }

    /**
     * 获取响应体转化元类型
     *
     * @return 转化元类型
     */
    public Class<?> getConvertMetaType() {
        ConvertMetaType metaTypeAnn = getMergedAnnotationCheckParent(ConvertMetaType.class);
        return metaTypeAnn == null ? Object.class : metaTypeAnn.value();
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getRootVar(String name) {
        return getRootVar(name, Object.class);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getNestRootVar(String name) {
        return getNestRootVar(name, Object.class);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量，并转化为指定的类型
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getRootVar(String name, Class<T> typeClass) {
        SpELConvert spELConverter = getSpELConverter();
        return parseExpression(spELConverter.getExpressionPrefix() + name + spELConverter.getExpressionSuffix(), typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个Root变量，并转化为指定的类型，
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getNestRootVar(String name, Class<T> typeClass) {
        SpELConvert spELConverter = getSpELConverter();
        return nestParseExpression(spELConverter.getExpressionPrefix() + name + spELConverter.getExpressionSuffix(), typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getVar(String name) {
        return getVar(name, Object.class);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name 变量名
     * @return 变量值
     */
    public Object getNestVar(String name) {
        return getNestVar(name, Object.class);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量，并转化为指定的类型
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getVar(String name, Class<T> typeClass) {
        return getRootVar(SPEL_VARIABLE_PREFIX + name, typeClass);
    }

    /**
     * 获取SpEL运行时环境中的某个普通变量，并转化为指定的类型，
     * 如果转化结果中依然包含表达式，则会继续转换，直到得到最终的结果
     *
     * @param name      变量名
     * @param typeClass 类型Cass
     * @param <T>       结果类型泛型
     * @return 指定类型的对象实例
     */
    public <T> T getNestVar(String name, Class<T> typeClass) {
        return getNestRootVar(SPEL_VARIABLE_PREFIX + name, typeClass);
    }

    /**
     * 获取一个函数执行器
     *
     * @param name 函数名
     * @return 函数执行器
     */
    public FunExecutor getFun(String name) {
        Object fun = getVar(name);
        if (fun instanceof FunExecutor) {
            return (FunExecutor) fun;
        }
        if (fun instanceof Method) {
            return new FunExecutor() {
                @Override
                public <T> T call(Object... args) {
                    try {
                        return (T) MethodUtils.invoke(null, (Method) fun, args);
                    } catch (Exception e) {
                        throw new LuckyReflectionException(e, "Function call failed: '{}'", name);
                    }

                }
            };
        }
        throw new IllegalArgumentException("Unsupported fun: " + name);
    }

    /**
     * 函数调用
     *
     * @param name 函数名
     * @param args 参数列表
     * @param <T>  结果泛型
     * @return 运行结果
     */
    public <T> T callFun(String name, Object... args) {
        return getFun(name).call(args);
    }

    /**
     * 解析SpEL表达式，并将结果转化为指定的类型
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}  ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``  ->  表示不需要使用嵌套解析}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 结果类型
     * @param setter     参数设置器，用于向当前SpEL运行时环境中添加额外的参数
     * @param <T>        结果类型泛型
     * @return 表达式结果
     */
    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return getSpELConvert().parseExpression(getFinalParamWrapper(expression, returnType, setter));
    }

    /**
     * [明确指定使用嵌套解析]
     * 解析SpEL表达式，并将结果转化为指定的类型
     *
     * @param expression SpEL表达式
     * @param returnType 结果类型
     * @param setter     参数设置器，用于向当前SpEL运行时环境中添加额外的参数
     * @param <T>        结果类型泛型
     * @return 表达式结果
     */
    @Override
    public <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return getSpELConvert().nestParseExpression(getFinalParamWrapper(expression, returnType, setter));
    }

    /**
     * 获取全局变量参数集
     *
     * @return 全局变量参数集
     */
    @NonNull
    @Override
    public MapRootParamWrapper getGlobalVar() {
        return getHttpProxyFactory().getGlobalSpELVar();
    }

    /**
     * 设置默认的上下文变量
     */
    @Override
    public void setContextVar() {
        getContextVar().addRootVariable(CONTEXT, LazyValue.of(this));
        getContextVar().addRootVariable(CONTEXT_ANNOTATED_ELEMENT, LazyValue.of(this::getCurrentAnnotatedElement));
        importSpELVar();
    }

    /**
     * 获取最终的SpEL运行时参数集
     *
     * @return 最终的SpEL运行时参数集
     */
    @NonNull
    @Override
    public ProperSourcesParamWrapper getFinallyVar() {
        ProperSourcesParamWrapper finalVar = new ProperSourcesParamWrapper();
        megerParentParamWrapper(finalVar, "GLOBAL-VAR", this, Context::getGlobalVar);
        megerParentParamWrapper(finalVar, "CONTEXT-VAR",this, Context::getContextVar);
        megerParentParamWrapper(finalVar, "REQUEST-VAR",this, Context::getRequestVar);
        megerParentParamWrapper(finalVar, "RESPONSE-VAR",this, Context::getResponseVar);
        return finalVar;
    }

    /**
     * 设置响应参数集
     *
     * @param response 响应对象
     */
    public void setResponseVar(Response response) {
        setResponseVar(response, this);
    }

    /**
     * IF表达式计算
     *
     * @param expression 表达式
     * @return 计算结果
     */
    public String ifExpressionEvaluation(String expression) {
        Matcher matcher = IF_PATTERN.matcher(expression);
        if (matcher.find()) {
            String group = matcher.group();
            String ifExp = group.substring(group.indexOf('(') + 1, group.lastIndexOf(')'));
            try {
                if (parseExpression(ifExp, boolean.class)) {
                    return expression.replaceFirst(IF_PATTERN.pattern(), "");
                }
                return null;
            } catch (Exception e) {
                throw new IllegalArgumentException("@if expression evaluation exception: '" + expression + "' => '" + ifExp + "'", e);
            }
        } else {
            return expression;
        }
    }

    /**
     * 获取最终的SpEL运行时参数集，并在其基础上添加额外的变量集，以及指定返回结果类型
     *
     * @param expression SpEL表达式
     * @param returnType 结果类型
     * @param setter     参数设置器，用于向当前SpEL运行时环境中添加额外的参数
     * @return 最终的SpEL运行时参数集
     */
    private ProperSourcesParamWrapper getFinalParamWrapper(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        ProperSourcesParamWrapper finalParamWrapper = getFinallyVar();
        finalParamWrapper.setExpression(expression);
        finalParamWrapper.setExpectedResultType(returnType);
        setter.setting(finalParamWrapper);
        return finalParamWrapper;
    }

    /**
     * 合并上下文链上的所有参数集
     *
     * @param sourceParamWrapper   源参数
     * @param context              上下文对象
     * @param paramWrapperFunction 参数集获取的方法
     * @return 合并后的参数集
     */
    private void megerParentParamWrapper(ProperSourcesParamWrapper sourceParamWrapper, String sourceName, Context context, Function<Context, MapRootParamWrapper> paramWrapperFunction) {
        Context pc = context.getParentContext();
        if (pc != null) {
            megerParentParamWrapper(sourceParamWrapper, sourceName, pc, paramWrapperFunction);
        }
        sourceName = StringUtils.format("[{}]-{}", sourceName, context.getClass().getSimpleName());
        sourceParamWrapper.coverMerge(sourceName, paramWrapperFunction.apply(context));
    }

    /**
     * SpEL变量声明与导入
     */
    protected void importSpELVar() {
        importSpELVarByAnnotatedElement(getCurrentAnnotatedElement());
    }

    /**
     * SpEL变量声明与导入
     *
     * @param annotatedElement 注解元素
     */
    protected void importSpELVarByAnnotatedElement(AnnotatedElement annotatedElement) {
        MapRootParamWrapper contextVar = getContextVar();
        for (SpELImport spELImportAnn : AnnotationUtils.getNestCombinationAnnotations(annotatedElement, SpELImport.class)) {

            if (spELImportAnn == null) {
                return;
            }

            // 属性名与属性值之间的分隔符
            String sp = spELImportAnn.separator();

            // 导入包
            contextVar.importPackage(spELImportAnn.pack());

            // 导入函数
            for (Class<?> fun : spELImportAnn.fun()) {
                StaticClassEntry classEntry = StaticClassEntry.create(fun);
                contextVar.addVariables(classEntry.getAllStaticMethods());
            }

            // 导入Root字面量
            for (String rootExp : spELImportAnn.rootLit()) {
                TempPair<String, Object> pair = analyticExpression(rootExp, sp, false);
                contextVar.addRootVariable(pair.getOne(), pair.getTwo());
            }

            // 导入普通字面量
            for (String rootExp : spELImportAnn.varLit()) {
                TempPair<String, Object> pair = analyticExpression(rootExp, sp, false);
                contextVar.addRootVariable(pair.getOne(), pair.getTwo());
            }

            // 导入Root变量
            for (String rootExp : spELImportAnn.root()) {
                TempPair<String, Object> pair = analyticExpression(rootExp, sp, true);
                contextVar.addRootVariable(pair.getOne(), pair.getTwo());
            }

            // 导入普通变量
            for (String valExp : spELImportAnn.var()) {
                TempPair<String, Object> pair = analyticExpression(valExp, sp, true);
                contextVar.addVariable(pair.getOne(), pair.getTwo());
            }
        }
    }

    /**
     * 查找注解元素上标注的某个类型的注解的组合注解实例。
     * 使用嵌套方式进行查找，即会查找注解元素上的所有注解，以及注解上的所有注解......
     *
     * @param annotatedElement 注解元素
     * @param annotationClass  注解类型
     * @param ignoreSourceAnn  是否忽略元注解类型的注解实例
     * @return 找到的所有组合注解实例
     */
    private <A extends Annotation> List<A> findNestCombinationAnnotations(AnnotatedElement annotatedElement, Class<A> annotationClass, boolean ignoreSourceAnn) {

        // 注解元素为Class类型时，还需要查找该类的继承链上的所有Class
        if (annotatedElement instanceof Class) {
            Class<?> temp = ((Class<?>) annotatedElement);
            List<A> annotationList = AnnotationUtils.getNestCombinationAnnotations(temp, annotationClass, ignoreSourceAnn);

            Class<?> superclass = temp.getSuperclass();
            Class<?>[] interfaces = temp.getInterfaces();

            // 查找父类
            if (superclass != null) {
                annotationList.addAll(findNestCombinationAnnotations(superclass, annotationClass, ignoreSourceAnn));
            }

            // 查找接口
            if (ContainerUtils.isNotEmptyArray(interfaces)) {
                for (Class<?> anInterface : interfaces) {
                    annotationList.addAll(findNestCombinationAnnotations(anInterface, annotationClass, ignoreSourceAnn));
                }
            }
            return annotationList;
        }
        // 注解元素为非Class
        else {
            return AnnotationUtils.getNestCombinationAnnotations(annotatedElement, annotationClass, ignoreSourceAnn);
        }
    }


    /**
     * 解析变量声明表达式
     *
     * @param expression  表达式
     * @param separator   属性名与属性值之间的分隔符
     * @param needAnalyze 是否执行值解析
     * @return 最终表达式名和表达式结果组成的Pair
     */
    private TempPair<String, Object> analyticExpression(String expression, String separator, boolean needAnalyze) {
        int index = expression.indexOf(separator);
        if (index == -1) {
            throw new IllegalArgumentException("Wrong @SpELImport expression '" + expression + "' : missing separator '" + separator + "'");
        }

        // 获取变量名和变量值表达式
        String nameExpression = expression.substring(0, index).trim();
        String valueExpression = expression.substring(index + separator.length()).trim();

        if (!needAnalyze) {
            return TempPair.of(nameExpression, valueExpression);
        }

        // 获取最终变量集，执行变量名解析和变量值解析
        ParamWrapper finallyVar = getFinallyVar();
        ParamWrapper namePw = new ParamWrapper(finallyVar).setExpression(nameExpression).setExpectedResultType(String.class);
        ParamWrapper valuePw = new ParamWrapper(finallyVar).setExpression(valueExpression).setExpectedResultType(Object.class);

        return TempPair.of(getSpELConvert().parseExpression(namePw), getSpELConvert().parseExpression(valuePw));
    }
}
