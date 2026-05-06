package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.context.FieldContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.io.FileUtils;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * 基本的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/1 07:23
 */
public abstract class AbstractDynamicParamResolver implements DynamicParamResolver {
    @Override
    public List<? extends ParamInfo> parser(DynamicParamContext context) {
        if (isNullValue(context) || isStaticField(context)) {
            return Collections.emptyList();
        }
        return doParser(context);
    }

    protected abstract List<? extends ParamInfo> doParser(DynamicParamContext context);

    /**
     * 是否是空值
     *
     * @param context 上下文对象
     * @return 是否是空值
     */
    public boolean isNullValue(DynamicParamContext context) {
        return context.getContext().isNullValue();
    }

    /**
     * 是否是静态属性
     * @param context 上下文对象
     * @return 是否是静态属性
     */
    public boolean isStaticField(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        if (valueContext instanceof FieldContext) {
            FieldContext fieldContext = (FieldContext) valueContext;
            Field field = (Field) fieldContext.getCurrentAnnotatedElement();
            return Modifier.isStatic(field.getModifiers());
        }
        return false;
    }
}
