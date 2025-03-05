package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 支持{@link Condition}功能的的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractConditionalSelectionResponseConvert implements ResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        List<Condition> conditions = new ArrayList<>();
        MethodContext methodContext = context.getContext();

        // 获取方法和类上的@Condition注解
        List<Condition> classConditionList = methodContext.getParentContext().findNestCombinationAnnotations(Condition.class);
        List<Condition> methodConditionList = methodContext.findNestCombinationAnnotations(Condition.class);

        if (ContainerUtils.isNotEmptyCollection(classConditionList)) {
            conditions.addAll(classConditionList);
        }

        if (ContainerUtils.isNotEmptyCollection(methodConditionList)) {
            conditions.addAll(methodConditionList);
        }

        for (Condition condition : conditions) {

            // enable为false时跳过该注解
            String enable = condition.enable();
            if (StringUtils.hasText(enable) && !context.parseExpression(enable, boolean.class)) {
                continue;
            }

            // 计算断言表达式
            boolean assertion = context.parseExpression(condition.assertion(), boolean.class);
            if (assertion) {

                // 返回result表达式结果
                String result = condition.result();
                if (StringUtils.hasText(result)) {
                    return context.parseExpression(
                            result,
                            getResultType(context.getContext(), condition.returnType())
                    );
                }

                // 抛出exception表达式返回的异常
                String exception = condition.exception();
                if (StringUtils.hasText(exception)) {
                    throwException(context, exception);
                }

                throw new ConditionalSelectionException("The 'result' attribute and 'exception' attribute of the @Condition annotation cannot be empty at the same time. Please check the configuration: {}", condition);
            }
        }

        return doConvert(response, context);
    }

    /**
     * 将响应对象转换为方法返回值类型
     *
     * @param response      响应实例
     * @param methodContext 方法上下文
     * @param <T>           方法返回值类型
     * @return 方法返回值类型对应的对象
     */
    protected <T> T getMethodResult(Response response, MethodContext methodContext) {
        return response.getEntity(methodContext.getResultType());
    }

    /**
     * 主动抛出异常，如果表达式返回的是异常实例则直接抛出，否则抛出{@link ActivelyThrownException}异常
     *
     * @param context   转换注解上下文对象
     * @param exception 异常表达式
     * @throws Throwable 异常
     */
    protected void throwException(ConvertContext context, String exception) throws Throwable {
        Object exObj = context.parseExpression(exception);
        if (exObj instanceof Throwable) {
            throw (Throwable) exObj;
        }
        throw new ActivelyThrownException(String.valueOf(exObj));
    }

    /**
     * 获取返回类型
     *
     * @param methodContext 方法上下文
     * @param returnType   BranchClass
     * @return 获取返回类型
     */
    protected Type getResultType(MethodContext methodContext, Class<?> returnType) {
        Type realMethodReturnType = methodContext.getResultType();
        // 方法返回值类型与@Branch注解中配置的类型兼容时返回@Branch注解中配置的类型
        if (Objects.requireNonNull(ResolvableType.forType(realMethodReturnType).getRawClass()).isAssignableFrom(returnType)) {
            return returnType;
        }
        return realMethodReturnType;
    }

    /**
     * 将相应实体转化为指定类型的实体
     *
     * @param response 响应实体
     * @param context  转化器注解上下文
     * @param <T>      返回实体类型
     * @return 返回实体
     * @throws Exception 转换失败会抛出异常
     */
    protected abstract <T> T doConvert(Response response, ConvertContext context) throws Throwable;
}
