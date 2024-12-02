package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;

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
     * @param context 方法上下文
     * @return 接口描述信息实体类
     */
    public static ApiDescribe describe(MethodContext context) {
        return ApiDescribe.of(context.getSameAnnotationCombined(Describe.class));
    }

    /**
     * 匹配接口ID，如果匹配返回true，否则返回false
     *
     * @param context 方法上下文
     * @param apiId   目标ID
     * @return 是否匹配
     */
    public static boolean matchId(MethodContext context, String apiId) {
        Describe describeAnn = context.getMergedAnnotation(Describe.class);
        if (describeAnn == null || !StringUtils.hasText(describeAnn.id())) {
            return false;
        }
        return Objects.equals(describeAnn.id(), apiId);
    }

    /**
     * 是否为TokenApi
     *
     * @param context 上下文对象
     * @return 当前API是否为TokenApi
     */
    public static boolean isTokenApi(MethodContext context) {
        return describe(context).isTokenApi();
    }

    /**
     * 是否为非TokenApi
     *
     * @param context 上下文对象
     * @return 当前API是否为非TokenApi
     */
    public static boolean nonTokenApi(MethodContext context) {
        return !isTokenApi(context);
    }
}
