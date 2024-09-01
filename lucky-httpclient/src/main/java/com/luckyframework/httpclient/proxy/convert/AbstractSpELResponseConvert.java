package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELResponseConvert implements ResponseConvert {

    /**
     * 将响应对象转换为方法返回值类型
     *
     * @param response      响应实例
     * @param methodContext 方法上下文
     * @param <T>           方法返回值类型
     * @return 方法返回值类型对应的对象
     */
    protected <T> T getMethodResult(Response response, MethodContext methodContext) {
        return response.getEntity(methodContext.getRealMethodReturnType());
    }

    /**
     * 主动抛出异常，如果表达式返回的是异常实例则直接抛出，否则抛出{@link ActivelyThrownException}异常
     *
     * @param context   转换注解上下文对象
     * @param exception 异常表达式
     * @throws Throwable 异常
     */
    protected void throwException(ConvertContext context, String exception) throws Throwable {
        Object exObj = context.parseExpression(exception);
        if (exObj instanceof Throwable) {
            throw (Throwable) exObj;
        }
        throw new ActivelyThrownException(String.valueOf(exObj));
    }

}
