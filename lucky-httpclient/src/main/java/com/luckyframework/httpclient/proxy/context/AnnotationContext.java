package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.spel.ContextSpELExecution;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.SpELVarManager;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CONTEXT;

/**
 * 注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/31 16:00
 */
public class AnnotationContext implements SpELVarManager, ContextSpELExecution {


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

    public <T> T getRootVar(String name, Class<T> typeClass) {
        return context.getRootVar(name, typeClass);
    }

    public <T> T getNestRootVar(String name, Class<T> typeClass) {
        return context.getNestRootVar(name, typeClass);
    }

    public Object getRootVar(String name) {
        return context.getRootVar(name);
    }

    public Object getNestRootVar(String name) {
        return context.getNestRootVar(name);
    }

    public <T> T getVar(String name, Class<T> typeClass) {
        return context.getVar(name, typeClass);
    }

    public <T> T getNestVar(String name, Class<T> typeClass) {
        return context.getNestVar( name, typeClass);
    }

    public Object getVar(String name) {
        return context.getVar(name);
    }

    public Object getNestVar(String name) {
        return context.getNestVar(name);
    }

    public Object runFunction(String function) {
        return context.runFunction(function);
    }

    public Object nestRunFunction(String function) {
        return context.nestRunFunction(function);
    }

    public <T> T runFunction(String function, Class<T> returnType) {
        return context.runFunction(function, returnType);
    }

    public <T> T nestRunFunction(String function, Class<T> returnType) {
        return context.nestRunFunction(function, returnType);
    }

    public Class<?> getConvertMetaType() {
        return context.getConvertMetaType();
    }

    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return context.parseExpression(expression, returnType, setter);
    }

    @Override
    public <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return context.nestParseExpression(expression, returnType, setter);
    }

    public <T> T generateObject(ObjectGenerate objectGenerate) {
        return this.context.generateObject(objectGenerate);
    }

    @NonNull
    @Override
    public MapRootParamWrapper getGlobalVar() {
        return context.getGlobalVar();
    }

    @Override
    public void setContextVar() {
        this.context.setContextVar();
        context.getContextVar().addRootVariable(CONTEXT, this);
        context.getContextVar().addRootVariable(ANNOTATION_INSTANCE, getAnnotation());
    }

    @NonNull
    @Override
    public MapRootParamWrapper getContextVar() {
        return context.getContextVar();
    }

    @Override
    public void setRequestVar(Request request) {
        this.context.setRequestVar(request);
    }

    @NonNull
    @Override
    public MapRootParamWrapper getRequestVar() {
        return context.getRequestVar();
    }

    @Override
    public void setVoidResponseVar(VoidResponse voidResponse, Context context) {
        this.context.setVoidResponseVar(voidResponse);
    }

    @NonNull
    @Override
    public MapRootParamWrapper getVoidResponseVar() {
        return context.getVoidResponseVar();
    }

    @Override
    public void setResponseVar(Response response, Context context) {
        this.context.setResponseVar(response);
    }

    @NonNull
    @Override
    public MapRootParamWrapper getResponseVar() {
        return context.getResponseVar();
    }

    @NonNull
    @Override
    public MapRootParamWrapper getFinallyVar() {
        return context.getFinallyVar();
    }
}
