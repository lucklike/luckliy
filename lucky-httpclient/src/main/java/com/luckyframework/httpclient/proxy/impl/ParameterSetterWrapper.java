package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.ObjectCreator;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.httpclient.proxy.annotations.HttpParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
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
            HttpParam classParamAnn = AnnotationUtils.findMergedAnnotation(paramValueClass, HttpParam.class);
            Field[] fields = ClassUtils.getAllFields(paramValueClass);
            HttpParam fieldParamAnn;
            for (Field field : fields) {
                if (AnnotationUtils.isAnnotated(field, NotHttpParam.class)) {
                    continue;
                }
                fieldParamAnn = AnnotationUtils.findMergedAnnotation(field, HttpParam.class);
                String fieldParamName = (fieldParamAnn != null && StringUtils.hasText(fieldParamAnn.name()))
                        ? fieldParamAnn.name() : field.getName();
                Object fieldValue = FieldUtils.getValue(paramValue, field);

                HttpParam finalParamAnn = getFinalHttpParam(classParamAnn, fieldParamAnn);
                if (finalParamAnn == null) {
                    setRequest(request, fieldParamName, fieldValue, ResolvableType.forField(field));
                } else {
                    ParameterSetter annParamSetter = objectCreator.newObject(finalParamAnn.paramSetter(), finalParamAnn.paramSetterMsg());
                    ParameterProcessor annParamProcessor = objectCreator.newObject(finalParamAnn.paramProcessor(), finalParamAnn.paramProcessorMsg());
                    annParamSetter.set(request, fieldParamName, annParamProcessor.paramProcess(fieldValue, finalParamAnn));
                }
            }
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(paramValue, this.annotationInstance));
        }
    }

    private HttpParam getFinalHttpParam(HttpParam classParamAnn, HttpParam methodParamAnn) {
        if (methodParamAnn != null) {
            return methodParamAnn;
        }
        return classParamAnn;
    }
}
