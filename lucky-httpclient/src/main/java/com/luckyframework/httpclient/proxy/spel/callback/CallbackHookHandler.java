package com.luckyframework.httpclient.proxy.spel.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.VarUnfoldException;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.HookHandler;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.httpclient.proxy.spel.var.VarType;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * 回调方法Hook的处理器
 */
public class CallbackHookHandler implements HookHandler {

    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {
        Method callbackMethod = (Method) namespaceWrap.getSource();
        Object result = executeCallbackMethod(context, callbackMethod);
        Callback callbackAnn = context.toAnnotation(Callback.class);
        if (callbackAnn.store() && result != null) {

            if (callbackAnn.unfold()) {
                try {
                    result = ConversionUtils.conversion(result, new SerializationTypeToken<Map<String, Object>>() {
                    });
                } catch (Exception e) {
                    throw new VarUnfoldException(e, "将回调方法xxx的运行结果展开为Map时出现异常");
                }
            }

            addVariable(
                    namespaceWrap.getNamespace(),
                    getVarName(callbackAnn.storeName(), callbackMethod),
                    result,
                    context.getContext(),
                    callbackAnn.storeType()
            );
        }
    }

    /**
     * 添加变量
     *
     * @param namespace 命名空间
     * @param name      变量名
     * @param value     变量值
     * @param context   上下文
     * @param type      变量类型
     */
    private void addVariable(String namespace, String name, Object value, Context context, VarType type) {
        Map<String, Object> varMap;
        if (StringUtils.hasText(namespace)) {
            varMap = Collections.singletonMap(namespace, Collections.singletonMap(name, value));
        } else {
            varMap = Collections.singletonMap(name, value);
        }
        if (type == VarType.ROOT) {
            context.getContextVar().addRootVariables(varMap);
        } else {
            context.getContextVar().addVariables(varMap);
        }
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
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new CallbackMethodExecuteException(e, "Failed to execute callback method : {}", callbackMethod.toGenericString());
        }
    }

}
