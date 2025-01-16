package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.VarUnfoldException;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.HookHandler;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 回调方法Hook的处理器
 */
public class CallbackHookHandler implements HookHandler {

    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {

        // 解析运行回调函数
        Callback callbackAnn = context.toAnnotation(Callback.class);
        Method callbackMethod = (Method) namespaceWrap.getSource();
        Object result = executeCallbackMethod(context, callbackMethod);

        // 根据配置来决定是否需要存储函数的返回结果
        if (callbackAnn.storeOrNot() && result != null) {
            addVariable(
                    context,
                    callbackMethod,
                    namespaceWrap.getNamespace(),
                    result
            );
        }
    }

    /**
     * 添加变量
     *
     * @param context   上下文
     * @param method    当前回调方法
     * @param namespace 命名空间
     * @param value     变量值
     */
    private void addVariable(HookContext context, Method method, String namespace, Object value) {
        Callback callbackAnn = context.toAnnotation(Callback.class);

        Map<String, Object> returnVarMap;
        Map<String, Object> varMap = getVarMap(context, method, callbackAnn, getVarName(callbackAnn.storeName(), method), value);

        if (StringUtils.hasText(namespace)) {
            returnVarMap = Collections.singletonMap(namespace, varMap);
        } else {
            returnVarMap = varMap;
        }
        if (callbackAnn.storeType() == VarType.ROOT) {
            context.getContextVar().addRootVariables(returnVarMap);
        } else {
            context.getContextVar().addVariables(returnVarMap);
        }
    }

    /**
     * 获取变量Map
     *
     * @param context     上下文对象
     * @param method      当前回调方法
     * @param callbackAnn Callback注解实例
     * @param name        存储结果的变量名
     * @param value       回调方法运行结果
     * @return 变量Map
     */
    private Map<String, Object> getVarMap(HookContext context, Method method, Callback callbackAnn, String name, Object value) {
        Map<String, Object> varMap;
        if (callbackAnn.unfold()) {
            try {
                varMap = ConversionUtils.conversion(value, new SerializationTypeToken<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new VarUnfoldException(e, "An exception occurs when expanding the result of callback function execution: {}", method.toGenericString());
            }
        } else {
            varMap = Collections.singletonMap(name, value);
        }
        if (callbackAnn.literal()) {
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
     * @param method     当前执行的回调方法
     * @return 用于存储当前回调方法运行结果的变量名
     */
    private String getVarName(String configName, Method method) {
        return StringUtils.hasText(configName) ? configName : "$" + method.getName();
    }

    /**
     * 执行回调方法
     *
     * @param context        上下文
     * @param callbackMethod 回调方法
     * @return 执行结果
     */
    private Object executeCallbackMethod(HookContext context, Method callbackMethod) {
        try {
            return MethodUtils.invoke(null, callbackMethod, context.getMethodParamObject(callbackMethod));
        } catch (LuckyInvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ActivelyThrownException(cause);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new CallbackMethodExecuteException(e, "Callback function running exception: '{}'", callbackMethod.toGenericString());
        }
    }

}
