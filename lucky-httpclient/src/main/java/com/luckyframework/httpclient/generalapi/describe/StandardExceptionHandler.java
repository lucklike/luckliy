package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.handle.HttpExceptionHandle;
import com.luckyframework.httpclient.proxy.interceptor.ErrStatusHandleInterceptor;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.InterceptorContext;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标准异常处理器
 * <pre>
 *     注意：
 *     被该注解标注的类必须满足如下两个条件
 *      1.实现{@link ErrStatusHandleInterceptor}接口，并重写其中的四个方法：
 *          {@link ErrStatusHandleInterceptor#isErrStatus(int) isErrStatus(int)}
 *          {@link ErrStatusHandleInterceptor#handleErrStatus(Response, InterceptorContext) handleErrStatus(Response, InterceptorContext)}
 *          {@link ErrStatusHandleInterceptor#isErrRespCode(Response, InterceptorContext) isErrRespCode(Response, InterceptorContext)}
 *          {@link ErrStatusHandleInterceptor#handleErrRespCode(Response, InterceptorContext) handleErrRespCode(Response, InterceptorContext)}
 *      2.提供一个和{@link HttpExceptionHandle#exceptionHandler(MethodContext, Request, Throwable)}方法签名一样的异常处理方法
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:14
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport(DescribeFunction.class)
@ExceptionHandle("#{$this$.exceptionHandler($mc$, $req$, $throwable$)}")
@InterceptorRegister(expression = "#{$this$}", priority = PriorityConstant.ERR_STATUS_HANDLE_PRIORITY)
public @interface StandardExceptionHandler {

}
