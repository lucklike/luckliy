package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.Branch;
import com.luckyframework.httpclient.proxy.annotations.ConditionalSelection;
import com.luckyframework.httpclient.proxy.annotations.VoidConditionalSelection;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpELUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 条件转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:28
 */
public class VoidConditionalSelectionResponseConvert extends AbstractSpELVoidResponseConvert {
    @Override
    public <T> T convert(VoidResponse voidResponse, ConvertContext context) throws Exception {
        VoidConditionalSelection conditionalSelectionAnn = context.toAnnotation(VoidConditionalSelection.class);
        // 获取配置
        Branch[] branches = conditionalSelectionAnn.branch();
        Consumer<SpELUtils.ExtraSpELArgs> spElArgConsumer = getSpElArgConsumer(voidResponse);

        for (Branch branch : branches) {
            boolean assertion = context.parseExpression(branch.assertion(), boolean.class, spElArgConsumer);
            if (assertion) {
                return context.parseExpression(
                        branch.result(),
                        getReturnType(context.getContext(), branch.returnType()),
                        spElArgConsumer
                );
            }
        }

        return getDefaultValue(voidResponse, context);
    }

    private Type getReturnType(MethodContext methodContext, Class<?> branchClass) {
        Type realMethodReturnType = methodContext.getRealMethodReturnType();
        // 方法返回值类型与@Branch注解中配置的类型兼容时返回@Branch注解中配置的类型
        if (Objects.requireNonNull(ResolvableType.forType(realMethodReturnType).getRawClass()).isAssignableFrom(branchClass)) {
            return branchClass;
        }
        return realMethodReturnType;
    }
}
