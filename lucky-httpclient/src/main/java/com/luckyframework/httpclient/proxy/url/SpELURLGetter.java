package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.ServerAddressMeta;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.lang.NonNull;

/**
 * 支持SpEL表达式的域名获取器，SpEL表达式部分需要写在#{}中
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 10:14
 */
public class SpELURLGetter implements PathGetter, BaseURLGetter {

    @Override
    public String getPath(HttpRequestContext context) {
        HttpRequest httpRequest = context.toAnnotation(HttpRequest.class);
        return analysisSpELAndFunc(context.getContext(), httpRequest.url(), httpRequest.func());
    }

    @Override
    public String getBaseUrl(DomainNameContext context) {
        ServerAddress domainAnn = context.toAnnotation(ServerAddress.class);
        String url = analysisSpELAndFunc(context.getContext(), domainAnn.url(), domainAnn.urlFunc());
        String path = analysisSpELAndFunc(context.getContext(), domainAnn.path(), domainAnn.pathFunc());
        return StringUtils.joinUrlPath(url, path);
    }


    /**
     * 解析 SpEL 表达式和函数
     *
     * @param context 上下文对象
     * @param spelEx  SpEL 表达式
     * @param funcEx  函数表达式
     * @return 运行结果
     */
    @NonNull
    public static String analysisSpELAndFunc(MethodContext context, String spelEx, String funcEx) {
        return context.autoExecuteSpELOrFunc(spelEx, funcEx, String.class, StringUtils::hasText, ServerAddressMeta.EMPTY);
    }
}
