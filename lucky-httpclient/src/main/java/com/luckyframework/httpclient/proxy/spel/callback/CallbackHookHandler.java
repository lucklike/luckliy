package com.luckyframework.httpclient.proxy.spel.callback;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.HookContext;
import com.luckyframework.httpclient.proxy.spel.HookHandler;
import com.luckyframework.httpclient.proxy.spel.NamespaceWrap;
import com.luckyframework.httpclient.proxy.spel.var.VarType;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

public class CallbackHookHandler implements HookHandler {

    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {
//        String namespace = namespaceWrap.getNamespace();

        Method callbackMethod = (Method) namespaceWrap.getSource();
        Object result = executeCallbackMethod(context, callbackMethod);
        Callback callbackAnn = context.toAnnotation(Callback.class);
        if (callbackAnn.store()) {
            if (callbackAnn.storeType() == VarType.ROOT) {
                context.getContextVar().addRootVariable("$"+callbackMethod.getName(), result);
            } else {
                context.getContextVar().addVariable("$"+callbackMethod.getName(), result);
            }
        }
    }


    /**
     * 执行回调方法
     *
     * @param context        上下文
     * @param agreedOnMethod 约定方法
     * @return 执行结果
     */
    private Object executeCallbackMethod(HookContext context, Method agreedOnMethod) {
        try {
            return MethodUtils.invoke(null, agreedOnMethod, context.getMethodParamObject(agreedOnMethod));
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new CallbackMethodExecuteException(e, "Failed to execute callback method : {}", agreedOnMethod.toGenericString());
        }
    }

}
