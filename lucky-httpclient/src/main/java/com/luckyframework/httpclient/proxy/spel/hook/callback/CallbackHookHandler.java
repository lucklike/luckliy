package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.logging.FontUtil;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

/**
 * 回调方法Hook的处理器
 */
public class CallbackHookHandler extends AbstractValueStoreHookHandler {

    @Override
    protected Object useHookReturnResult(HookContext context, NamespaceWrap namespaceWrap) {
        return executeCallbackMethod(context, namespaceWrap);
    }

    @Override
    protected String geDefStoreName(NamespaceWrap namespaceWrap) {
        Method method = (Method) namespaceWrap.getSource();
        return "$" + method.getName();
    }

    @Override
    protected String getStoreDesc(NamespaceWrap namespaceWrap) {
        return StringUtils.format("@Callback[{}]", FontUtil.getYellowUnderline(MethodUtils.getLocation((Method) namespaceWrap.getSource())));
    }

    /**
     * 执行回调方法
     *
     * @param context       上下文
     * @param namespaceWrap 命名空间包装类
     * @return 执行结果
     */
    private Object executeCallbackMethod(HookContext context, NamespaceWrap namespaceWrap) {
        try {
            Method callbackMethod = (Method) namespaceWrap.getSource();
            return MethodUtils.invoke(null, callbackMethod, context.getMethodParamObject(callbackMethod));
        } catch (LuckyInvocationTargetException e) {
            throw new ActivelyThrownException(e.getCause());
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new CallbackMethodExecuteException(e, "Callback function running exception: '{}'", getStoreDesc(namespaceWrap));
        }
    }

}
