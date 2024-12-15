package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.HookHandler;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * 导包Hook处理器
 */
public class PackHookHandler implements HookHandler {

    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {

        // 校验enable属性，结果为false时不将执行该回调
        Pack packAnn = context.toAnnotation(Pack.class);
        String enable = packAnn.enable();
        if (StringUtils.hasText(enable) && !context.parseExpression(enable, boolean.class)) {
            return;
        }

        Field field = (Field) namespaceWrap.getSource();
        Object fieldValue = getFieldValue(field);
        if (fieldValue != null) {
            importPackages(context, fieldValue);
        }
    }


    /**
     * 导包操作
     *
     * @param context    上下文
     * @param fieldValue 属性值
     */
    private void importPackages(HookContext context, Object fieldValue) {
        if (ContainerUtils.isIterable(fieldValue)) {
            ContainerUtils.getIterable(fieldValue).forEach(item -> {
                context.getContextVar().addPackage(toStr(item));
            });
        } else {
            context.getContextVar().addPackage(toStr(fieldValue));
        }
    }

    private Object getFieldValue(Field field) {
        try {
            return FieldUtils.getValue(null, field);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new PackGetterException(e, "Failed to obtain the field value: '{}'", field.toGenericString());
        }
    }

    private String toStr(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Class) {
            return ((Class<?>) value).getPackage().getName();
        }
        throw new PackGetterException("Unsupported package import value types: '{}'", value);
    }

}
