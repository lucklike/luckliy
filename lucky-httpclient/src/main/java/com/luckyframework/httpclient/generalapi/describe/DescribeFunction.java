package com.luckyframework.httpclient.generalapi.describe;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.var.MethodRootVar;

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
     * 接口表述信息，被导入时会向SpEL运行时环境中注入变量{@link ApiDescribe}
     */
    @MethodRootVar
    private static final String $api = "#{#describe($mc$)}";

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
     * {@link #matchId(MethodContext, String)}方法的简写方法
     * <pre>
     *     eg:
     *     ``#{#$matchId('FUN-TOKEN')}``
     * </pre>
     *
     * @param apiId 目标ID
     * @return 调用matchId方法的表达式
     */
    public static String $matchId(String apiId) {
        return StringUtils.format("#{#matchId($mc$, '{}')}", apiId);
    }
}
