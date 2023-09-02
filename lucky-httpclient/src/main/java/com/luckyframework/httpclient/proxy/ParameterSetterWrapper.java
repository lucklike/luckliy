package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.annotations.OverDynamicParam;
import com.luckyframework.httpclient.proxy.impl.CachedReflectObjectCreator;
import com.luckyframework.httpclient.proxy.impl.NotProcessor;
import com.luckyframework.httpclient.proxy.impl.QueryParameterSetter;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.sun.org.apache.bcel.internal.generic.Select;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

/**
 * 复杂参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:22
 */
public class ParameterSetterWrapper {

    private final ObjectCreator objectCreator;
    private final ParameterSetter parameterSetter;
    private final ParameterProcessor parameterProcessor;
    private final Annotation annotationInstance;


    public ParameterSetterWrapper(ObjectCreator objectCreator, ParameterSetter parameterSetter, ParameterProcessor parameterProcessor, Annotation annotationInstance) {
        this.objectCreator = objectCreator;
        this.parameterSetter = parameterSetter;
        this.parameterProcessor = parameterProcessor;
        this.annotationInstance = annotationInstance;
    }

    public ParameterSetterWrapper(ObjectCreator objectCreator, ParameterSetter parameterSetter, ParameterProcessor parameterProcessor) {
        this(objectCreator, parameterSetter, parameterProcessor, null);
    }

    public ParameterSetterWrapper(ObjectCreator objectCreator) {
        this(objectCreator, new QueryParameterSetter(), new NotProcessor());
    }

    public ParameterSetterWrapper(ParameterSetter parameterSetter, ParameterProcessor parameterProcessor) {
        this(new CachedReflectObjectCreator(), parameterSetter, parameterProcessor);
    }

    public ParameterSetterWrapper() {
        this(new QueryParameterSetter(), new NotProcessor());
    }

    /**
     * 使用注解元素{@link AnnotatedElement}和对象创建器{@link ObjectCreator}来创建一个包装器实例，创建过程如下：
     *
     * @param annotatedElement 注解元素
     * @param objectCreator    对象创建器
     * @return ParameterSetterWrapper对象
     */
    public static ParameterSetterWrapper createByAnnotatedElement(AnnotatedElement annotatedElement, ObjectCreator objectCreator) {
        DynamicParam dynamicParam = AnnotationUtils.getCombinationAnnotation(annotatedElement, DynamicParam.class);
        OverDynamicParam overDynamicParam = AnnotationUtils.getCombinationAnnotation(annotatedElement, OverDynamicParam.class);
        // 没有任何注解，使用默认构造
        if (dynamicParam == null && overDynamicParam == null) {
            return new ParameterSetterWrapper(objectCreator);
        }

        // dynamicParam为null，overDynamicParam不为null，使用overDynamicParam配置构造
        if (dynamicParam == null) {
            TempPair<ParameterSetter, ParameterProcessor> parameterPair = createParameter(
                    objectCreator,
                    overDynamicParam.overParamSetterMsg(),
                    overDynamicParam.overParamProcessorMsg(),
                    overDynamicParam.overParamSetter(),
                    overDynamicParam.overParamProcessor());
            return new ParameterSetterWrapper(objectCreator, parameterPair.getOne(), parameterPair.getTwo(), overDynamicParam);
        }

        // overDynamicParam为null或者dynamicParam不支持覆盖时，使用dynamicParam配置构造
        if (overDynamicParam == null || !dynamicParam.acceptOverlay()) {
            TempPair<ParameterSetter, ParameterProcessor> parameterPair = createParameter(
                    objectCreator,
                    dynamicParam.paramSetterMsg(),
                    dynamicParam.paramProcessorMsg(),
                    dynamicParam.paramSetter(),
                    dynamicParam.paramProcessor());
            return new ParameterSetterWrapper(objectCreator, parameterPair.getOne(), parameterPair.getTwo(), dynamicParam);
        }

        // overDynamicParam和dynamicParam都不为null，且需要覆盖的情况
        Class<? extends ParameterSetter> setterClass = select(overDynamicParam.overParamSetter(), dynamicParam.paramSetter(), ParameterSetter.class);
        Class<? extends ParameterProcessor> processorClass = select(overDynamicParam.overParamProcessor(), dynamicParam.paramProcessor(), ParameterProcessor.class);
        String setterMsg = select(overDynamicParam.overParamSetterMsg(), dynamicParam.paramSetterMsg(), "");
        String processorMsg = select(overDynamicParam.overParamProcessorMsg(), dynamicParam.paramProcessorMsg(), "");
        TempPair<ParameterSetter, ParameterProcessor> parameterPair = createParameter(
                objectCreator,
                setterMsg,
                processorMsg,
                setterClass,
                processorClass);
        return new ParameterSetterWrapper(objectCreator, parameterPair.getOne(), parameterPair.getTwo(), AnnotationUtils.createCombinationAnnotation(DynamicParam.class, dynamicParam, overDynamicParam));
    }

