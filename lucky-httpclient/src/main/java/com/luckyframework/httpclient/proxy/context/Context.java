package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ConvertMetaType;
import com.luckyframework.httpclient.proxy.annotations.HttpExec;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.spel.ParamWrapper;
import com.sun.javafx.collections.MappingChange;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT_ANNOTATED_ELEMENT;

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

//    /**
//     * SpEL变量管理器
//     */
//    private final SpELVarManager spELVarManager;

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
//        this.spELVarManager = new ContextSpELVarManager(this);
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
     *
     * @return Http执行器
     */
    public HttpExecutor getHttpExecutor() {
        if (httpExecutor == null) {
            HttpExec execAnn = getMergedAnnotationCheckParent(HttpExec.class);
            if (execAnn != null && execAnn.exec() != HttpExecutor.class) {
                httpExecutor = getHttpProxyFactory().getObjectCreator().newObject(execAnn.exec(), "", this, Scope.SINGLETON);
            } else {
                httpExecutor = getHttpProxyFactory().getHttpExecutor();
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


    public Annotation getCombinedAnnotation(Class<? extends Annotation> annotationClass) {
        return this.combinedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.getCombinationAnnotation(this.currentAnnotatedElement, annotationClass));
    }

    public Annotation getCombinedAnnotationCheckParent(Class<? extends Annotation> annotationClass) {
        Annotation combinedAnn = getCombinedAnnotation(annotationClass);
        if (combinedAnn == null && parentContext != null) {
            combinedAnn = parentContext.getCombinedAnnotationCheckParent(annotationClass);
        }
        return combinedAnn;
    }

    public <A extends Annotation> A getSameAnnotationCombined(Class<? extends Annotation> annotationClass) {
        return (A) this.sameSombinedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.sameAnnotationCombined(this.currentAnnotatedElement, annotationClass));
    }

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

    public Object getAnnotationAttribute(Annotation annotation, String attributeName) {
        return annotation == null ? null : AnnotationUtils.getValue(annotation, attributeName);
    }

    private Set<Annotation> getContainCombinationAnnotations(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass, boolean ignoreSourceAnn) {
        if (annotatedElement instanceof Class) {
            Class<?> temp = ((Class<?>) annotatedElement);
            Set<Annotation> annotationSet = new HashSet<>(AnnotationUtils.getContainCombinationAnnotations(temp, annotationClass, ignoreSourceAnn));

            Class<?> superclass = temp.getSuperclass();
            Class<?>[] interfaces = temp.getInterfaces();

            if (superclass != null) {
                annotationSet.addAll(getContainCombinationAnnotations(superclass, annotationClass, ignoreSourceAnn));
            }

            if (ContainerUtils.isNotEmptyArray(interfaces)) {
                for (Class<?> anInterface : interfaces) {
                    annotationSet.addAll(getContainCombinationAnnotations(anInterface, annotationClass, ignoreSourceAnn));
                }
            }
            return annotationSet;
        } else {
            return AnnotationUtils.getContainCombinationAnnotations(annotatedElement, annotationClass, ignoreSourceAnn);
        }
    }

    public Set<Annotation> getContainCombinationAnnotations(Class<? extends Annotation> annotationClass, boolean ignoreSourceAnn) {
        return getContainCombinationAnnotations(this.currentAnnotatedElement, annotationClass, ignoreSourceAnn);
    }

    public Set<Annotation> getContainCombinationAnnotations(Class<? extends Annotation> annotationClass) {
        return getContainCombinationAnnotations(annotationClass, false);
    }

    public Set<Annotation> getContainCombinationAnnotationsIgnoreSource(Class<? extends Annotation> annotationClass) {
        return getContainCombinationAnnotations(annotationClass, true);
    }

    public AnnotatedElement getCurrentAnnotatedElement() {
        return currentAnnotatedElement;
    }

    public boolean isAnnotated(Class<? extends Annotation> annotationClass) {
        return AnnotationUtils.isAnnotated(getCurrentAnnotatedElement(), annotationClass);
    }

    public boolean isAnnotatedCheckParent(Class<? extends Annotation> annotationClass) {
        if (isAnnotated(annotationClass)) {
            return true;
        }
        return parentContext != null && parentContext.isAnnotatedCheckParent(annotationClass);
    }

    public <A extends Annotation> A toAnnotation(Annotation annotation, @NonNull Class<A> resultAnnotationType) {
        if (annotation == null) {
            return null;
        }
        if (resultAnnotationType == annotation.annotationType()) {
            return (A) annotation;
        }
        return AnnotationUtils.createCombinationAnnotation(resultAnnotationType, annotation);
    }

    public <C extends Context> C lookupContext(Class<C> contentType) {
        Context temp = this;
        while (temp.getClass() != contentType) {
            temp = temp.getParentContext();
        }
        return (C) temp;
    }

    public <T> T generateObject(ObjectGenerate objectGenerate) {
        return (T) getHttpProxyFactory().getObjectCreator().newObject(objectGenerate, this);
    }

    public SpELConvert getSpELConvert() {
        return getHttpProxyFactory().getSpELConverter();
    }

    public Class<?> getConvertMetaType() {
        ConvertMetaType metaTypeAnn = getMergedAnnotationCheckParent(ConvertMetaType.class);
        return metaTypeAnn == null ? Object.class : metaTypeAnn.value();
    }

    public <T> T getRootVar(String name, Class<T> typeClass) {
        SpELConvert spELConverter = getSpELConverter();
        return parseExpression(spELConverter.getExpressionPrefix() + name + spELConverter.getExpressionSuffix(), typeClass);
    }

    public Object getRootVar(String name) {
        return getRootVar(name, Object.class);
    }

    public <T> T getVar(String name, Class<T> typeClass) {
        return getRootVar("#" + name, typeClass);
    }

    public Object getVar(String name) {
        return getVar(name, Object.class);
    }

    public Object runFunction(String function) {
        return runFunction(function, Object.class);
    }

    public <T> T runFunction(String function, Class<T> returnType) {
        if (!function.contains("(") || !function.contains(")")) {
            function += "()";
        }
        return getVar(function, returnType);
    }

    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        MapRootParamWrapper finallyVar = getFinallyVar();
        finallyVar.setExpression(expression);
        finallyVar.setExpectedResultType(returnType);
        setter.setting(finallyVar);
        return getSpELConvert().parseExpression(finallyVar);
    }


    private void importCurrentContextPackage(ContextParamWrapper cpw) {
        ClassContext cc = lookupContext(ClassContext.class);
        if (cc != null) {
            cpw.extractPackages(cc.getCurrentAnnotatedElement().getPackage().getName());
        }
    }

    private void importContextAnnotationVar(ContextParamWrapper cpw) {
        Context pc = getParentContext();
        if (pc != null) {
            pc.importContextAnnotationVar(cpw);
        }
        importCurrentContextAnnotationVar(cpw);
    }

    public void setContextVar() {
        getContextVar().addRootVariable(CONTEXT, this);
        getContextVar().addRootVariable(CONTEXT_ANNOTATED_ELEMENT, getCurrentAnnotatedElement());
    }

    public void setResponseVar(Response response) {
        setResponseVar(response, getConvertMetaType());
    }

    @NotNull
    @Override
    public MapRootParamWrapper getFinallyVar() {
        MapRootParamWrapper finalVar = new MapRootParamWrapper();
        finalVar.mergeVar(megerParentParamWrapper(this, Context::getGlobalVar));
        finalVar.mergeVar(megerParentParamWrapper(this, Context::getContextVar));
        finalVar.mergeVar(megerParentParamWrapper(this, Context::getRequestVar));
        finalVar.mergeVar(megerParentParamWrapper(this, Context::getVoidResponseVar));
        finalVar.mergeVar(megerParentParamWrapper(this, Context::getResponseVar));
        return finalVar;
    }

    private MapRootParamWrapper megerParentParamWrapper(Context context, Function<Context, MapRootParamWrapper> paramWrapperFunction) {
        MapRootParamWrapper resultPw = new MapRootParamWrapper();
        Context pc = context.getParentContext();
        if (pc != null) {
            resultPw.mergeVar(megerParentParamWrapper(pc, paramWrapperFunction));
        }
        resultPw.mergeVar(paramWrapperFunction.apply(context));
        return resultPw;
    }

    private void importCurrentContextAnnotationVar(ContextParamWrapper cpw) {
//        ParamWrapper annPw = spELVarManager.getAnnotationParamWrapper(cpw);
//        cpw.extractPackages(annPw.getKnownPackagePrefixes());
//        cpw.extractRootMap(((Map<String, Object>) annPw.getRootObject()));
//        cpw.extractVariableMap(annPw.getVariables());
    }
}
