package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.Binary;
import com.luckyframework.httpclient.proxy.annotations.MultipartFormData;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.reflect.ClassUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MultipartForm静态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/3 00:26
 */
public class MultipartFormStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        MultipartFormData mfdAnn = context.toAnnotation(MultipartFormData.class);

        // condition条件检验
        String condition = mfdAnn.condition();
        if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
            return Collections.emptyList();
        }

        List<ParamInfo> paramInfos = new ArrayList<>();

        // Key-Value分隔符
        String separation = mfdAnn.separator();

        // 添加Txt格式的数据
        IfExpressionUtils.filterAndAdd(
                context.getContext(),
                paramInfos,
                mfdAnn.txt(),
                separation,
                (e, k, v, kv, vv) -> new ParamInfo(kv, vv)
        );

        // 添加文件格式的数据
        IfExpressionUtils.filterAndAdd(
                context.getContext(),
                paramInfos,
                mfdAnn.file(),
                separation,
                (e, k, v, kv, vv) -> {
                    if (HttpExecutor.isResourceParam(vv)) {
                        return new ParamInfo(kv, vv);
                    }
                    throw new IllegalArgumentException(StringUtils.format("The value '{}' corresponding to the key named '{}' in the Map cannot be converted to HttpFile type.", ClassUtils.getClassSimpleName(vv), kv));
                }
        );

        // 添加二进制格式的数据
        for (Binary binary : mfdAnn.binary()) {
            String fileCondition = binary.condition();
            if (StringUtils.hasText(fileCondition) && !context.parseExpression(fileCondition, boolean.class)) {
                continue;
            }

            String name = binary.name();
        }

        return paramInfos;
    }
}
