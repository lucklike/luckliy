package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.StandardObjectParam;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.FieldContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.CarrySetterParamInfo;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamConstant.*;

/**
 * 标准的Object动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/1 07:20
 */
public class StandardObjectDynamicParamResolver extends AbstractDynamicParamResolver {

    private Supplier<ParameterSetter> defaultSetterSupplier = QUERY_SETTER_SUPPLIER;
    private Supplier<DynamicParamResolver> defaultResolverSupplier = LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_SUPPLIER;


    @Override
    public List<CarrySetterParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        StandardObjectParam standardObjectParam = context.toAnnotation(StandardObjectParam.class);
        if (standardObjectParam != null) {
            defaultResolverSupplier = () -> valueContext.generateObject(standardObjectParam.baseResolver());
            defaultSetterSupplier = () -> valueContext.generateObject(standardObjectParam.setter());
        }

        String name = getParamName(valueContext, standardObjectParam);
        if (valueContext.isMapInstance()) {
            return parserMap(name, ((Map<?, ?>) valueContext.getValue()), valueContext, standardObjectParam);
        }
        if (valueContext.isIterableInstance()) {
            return parserIterable(name, ContainerUtils.getIterator(valueContext.getValue()), valueContext, standardObjectParam);
        }
        if (valueContext.isNullValue() || valueContext.isSimpleBaseType()) {
            return defaultResolverSupplier.get().parser(new DynamicParamContext(valueContext, standardObjectParam))
                    .stream()
                    .map(pi -> new CarrySetterParamInfo(pi.getName(), pi.getValue(), defaultSetterSupplier.get()))
                    .collect(Collectors.toList());
        }
        return parserEntity(name, valueContext.getValue(), valueContext, standardObjectParam);
    }

    private String getParamName(ValueContext context, Annotation dynamicParamAnn) {
        if (context instanceof ParameterContext) {
            if (context.isSimpleBaseType() || context.isIterableInstance()) {
                return getOriginalParamName(context);
            }
            return dynamicParamAnn == null
                    ? ""
                    : context.toAnnotation(dynamicParamAnn, DynamicParam.class).name();
        }
        return context.getName();
    }


    private List<CarrySetterParamInfo> parserMap(String prefix, Map<?, ?> map, ValueContext context, Annotation argDynamicAnn) {
        List<CarrySetterParamInfo> resultList = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Assert.notNull(key, "map key is null");
            Object value = entry.getValue();
            String newPrefix = getPrefix(prefix, String.valueOf(key));
            parserAppendObject(resultList, newPrefix, value, context, argDynamicAnn);
        }
        return resultList;
    }

    private List<CarrySetterParamInfo> parserIterable(String prefix, Iterator<?> iterator, ValueContext context, Annotation argDynamicAnn) {
        List<CarrySetterParamInfo> resultList = new ArrayList<>();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            parserAppendObject(resultList, prefix, value, context, argDynamicAnn);
        }
        return resultList;
    }

    private List<CarrySetterParamInfo> parserEntity(String prefix, Object entity, ValueContext context, Annotation argDynamicAnn) {
        List<CarrySetterParamInfo> resultList = new ArrayList<>();
        ClassContext classContext = new ClassContext(entity.getClass());
        classContext.setParentContext(context);
        for (FieldContext fieldContext : classContext.getFieldContexts(entity)) {
            if (fieldContext.notHttpParam()) {
                continue;
            }
            fieldContext.setParentContext(context);
            DynamicParam dynamicParamAnn = fieldContext.getSameAnnotationCombined(DynamicParam.class);
            String nextPrefix = getPrefix(prefix, fieldContext.getName());
            fieldContext.setName(nextPrefix);

            // 属性上存在@DynamicParam注解时
            if (dynamicParamAnn != null) {
                String dyName = dynamicParamAnn.name();
                if (StringUtils.hasText(dyName)) {
                    fieldContext.setName(getPrefix(prefix, dyName));
                }
                TempPair<Supplier<ParameterSetter>, Supplier<DynamicParamResolver>> pair = DynamicParamLoader.defaultSetterResolver(dynamicParamAnn, defaultSetterSupplier, defaultResolverSupplier, fieldContext);
                ParameterSetter setter = pair.getOne().get();
                pair.getTwo().get().parser(new DynamicParamContext(fieldContext, dynamicParamAnn)).forEach(pi -> {
                    resultList.add(pi instanceof CarrySetterParamInfo
                            ? ((CarrySetterParamInfo) pi)
                            : new CarrySetterParamInfo(pi, setter));
                });
            }
            // 忽略空值和响应处理器
            else if (fieldContext.isNullValue() || fieldContext.isResponseProcessorInstance()) {
                // ignore value
            }
            // 资源类型参数 File、Resource、MultipartFile、HttpFile以及他们的数组和集合类型
            else if (fieldContext.isResourceType()) {
                STANDARD_HTTP_FILE_RESOLVER.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, STANDARD_HTTP_FILE_SETTER));
                });
            }
            // 二进制类型参数 byte[]、Byte[]、InputStream
            else if (fieldContext.isBinaryType()) {
                STANDARD_BINARY_RESOLVER.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, STANDARD_BODY_SETTER));
                });
            }
            // 请求体类型参数
            else if (fieldContext.isBodyObjectInstance()) {
                RETURN_ORIGINAL_RESOLVER.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, STANDARD_BODY_SETTER));
                });
            }
            // 基本类型参数
            else if (fieldContext.isSimpleBaseType()) {
                defaultResolverSupplier.get().parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetterSupplier.get()));
                });

            }
            // 复杂类型参数
            else {
                this.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetterSupplier.get()));
                });
            }
        }
        return resultList;
    }

    private void parserAppendObject(List<CarrySetterParamInfo> resultList, String prefix, Object value, ValueContext context, Annotation argDynamicAnn) {
        if (value == null || ClassUtils.isSimpleBaseType(value.getClass())) {
            resultList.add(new CarrySetterParamInfo(prefix, value, defaultSetterSupplier.get()));
        } else if (value instanceof Map) {
            resultList.addAll(parserMap(prefix, ((Map<?, ?>) value), context, argDynamicAnn));
        } else if (ContainerUtils.isIterable(value)) {
            resultList.addAll(parserIterable(prefix, ContainerUtils.getIterator(value), context, argDynamicAnn));
        } else {
            resultList.addAll(parserEntity(prefix, value, context, argDynamicAnn));
        }
    }

    private String getPrefix(String oldPrefix, String newPrefix) {
        return StringUtils.hasText(oldPrefix)
                ? oldPrefix + "." + newPrefix
                : newPrefix;
    }

}
