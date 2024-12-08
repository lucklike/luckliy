package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.VarUnfoldException;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.HookHandler;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 变量Hook处理器
 */
public class VarHookHandler implements HookHandler {

    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {

        // 校验enable属性，结果为false时不将执行该回调
        Var varAnn = context.toAnnotation(Var.class);
        String enable = varAnn.enable();
        if (StringUtils.hasText(enable) && !context.parseExpression(enable, boolean.class)) {
            return;
        }

        Field field = (Field) namespaceWrap.getSource();
        Object fieldValue = getFieldValue(field);
        if (fieldValue != null) {
            addVariable(context, field, namespaceWrap.getNamespace(), fieldValue);
        }
    }


    /**
     * 添加变量到上下文中
     *
     * @param context    上下文
     * @param field      属性对象
     * @param namespace  命名空间
     * @param fieldValue 属性值
     */
    private void addVariable(HookContext context, Field field, String namespace, Object fieldValue) {
        Var varAnn = context.toAnnotation(Var.class);

        Map<String, Object> returnVarMap;
        Map<String, Object> varMap = getVarMap(context, field, varAnn, getVarName(varAnn.name(), field), fieldValue);

        if (StringUtils.hasText(namespace)) {
            returnVarMap = Collections.singletonMap(namespace, varMap);
        } else {
            returnVarMap = varMap;
        }
        if (varAnn.type() == VarType.ROOT) {
            context.getContextVar().addRootVariables(returnVarMap);
        } else {
            context.getContextVar().addVariables(returnVarMap);
        }
    }

    /**
     * 获取变量Map
     *
     * @param context 上下文对象
     * @param field   当前回调方法
     * @param varAnn  Var注解实例
     * @param name    存储结果的变量名
     * @param value   回调方法运行结果
     * @return 变量Map
     */
    private Map<String, Object> getVarMap(HookContext context, Field field, Var varAnn, String name, Object value) {
        Map<String, Object> varMap;
        if (varAnn.unfold()) {
            try {
                varMap = ConversionUtils.conversion(value, new SerializationTypeToken<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new VarUnfoldException(e, "An exception occurs when expanding the Hook property: {}", field.toGenericString());
            }
        } else {
            varMap = Collections.singletonMap(name, value);
        }
        if (varAnn.literal()) {
            return varMap;
        }

        Map<String, Object> afterCalculationMap = new LinkedHashMap<>();
        varMap.forEach((k, v) -> {
            String varName = context.parseExpression(k);
            Object varValue = context.getParsedValue(v);
            afterCalculationMap.put(varName, varValue);
        });
        return afterCalculationMap;
    }

    /**
     * 获取变量名
     *
     * @param configName 配置的变量名
     * @param field      当前执行的回调方法
     * @return 用于存储当前回调方法运行结果的变量名
     */
    private String getVarName(String configName, Field field) {
        return StringUtils.hasText(configName) ? configName : field.getName();
    }


    private Object getFieldValue(Field field) {
        try {
            return FieldUtils.getValue(null, field);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new CallbackMethodExecuteException(e, "Callback function running exception: {}", field.toGenericString());
        }
    }

}
