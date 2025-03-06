package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.annotations.InterceptorMeta;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorPerformer;
import com.luckyframework.httpclient.proxy.mock.MockMeta;
import com.luckyframework.reflect.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$MOCK_RESPONSE_FACTORY$__;


/**
 * 类的元信息
 */
public class ApiLogInfo {

    private final MethodContext context;

    public ApiLogInfo(MethodContext context) {
        this.context = context;
    }

    /**
     * 代理模型
     *
     * @return 代理模型
     */
    public String getPoxyModel() {
        Object proxyObject = context.getProxyObject();
        return ClassUtils.isJDKProxy(proxyObject) ? "JDK" : "CGLIB";
    }

    /**
     * 代理API对应的类名
     *
     * @return 代理API对应的类名
     */
    public String getApiClassName() {
        return context.getClassContext().getCurrentAnnotatedElement().getName();
    }

    /**
     * 代理API对应的方法名
     *
     * @return 代理API对应的方法名
     */
    public String getApiMethodName() {
        return context.getCurrentAnnotatedElement().toGenericString();
    }

    /**
     * HTTP执行器
     *
     * @return HTTP执行器
     */
    public String getHttpExecutorName() {
        return context.getHttpExecutor().getClass().getName();
    }

    /**
     * 当前代理方法的运行时参数列表信息
     *
     * @return 当前代理方法的运行时参数列表信息
     */
    public ParameterContext[] getArgsInfo() {
        return context.getParameterContexts();
    }

    /**
     * 当前方法是否是一个Mock方法
     *
     * @return 方法是否是一个Mock方法
     */
    public boolean isMock() {
        if (context.getVar(__$MOCK_RESPONSE_FACTORY$__) != null) {
            return true;
        }
        MockMeta mockAnn = context.getSameAnnotationCombined(MockMeta.class);
        return mockAnn != null && (
                !StringUtils.hasText(mockAnn.enable()) ||
                        context.parseExpression(mockAnn.enable(), boolean.class));
    }

    /**
     * 当前方法是否是一个异步方法
     *
     * @return 方法是否是一个异步方法
     */
    public boolean isAsync() {
        return context.isAsyncMethod() || context.isFutureMethod();
    }

    /**
     * 获取简单注解的注解信息
     *
     * @param annotationType 注解类型
     * @return 简单注解的信息
     */
    public TempPair<String, String> getSimpleAnnotationInfo(Class<? extends Annotation> annotationType) {
        Annotation methodAnn = context.getMergedAnnotation(annotationType);
        if (methodAnn != null) {
            return TempPair.of("method", methodAnn.toString());
        }
        Annotation classAnn = context.getParentContext().getMergedAnnotation(annotationType);
        if (classAnn != null) {
            return TempPair.of("class", classAnn.toString());
        }
        return null;
    }

    /**
     * 获取可以标注多次的注解信息
     *
     * @param annotationType 注解类型
     * @return 可以标注多次的注解信息
     */
    public TempPair<List<String>, List<String>> getMultiAnnotationInfo(Class<? extends Annotation> annotationType) {
        List<String> methodAnnList = context.findNestCombinationAnnotations(annotationType).stream().map(Annotation::toString).collect(Collectors.toList());
        List<String> classAnnList = context.getParentContext().findNestCombinationAnnotations(annotationType).stream().map(Annotation::toString).collect(Collectors.toList());
        return TempPair.of(methodAnnList, classAnnList);
    }

    /**
     * 获取拦截器信息
     *
     * @return 拦截器信息集合
     */
    public List<InterceptorInfo> getInterceptorInfo() {
        List<InterceptorPerformer> performerList = context.getInterceptorPerformerList();
        List<InterceptorMeta> interClassAnn = context.getParentContext().findNestCombinationAnnotations(InterceptorMeta.class);
        List<InterceptorMeta> interMethodAnn = context.findNestCombinationAnnotations(InterceptorMeta.class);

        List<InterceptorInfo> interceptorInfoList = new ArrayList<>(performerList.size() + interClassAnn.size() + interMethodAnn.size());

        for (InterceptorPerformer performer : performerList) {
            interceptorInfoList.add(new InterceptorInfo("using", performer.getInterceptor(context).toString(), performer.getPriority(context)));
        }

        for (InterceptorMeta interceptorMeta : interClassAnn) {
            interceptorInfoList.add(new InterceptorInfo("class", interceptorMeta.toString(), interceptorMeta.priority()));
        }

        for (InterceptorMeta interceptorMeta : interMethodAnn) {
            interceptorInfoList.add(new InterceptorInfo("class", interceptorMeta.toString(), interceptorMeta.priority()));
        }
        interceptorInfoList.sort(Comparator.comparing(InterceptorInfo::getPriority));
        return interceptorInfoList;
    }

    /**
     * 拦截器信息
     */
    public static class InterceptorInfo {
        private final String location;
        private final String interceptor;
        private final Integer priority;

        public InterceptorInfo(String location, String interceptor, Integer priority) {
            this.location = location;
            this.interceptor = interceptor;
            this.priority = priority;
        }

        public String getLocation() {
            return location;
        }

        public String getInterceptor() {
            return interceptor;
        }

        public Integer getPriority() {
            return priority;
        }
    }
}
