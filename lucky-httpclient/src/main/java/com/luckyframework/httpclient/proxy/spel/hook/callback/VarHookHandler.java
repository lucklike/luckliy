package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * 变量Hook处理器
 */
public class VarHookHandler extends AbstractValueStoreHookHandler {

    @Override
    protected Object useHookReturnResult(HookContext context, NamespaceWrap namespaceWrap) {
        return getFieldValue(context, namespaceWrap);
    }

    @Override
    protected String geDefStoreName(NamespaceWrap namespaceWrap) {
        return ((Field) namespaceWrap.getSource()).getName();
    }

    @Override
    protected String getStoreDesc(NamespaceWrap namespaceWrap) {
        Field field = (Field) namespaceWrap.getSource();
        return StringUtils.format("@Var[{}.{}]", field.getDeclaringClass().getName(), field.getName());
    }

    private Object getFieldValue(HookContext context, NamespaceWrap namespaceWrap) {
        try {
            Object value = FieldUtils.getValue(null, ((Field) namespaceWrap.getSource()));
            if (value instanceof String) {
                return context.parseExpression((String) value);
            }
            return value;
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new VarGetterException(e, "Failed to obtain the field value: '{}'", getStoreDesc(namespaceWrap));
        }
    }

}