    private static TempPair<ParameterSetter, ParameterProcessor> createParameter(ObjectCreator objectCreator,
                                                                                 String setterMsg,
                                                                                 String processorMsg,
                                                                                 Class<? extends ParameterSetter> setterClass,
                                                                                 Class<? extends ParameterProcessor> processorClass) {
        ParameterSetter setter = setterClass == ParameterSetter.class ? new QueryParameterSetter() : objectCreator.newObject(setterClass, setterMsg);
        ParameterProcessor processor = processorClass == ParameterProcessor.class ? new NotProcessor() : objectCreator.newObject(processorClass, processorMsg);
        return TempPair.of(setter, processor);
    }

    private static <T> T select(T high, T low, T def) {
        if (high == null && low == null) {
            return def;
        }
        if (high == null) {
            return low;
        }
        if (low == null) {
            return high;
        }
        return high.equals(def) ? low : high;
    }

    public void setRequest(Request request, String paramName, Object paramValue, ResolvableType paramType) {
        // 空值忽略
        if (paramValue == null) {
            return;
        }

        // 响应处理器忽略
        if (paramValue instanceof ResponseProcessor) {
            return;
        }

        // 资源、文件类型资源设置
        if (HttpExecutor.isResourceParam(paramType)) {
            request.addHttpFiles(paramName, HttpExecutor.toHttpFiles(paramValue));
        }
        // BodyObject类型参数设置
        else if (paramValue instanceof BodyObject) {
            request.setBody((BodyObject) paramValue);
        }
        // 基本类型参数设置
        else if (ClassUtils.isSimpleBaseType(paramValue.getClass())) {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(paramValue, this.annotationInstance));
        }
        // Map类型参数设置
        else if (paramValue instanceof Map) {
            mapParamSetting(request, paramName, (Map<?, ?>) paramValue, paramType);
        }
        // 可迭代类型参数设置
        else if (ContainerUtils.isIterable(paramValue)) {
            iterateParamSetting(request, paramName, ContainerUtils.getIterator(paramValue), paramType);
        }
        // 实体类型参数设置
        else {
            entityParamSetting(request, paramName, paramValue);
        }
    }

    protected void mapParamSetting(Request request, String paramName, Map<?, ?> mapParam, ResolvableType paramType) {
        if (this.parameterProcessor.needExpansionAnalysis()) {
            mapParam.forEach((name, value) -> setRequest(request, String.valueOf(name), value, paramType.getGeneric(1)));
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(mapParam, this.annotationInstance));
        }
    }

    protected void iterateParamSetting(Request request, String paramName, Iterator<?> iterator, ResolvableType paramType) {
        if (this.parameterProcessor.needExpansionAnalysis()) {
            ResolvableType elementType = paramType.isArray() ? paramType.getComponentType() : paramType.getGeneric(0);
            while (iterator.hasNext()) {
                setRequest(request, paramName, iterator.next(), elementType);
            }
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(iterator, this.annotationInstance));
        }
    }

    protected void entityParamSetting(Request request, String paramName, Object paramValue) {
        if (this.parameterProcessor.needExpansionAnalysis()) {
            Class<?> paramValueClass = paramValue.getClass();
            DynamicParam classParamAnn = AnnotationUtils.findMergedAnnotation(paramValueClass, DynamicParam.class);
            Field[] fields = ClassUtils.getAllFields(paramValueClass);
            DynamicParam fieldParamAnn;
            for (Field field : fields) {
                if (AnnotationUtils.isAnnotated(field, NotHttpParam.class)) {
                    continue;
                }
                fieldParamAnn = AnnotationUtils.findMergedAnnotation(field, DynamicParam.class);
                String fieldParamName = (fieldParamAnn != null && StringUtils.hasText(fieldParamAnn.name()))
                        ? fieldParamAnn.name() : field.getName();
                Object fieldValue = FieldUtils.getValue(paramValue, field);
                DynamicParam finalParamAnn = getFinalHttpParam(classParamAnn, fieldParamAnn);
                if (finalParamAnn == null) {
                    setRequest(request, fieldParamName, fieldValue, ResolvableType.forField(field));
                } else {
                    ParameterSetter annParamSetter = objectCreator.newObject(finalParamAnn.paramSetter(), finalParamAnn.paramSetterMsg());
                    ParameterProcessor annParamProcessor = objectCreator.newObject(finalParamAnn.paramProcessor(), finalParamAnn.paramProcessorMsg());
                    if (finalParamAnn.acceptOverlay()) {
                        annParamSetter = this.parameterSetter;
                        annParamProcessor = this.parameterProcessor;
                    }
                    annParamSetter.set(request, fieldParamName, annParamProcessor.paramProcess(fieldValue, finalParamAnn));
                }
            }
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(paramValue, this.annotationInstance));
        }
    }

    private DynamicParam getFinalHttpParam(DynamicParam classParamAnn, DynamicParam methodParamAnn) {
        if (methodParamAnn != null) {
            return methodParamAnn;
        }
        return classParamAnn;
    }
}
