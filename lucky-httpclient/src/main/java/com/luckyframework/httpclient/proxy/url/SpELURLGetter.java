package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.annotations.DomainName;
import com.luckyframework.httpclient.proxy.annotations.DomainNameMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

/**
 * 支持SpEL表达式的域名获取器，SpEL表达式部分需要写在#{}中
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELURLGetter implements URLGetter, DomainNameGetter {


    @Override
    public String getUrl(HttpRequestContext context) {
        HttpRequest httpRequest = context.toAnnotation(HttpRequest.class);
        return getUrl(context.getContext(), httpRequest.url(), httpRequest.fun());
    }

    @Override
    public String getDomainName(DomainNameContext context) {
        DomainName domainAnn = context.toAnnotation(DomainName.class);
        return getUrl(context.getContext(), domainAnn.value(), domainAnn.fun());
    }

    /**
     * 获取URL
     *
     * @param context    方法上下文
     * @param expression URL表达式
     * @param urlFun     URL获取函数
     * @return URL
     */
    public static String getUrl(MethodContext context, String expression, String urlFun) {
        // 优先使用表达式解析
        if (StringUtils.hasText(expression)) {
            return context.parseExpression(expression, String.class);
        }
        // 从函数解析
        if (StringUtils.hasText(urlFun)) {
            return executeFuncMethod(context, findUrlMethod(context, urlFun));
        }
        return DomainNameMeta.EMPTY;
    }

    /**
     * 通过URL函数获取对应的方法
     *
     * @param context 方法上下文
     * @param urlFun  URL函数
     * @return 对应的方法对象
     */
    public static Method findUrlMethod(MethodContext context, String urlFun) {
        Method fun = context.getVar(urlFun, Method.class);
        if (fun != null) {
            return fun;
        }
        throw new UrlGetException("URL function {} cannot be found", urlFun);
    }

    /**
     * 执行URL获取方法
     *
     * @param context   方法上下文
     * @param funMethod 方法
     * @return 执行结果
     */
    public static String executeFuncMethod(MethodContext context, Method funMethod) {
        try {
            return (String) context.invokeMethod(null, funMethod);
        } catch (LuckyInvocationTargetException e) {
            throw new ActivelyThrownException(e.getCause());
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new UrlGetException(e, "Url function run exception: ['{}']", FontUtil.getBlueUnderline(MethodUtils.getLocation(funMethod)));
        }
    }

}
