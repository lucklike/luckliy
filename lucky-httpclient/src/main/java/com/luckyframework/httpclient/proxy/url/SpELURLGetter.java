package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.ServerAddressMeta;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;

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
        return getUrl(context.getContext(), httpRequest.url(), httpRequest.func());
    }

    @Override
    public String getDomainName(DomainNameContext context) {
        ServerAddress domainAnn = context.toAnnotation(ServerAddress.class);
        String url = getUrl(context.getContext(), domainAnn.url(), domainAnn.func());
        String path = context.parseExpression(domainAnn.path(), String.class);
        return StringUtils.joinUrlPath(url, path);
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
            return autoInjectParamExecuteUrlFunction(context, urlFun);
        }
        return ServerAddressMeta.EMPTY;
    }


    /**
     * 自动注入参数后执行URL获取函数
     *
     * @param context 方法上下文
     * @param urlFun  URL函数
     * @return URL
     */
    public static String autoInjectParamExecuteUrlFunction(MethodContext context, String urlFun) {
        return (String) context.autoInjectParamExecuteFunction(
                urlFun,
                ResolvableType.forClass(String.class),
                () -> new UrlGetException("URL function '{}' cannot be found", FontUtil.getYellowUnderline(urlFun)),
                e -> new UrlGetException(e, "URL function '{}' failed to obtain", FontUtil.getYellowUnderline(urlFun)),
                fe -> new UrlGetException(fe.getThrowable(), "Url function run exception: ['{}']['{}']", FontUtil.getYellowStr(urlFun), FontUtil.getRedUnderline(MethodUtils.getLocation(fe.getMethod()))),
                fe -> new ActivelyThrownException(fe.getThrowable().getCause())
        );
    }

}
