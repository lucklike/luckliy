package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.List;

/**
 * -@if表达式工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/3 00:49
 */
public abstract class IfExpressionUtils {

    /**
     * 过滤不符合@if表达式的参数，添加符合@if表达式的参数
     *
     * @param context          方法上下文
     * @param paramInfos       参数信息集合
     * @param dataArray        数据数组
     * @param separation       Key-Value分隔符
     * @param keyValuePostProcess 键值对消费者
     */
    public static void filterAndAdd(MethodContext context,
                                    List<ParamInfo> paramInfos,
                                    String[] dataArray,
                                    String separation,
                                    KeyValuePostProcess keyValuePostProcess) {

        if (ContainerUtils.isEmptyArray(dataArray)) {
            return;
        }

        for (String keyValueStr : dataArray) {

            // @if表达式计算
            keyValueStr = context.ifExpressionEvaluation(keyValueStr);
            if (!StringUtils.hasText(keyValueStr)) {
                continue;
            }

            // 验证表达式的正确性
            int index = keyValueStr.indexOf(separation);
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static parameter expression: '" + keyValueStr + "'. Please use the correct separator: '" + separation + "'");
            }

            // 分割表达式
            String nameExpression = keyValueStr.substring(0, index).trim();
            String valueExpression = keyValueStr.substring(index + separation.length()).trim();

            // 计算表达式结果
            Object nameExpressionValue = context.parseExpression(nameExpression);
            Object valueExpressionValue = context.parseExpression(valueExpression);

            ParamInfo paramInfo = keyValuePostProcess.process(keyValueStr, nameExpression, valueExpression, nameExpressionValue, valueExpressionValue);
            paramInfos.add(paramInfo);
        }
    }
}

