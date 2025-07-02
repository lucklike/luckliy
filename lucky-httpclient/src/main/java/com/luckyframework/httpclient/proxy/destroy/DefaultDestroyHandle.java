package com.luckyframework.httpclient.proxy.destroy;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.context.MethodWrap;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

/**
 * 默认的销毁处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 01:28
 */
public class DefaultDestroyHandle implements DestroyHandle {

    /**
     * 约定的转换方法后缀
     */
    public final String DESTROY_FUNCTION_SUFFIX = "$Destroy";

    @Override
    public void destroy(DestroyContext context) throws Throwable {
        Destroy destroyAnn = context.toAnnotation(Destroy.class);

        // 如果指定了SpEL表达式则使用该表达式
        String value = destroyAnn.value();
        if (StringUtils.hasText(value)) {
            context.parseExpression(value);
            return;
        }

        // 使用指定的销毁方法进行资源销毁
        Method destroyFuncMethod = getDestroyFuncMethod(context, destroyAnn.func());
        if (destroyFuncMethod != null) {
            executeDestroyFuncMethod(context, destroyFuncMethod);
        }

    }

    /**
     * 获取用于销毁处理的SpEL函数
     *
     * @param context  上下文对象
     * @param funcName 指定的转换函数
     * @return 对应的Method对象
     */
    private Method getDestroyFuncMethod(DestroyContext context, String funcName) {

        // 是否指定了处理函数
        boolean isAppoint = StringUtils.hasText(funcName);

        // 获取指定的Destroy函数名，如果不存在则使用约定的Destroy函数名
        MethodWrap destroyFuncMethodWrap = context.getSpELFuncOrDefault(funcName, DESTROY_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (destroyFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Destroy SpEL function named '{}' is not found in context.", funcName);
            }
            return null;
        }

        return destroyFuncMethodWrap.getMethod();
    }

    /**
     * 执行响应转换方法
     *
     * @param context           销毁上下文
     * @param convertFuncMethod 响应转换方法
     */
    private void executeDestroyFuncMethod(DestroyContext context, Method convertFuncMethod) throws Throwable {
        try {
            context.invokeMethod(null, convertFuncMethod);
        }
        catch (LuckyInvocationTargetException e) {
            throw e.getCause();
        }
        catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new SpELFunctionExecuteException(e, "Response Convert method run exception: ['{}']", MethodUtils.getLocation(convertFuncMethod));
        }
    }
}
