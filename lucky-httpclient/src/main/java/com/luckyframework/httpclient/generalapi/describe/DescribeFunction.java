package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;

import java.util.Objects;

/**
 * 获取接口描述信息相关的工具方法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/14 23:27
 */
public class DescribeFunction {

    /**
     * 获取接口描述信息实体类
     *
     * @param context 上下文
     * @return 接口描述信息实体类
     */
    public static ApiDescribe describe(Context context) {
        ApiDescribe apiDescribe = ApiDescribe.of(context.getSameAnnotationCombined(Describe.class));
        if (context instanceof MethodContext) {
            MethodContext mc = (MethodContext) context;
            String name = apiDescribe.getName();
            if (!StringUtils.hasText(name)) {
                apiDescribe.setName(mc.getCurrentAnnotatedElement().getName());
            }
            apiDescribe.setMethod(mc.getCurrentAnnotatedElement().getName());
            apiDescribe.setClazz(mc.getClassContext().getCurrentAnnotatedElement().getName());
        } else if (context instanceof MethodMetaContext) {
            MethodMetaContext mec = (MethodMetaContext) context;
            String name = apiDescribe.getName();
            if (!StringUtils.hasText(name)) {
                apiDescribe.setName(mec.getCurrentAnnotatedElement().getName());
            }
            apiDescribe.setMethod(mec.getCurrentAnnotatedElement().getName());
            apiDescribe.setClazz(mec.getParentContext().getCurrentAnnotatedElement().getName());
        }
        return apiDescribe;
    }

    /**
     * 匹配接口ID，如果匹配返回true，否则返回false
     *
     * @param context 上下文
     * @param apiId   目标ID
     * @return 是否匹配
     */
    public static boolean matchId(Context context, String apiId) {
        Describe describeAnn = context.getMergedAnnotation(Describe.class);
        if (describeAnn == null || !StringUtils.hasText(describeAnn.id())) {
            return false;
        }
        return Objects.equals(describeAnn.id(), apiId);
    }

    /**
     * 是否需要携带Token
     *
     * @param context 上下文对象
     * @return 当前API是否需要携带Token
     */
    public static boolean needToken(Context context) {
        if (context.isAnnotated(TokenApi.class)) {
            return false;
        }
        return describe(context).isNeedToken();
    }

}
