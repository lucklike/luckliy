package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.CarrySetterParamInfo;
import com.luckyframework.httpclient.proxy.annotations.StandardObjectParam;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.FieldContext;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.annotations.NotHttpParam;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader.QUERY_SETTER;
import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader.LOOK_UP_SPECIAL_ANNOTATION_RESOLVER;
import static com.luckyframework.httpclient.proxy.dynamic.DynamicParamLoader.STANDARD_HTTP_FILE_RESOLVER;

/**
 * 标准的Object动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/1 07:20
 */
public class StandardObjectDynamicParamResolver extends AbstractDynamicParamResolver {

    private
    final ObjectCreator objectCreator;
    private ParameterSetter defaultSetter = QUERY_SETTER;
    private DynamicParamResolver defaultResolver = LOOK_UP_SPECIAL_ANNOTATION_RESOLVER;

    public StandardObjectDynamicParamResolver() {
        this.objectCreator = HttpClientProxyObjectFactory.getObjectCreator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CarrySetterParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        Annotation dynamicParamAnn = context.getAnnotation();
        if (dynamicParamAnn != null) {
            Class<? extends DynamicParamResolver> baseResolverClass = (Class<? extends DynamicParamResolver>) valueContext.getAnnotationAttribute(dynamicParamAnn, StandardObjectParam.ATTRIBUTE_BASE_RESOLVER);
            String baseResolverMsg = valueContext.getAnnotationAttribute(dynamicParamAnn, StandardObjectParam.ATTRIBUTE_BASE_RESOLVER_MSG, String.class);
            defaultResolver = objectCreator.newObject(baseResolverClass, baseResolverMsg);

            Class<? extends ParameterSetter> setterClass = (Class<? extends ParameterSetter>) valueContext.getAnnotationAttribute(dynamicParamAnn, DynamicParam.ATTRIBUTE_PARAM_SETTER);
            String setterMsg = valueContext.getAnnotationAttribute(dynamicParamAnn, DynamicParam.ATTRIBUTE_PARAM_SETTER_MSG, String.class);
            defaultSetter = objectCreator.newObject(setterClass, setterMsg);
        }

        String name = getParamName(valueContext, dynamicParamAnn);
        if (valueContext.isMapInstance()) {
            return parserMap(name, ((Map<?, ?>) valueContext.getValue()), valueContext, dynamicParamAnn);
        }
        if (valueContext.isIterableInstance()) {
            return parserIterable(name, ContainerUtils.getIterator(valueContext.getValue()), valueContext, dynamicParamAnn);
        }
        if (valueContext.isNullValue() || valueContext.isSimpleBaseType()) {
            return defaultResolver.parser(new DynamicParamContext(valueContext, dynamicParamAnn))
                    .stream()
                    .map(pi -> new CarrySetterParamInfo(name, pi.getValue(), defaultSetter))
                    .collect(Collectors.toList());
        }
        return parserEntity(name, valueContext.getValue(), valueContext, dynamicParamAnn);
    }

    private String getParamName(ValueContext context, Annotation dynamicParamAnn) {
        if (context instanceof ParameterContext) {
            if (context.isSimpleBaseType() || context.isIterableInstance()) {
                return getOriginalParamName(context);
            }
            return dynamicParamAnn == null
                    ? ""
                    : context.getAnnotationAttribute(dynamicParamAnn, DynamicParam.ATTRIBUTE_NAME, String.class);
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
        for (FieldContext fieldContext : classContext.getFieldContexts(entity)) {
            if (fieldContext.isAnnotated(NotHttpParam.class)) {
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
                TempPair<ParameterSetter, DynamicParamResolver> pair = DynamicParamLoader.defaultSetterResolver(objectCreator, dynamicParamAnn, defaultSetter, defaultResolver);
                ParameterSetter setter = pair.getOne();
                pair.getTwo().parser(new DynamicParamContext(fieldContext, dynamicParamAnn)).forEach(pi -> {
                    resultList.add(pi instanceof CarrySetterParamInfo
                            ? ((CarrySetterParamInfo) pi)
                            : new CarrySetterParamInfo(pi, setter));
                });
            }
            // 忽略空值和响应处理器
            else if (fieldContext.isNullValue() || fieldContext.isResponseProcessorInstance()) {
                // ignore value
            }
            // 资源类型参数
            else if (fieldContext.isResourceType()) {
                STANDARD_HTTP_FILE_RESOLVER.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetter));
                });

            }
            // 请求体类型参数
            else if (fieldContext.isBodyObjectInstance()) {
                LOOK_UP_SPECIAL_ANNOTATION_RESOLVER.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetter));
                });
            }
            // 基本类型参数
            else if (fieldContext.isSimpleBaseType()) {
                defaultResolver.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetter));
                });

            }
            // 复杂类型参数
            else {
                this.parser(new DynamicParamContext(fieldContext, argDynamicAnn)).forEach(pi -> {
                    resultList.add(new CarrySetterParamInfo(pi, defaultSetter));
                });
            }
        }
        return resultList;
    }

    private void parserAppendObject(List<CarrySetterParamInfo> resultList, String prefix, Object value, ValueContext context, Annotation argDynamicAnn) {
        if (value == null || ClassUtils.isSimpleBaseType(value.getClass())) {
            resultList.add(new CarrySetterParamInfo(prefix, value, defaultSetter));
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
