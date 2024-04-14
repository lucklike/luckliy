package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/31 16:00
 */
public class AnnotationContext implements ContextSpELExecution {


    /**
     * 上下文
     */
    private Context context;

    /**
     * 注解实例
     */
    private Annotation annotation;

    /**
     * 获取上下文
     *
     * @return 上下文
     */
    public Context getContext() {
        return context;
    }


    /**
     * 设置上下文
     *
     * @param context 上下文
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * 获取注解实例
     *
     * @return 重试注解实例
     */
    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * 设置注解实例
     *
     * @param annotation 注解实例
     */
    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    /**
     * 根据属性名获取注解中的属性值
     *
     * @param attributeName 属性名
     * @return 注解中对应的属性值
     */
    public Object getAnnotationAttribute(String attributeName) {
        return this.context.getAnnotationAttribute(this.annotation, attributeName);
    }

    /**
     * 根据属性名获取注解中的属性值，并转成具体的类型
     *
     * @param attributeName 属性名
     * @return 注解中对应的属性值
     */
    public <T> T getAnnotationAttribute(String attributeName, Class<T> type) {
        return this.context.getAnnotationAttribute(this.annotation, attributeName, type);
    }

    public HttpClientProxyObjectFactory getHttpProxyFactory() {
        return this.context.getHttpProxyFactory();
    }

    public <A extends Annotation> A getMergedAnnotation(Class<A> annotationClass) {
        return this.context.getMergedAnnotation(annotationClass);
    }

    public <A extends Annotation> A getMergedAnnotationCheckParent(Class<A> annotationClass) {
        return this.context.getMergedAnnotationCheckParent(annotationClass);
    }

    public Annotation getCombinedAnnotation(Class<? extends Annotation> annotationClass) {
        return this.context.getCombinedAnnotation(annotationClass);
    }

    public Annotation getCombinedAnnotationCheckParent(Class<? extends Annotation> annotationClass) {
        return this.context.getCombinedAnnotationCheckParent(annotationClass);
    }

    public <A extends Annotation> A getSameAnnotationCombined(Class<? extends Annotation> annotationClass) {
        return this.context.getSameAnnotationCombined(annotationClass);
    }


    public Set<Annotation> getContainCombinationAnnotations(Class<? extends Annotation> annotationClass, boolean ignoreSourceAnn) {
        return this.context.getContainCombinationAnnotations(annotationClass, ignoreSourceAnn);
    }

    public Set<Annotation> getContainCombinationAnnotations(Class<? extends Annotation> annotationClass) {
        return getContainCombinationAnnotations(annotationClass, false);
    }

    public Set<Annotation> getContainCombinationAnnotationsIgnoreSource(Class<? extends Annotation> annotationClass) {
        return getContainCombinationAnnotations(annotationClass, true);
    }

    public boolean isAnnotated(Class<? extends Annotation> annotationClass) {
        return this.context.isAnnotated(annotationClass);
    }

    public boolean isAnnotatedCheckParent(Class<? extends Annotation> annotationClass) {
        return this.context.isAnnotatedCheckParent(annotationClass);
    }

    public boolean notNullAnnotated() {
        return this.annotation != null;
    }

    public <A extends Annotation> A toAnnotation(Class<A> annotationType) {
        return context.toAnnotation(annotation, annotationType);
    }

    public Object getSpElRootVariable(String name) {
        return context.getSpElRootVariable(name);
    }

    public <T> T getSpElRootVariable(String name, Class<T> typeClass) {
        return context.getSpElRootVariable(name, typeClass);
    }


    public Object getSpElVariable(String name) {
        return context.getSpElVariable(name);
    }

    public <T> T getSpElVariable(String name, Class<T> typeClass) {
        return context.getSpElVariable(name, typeClass);
    }


    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, Consumer<ContextParamWrapper> paramSetter) {
        return context.parseExpression(expression, returnType, cpw -> {
            cpw.extractAnnotationContext(this);
            paramSetter.accept(cpw);
        });
    }

    public <T> T generateObject(ObjectGenerate objectGenerate){
        return this.context.generateObject(objectGenerate);
    }
}
