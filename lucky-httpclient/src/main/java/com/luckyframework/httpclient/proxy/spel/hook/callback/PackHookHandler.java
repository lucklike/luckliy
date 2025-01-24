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
        Field field = (Field) namespaceWrap.getSource();
        Object fieldValue = getFieldValue(field);
        if (fieldValue != null) {
            importPackages(context, getHookInfo(field), fieldValue);
        }
    }


    /**
     * 导包操作
     *
     * @param context    上下文
     * @param hookInfo   hook信息
     * @param fieldValue 属性值
     */
    private void importPackages(HookContext context, String hookInfo, Object fieldValue) {
        if (ContainerUtils.isIterable(fieldValue)) {
            ContainerUtils.getIterable(fieldValue).forEach(item -> {
                context.getContextVar().addPackage(toStr(item, hookInfo));
            });
        } else {
            context.getContextVar().addPackage(toStr(fieldValue, hookInfo));
        }
    }

    private Object getFieldValue(Field field) {
        try {
            return FieldUtils.getValue(null, field);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new PackGetterException(e, "Failed to obtain the field value: '{}'", getHookInfo(field));
        }
    }

    private String toStr(Object value, String hookInfo) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Class) {
            return ((Class<?>) value).getPackage().getName();
        }
        throw new PackGetterException("Unsupported package import value types '{}' hook: '{}'", value, hookInfo);
    }

    private String getHookInfo(Field field) {
        return StringUtils.format("@Pack[{}.{}]", field.getDeclaringClass().getName(), field.getName());
    }

}
