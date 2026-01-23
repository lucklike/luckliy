package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.meta.RequestMethod;

/**
 * URL地址获取器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 22:43
 */
@FunctionalInterface
public interface URLGetter {

    /**
     * 获取URL信息
     *
     * @param context                 注解上下文
     * @param enableAutoUrlDerivation 是否开启方法名自动推导
     * @return URL信息
     * @throws Exception 计算过程中可能出现的一异常
     */
    default String getUrl(HttpRequestContext context, boolean enableAutoUrlDerivation) throws Exception {
        String url = getUrl(context);

        // 存在url或者未开启URL自动推导时直接返回
        if (StringUtils.hasText(url) || !enableAutoUrlDerivation) {
            return url;
        }

        // 使用方法名自动推导
        return methodNameToUrl(context.getContext().getCurrentAnnotatedElement().getName()).getOne();
    }


    /**
     * 将方法名转为URL信息
     *
     * @param methodName 方法名
     * @return URL
     */
    static TempPair<String, RequestMethod> methodNameToUrl(String methodName) {
        final String METHOD_FLAG = "$$", PATH_FLAG = "$", PATH_SEPARATION = "/";
        int i = methodName.indexOf(METHOD_FLAG);
        RequestMethod method = i == -1 ? null : RequestMethod.valueOf(methodName.substring(0, i).toUpperCase());
        String path = (i == -1 ? methodName : methodName.substring(i + METHOD_FLAG.length())).replace(PATH_FLAG, PATH_SEPARATION);
        return TempPair.of(path, method);
    }

    /**
     * 获取URL
     *
     * @param context 注解上下文
     * @return URL
     * @throws Exception 计算过程中可能出现的一异常
     */
    String getUrl(HttpRequestContext context) throws Exception;
}
