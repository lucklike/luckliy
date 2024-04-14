package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELConvert;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 19:21
 */
@SuppressWarnings("unchecked")
public abstract class Context extends DefaultSpElInfoCache implements ContextSpELExecution {

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

    public Object getProxyObject() {
        if (proxyObject != null) {
            return proxyObject;
        }
        return parentContext == null ? null : parentContext.getProxyObject();
    }

    public void setProxyObject(Object proxyObject) {
        this.proxyObject = proxyObject;
    }

    public Context(AnnotatedElement currentAnnotatedElement) {
        this.currentAnnotatedElement = currentAnnotatedElement;
    }

    public Context getParentContext() {
        return parentContext;
    }

    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }

    public HttpClientProxyObjectFactory getHttpProxyFactory() {
        return httpProxyFactory == null ? (parentContext == null ? null : parentContext.getHttpProxyFactory()) : httpProxyFactory;
    }

    public void setHttpProxyFactory(HttpClientProxyObjectFactory httpProxyFactory) {
        this.httpProxyFactory = httpProxyFactory;
    }

    public <A extends Annotation> A getMergedAnnotation(Class<A> annotationClass) {
        return (A) this.mergedAnnotationMap.computeIfAbsent(annotationClass, key -> AnnotationUtils.findMergedAnnotation(this.currentAnnotatedElement, annotationClass));
    }

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

    public Object getSpElRootVariable(String name) {
        return parseExpression("#{" + name + "}");
    }

    public <T> T getSpElRootVariable(String name, Class<T> typeClass) {
        return parseExpression("#{" + name + "}", typeClass);
    }

    public Object getSpElVariable(String name) {
        return parseExpression("#{#" + name + "}");
    }

    public <T> T getSpElVariable(String name, Class<T> typeClass) {
        return parseExpression("#{#" + name + "}", typeClass);
    }

    @Override
    public ContextParamWrapper initContextParamWrapper() {
        return ContextSpELExecution.super.initContextParamWrapper().extractContext(this);
    }

    public <T> T parseExpression(String expression, ResolvableType returnType, Consumer<ContextParamWrapper> paramSetter) {
        ContextParamWrapper cpw = initContextParamWrapper();
        paramSetter.accept(cpw);
        initialize(this, cpw);
        return getSpELConvert().parseExpression(cpw.getParamWrapper().setExpression(expression).setExpectedResultType(returnType));
    }

    public <T> T generateObject(ObjectGenerate objectGenerate) {
        return (T) getHttpProxyFactory().getObjectCreator().newObject(objectGenerate, this);
    }

    public SpELConvert getSpELConvert() {
        return getHttpProxyFactory().getSpELConverter();
    }
}
