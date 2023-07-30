package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.proxy.annotations.KV;
import com.luckyframework.httpclient.proxy.annotations.HttpParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.httpclient.proxy.ParameterSetter;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    private final ParameterSetter parameterSetter;
    private final ParameterProcessor parameterProcessor;
    private final Map<String, String> extraParamConfigMap;

    public ParameterSetterWrapper(ParameterSetter parameterSetter, ParameterProcessor parameterProcessor, Map<String, String> extraParamConfigMap) {
        this.parameterSetter = parameterSetter;
        this.parameterProcessor = parameterProcessor;
        this.extraParamConfigMap = extraParamConfigMap;
    }

    public ParameterSetterWrapper(ParameterSetter parameterSetter, ParameterProcessor parameterProcessor) {
        this(parameterSetter, parameterProcessor, new HashMap<>());
    }

    public ParameterSetterWrapper() {
        this(new QueryParameterSetter(), new NotProcessor());
    }

    public void addExtraParamConfig(String name, String value) {
        this.extraParamConfigMap.put(name, value);
    }

    public Map<String, String> getExtraParamConfigMap() {
        return extraParamConfigMap;
    }

    public void setRequest(Request request, String paramName, Object paramValue) {
        // 空值忽略
        if (paramValue == null) {
            return;
        }

        // 响应处理器忽略
        if (paramValue instanceof ResponseProcessor) {
            return;
        }

        // 资源、文件类型资源设置
        if (resourceParamSetting(request, paramName, paramValue)) {
            return;
        }

        // BodyObject类型参数设置
        if (paramValue instanceof BodyObject) {
            request.setBody((BodyObject) paramValue);
        }
        // 基本类型参数设置
        else if (ClassUtils.isSimpleBaseType(paramValue.getClass())) {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(paramValue, this.extraParamConfigMap));
        }
        // Map类型参数设置
        else if (paramValue instanceof Map) {
            mapParamSetting(request, paramName, (Map<?, ?>) paramValue);
        }
        // 可迭代类型参数设置
        else if (ContainerUtils.isIterable(paramValue)) {
            iterateParamSetting(request, paramName, ContainerUtils.getIterator(paramValue));
        }
        // 实体类型参数设置
        else {
            entityParamSetting(request, paramName, paramValue);
        }
    }

    protected boolean resourceParamSetting(Request request, String paramName, Object paramValue) {
        if (paramValue instanceof File) {
            request.addFiles(paramName, (File) paramValue);
            return true;
        }
        if (paramValue instanceof File[]) {
            request.addFiles(paramName, (File[]) paramValue);

        }
        if (paramValue instanceof MultipartFile) {
            request.addMultipartFiles(paramName, (MultipartFile) paramValue);
            return true;
        }
        if (paramValue instanceof MultipartFile[]) {
            request.addMultipartFiles(paramName, (MultipartFile[]) paramValue);
            return true;
        }
        if (paramValue instanceof Resource) {
            request.addResources(paramName, (Resource) paramValue);
            return true;
        }
        if (paramValue instanceof Resource[]) {
            request.addResources(paramName, (Resource[]) paramValue);
            return true;
        }
        if (ContainerUtils.isCollection(paramValue)) {
            Class<?> paramGenericType = ResolvableType.forClass(Collection.class, paramValue.getClass()).getGeneric(0).getRawClass();
            assert paramGenericType != null;
            if (File.class == paramGenericType) {
                request.addFiles(paramName, ConversionUtils.conversion(paramValue, File[].class));
                return true;
            }
            if (MultipartFile.class == paramGenericType) {
                request.addMultipartFiles(paramName, ConversionUtils.conversion(paramValue, MultipartFile[].class));
                return true;
            }
            if (Resource.class.isAssignableFrom(paramGenericType)) {
                request.addResources(paramName, ConversionUtils.conversion(paramValue, Resource[].class));
                return true;
            }
        }
        return false;
    }

    protected void mapParamSetting(Request request, String paramName, Map<?, ?> mapParam) {
        if (this.parameterProcessor.needExpansionAnalysis()) {
            mapParam.forEach((name, value) -> setRequest(request, String.valueOf(name), value));
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(mapParam, this.extraParamConfigMap));
        }
    }

    protected void iterateParamSetting(Request request, String paramName, Iterator<?> iterator) {
        if (this.parameterProcessor.needExpansionAnalysis()) {
            while (iterator.hasNext()) {
                setRequest(request, paramName, iterator.next());
            }
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(iterator, this.extraParamConfigMap));
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

                HttpParam useParamAnn = getHttpParam(classParamAnn, fieldParamAnn);
                if (useParamAnn == null) {
                    setRequest(request, fieldParamName, fieldValue);
                } else {
                    ParameterSetter annParamSetter = ClassUtils.newObject(useParamAnn.paramSetter());
                    ParameterProcessor annParamProcessor = ClassUtils.newObject(useParamAnn.paramProcessor());
                    annParamSetter.set(request, fieldParamName, annParamProcessor.paramProcess(fieldValue, getExtraParamMap(useParamAnn)));
                }
            }
        } else {
            this.parameterSetter.set(request, paramName, this.parameterProcessor.paramProcess(paramValue, this.extraParamConfigMap));
        }

    }

    private Map<String, String> getExtraParamMap(HttpParam httpParamAnn) {
        KV[] kvs = httpParamAnn.extraConfig();
        Map<String, String> map = new HashMap<>(kvs.length);
        for (KV kv : kvs) {
            map.put(kv.name(), kv.value());
        }
        return Collections.unmodifiableMap(map);
    }

    private HttpParam getHttpParam(HttpParam classParamAnn, HttpParam methodParamAnn) {
        if (methodParamAnn != null) {
            return methodParamAnn;
        }
        return classParamAnn;
    }
}
