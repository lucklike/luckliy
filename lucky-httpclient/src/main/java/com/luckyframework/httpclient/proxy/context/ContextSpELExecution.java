package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.ContextParamWrapper;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.function.Consumer;


/**
 * 上下文SpEL表达式执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/8 11:10
 */
@FunctionalInterface
public interface ContextSpELExecution {


    <T> T parseExpression(String expression, ResolvableType returnType, Consumer<ContextParamWrapper> paramSetter);

    default <T> T parseExpression(String expression, ResolvableType returnType) {
        return parseExpression(expression, returnType, arg -> {});
    }


    default <T> T parseExpression(String expression, Class<T> returnType, Consumer<ContextParamWrapper> paramSetter) {
        return parseExpression(expression, ResolvableType.forClass(returnType), paramSetter);
    }

    default <T> T parseExpression(String expression, Class<T> returnType) {
        return parseExpression(expression, ResolvableType.forClass(returnType), arg -> {});
    }

    default <T> T parseExpression(String expression, Type returnType, Consumer<ContextParamWrapper> paramSetter) {
        return parseExpression(expression, ResolvableType.forType(returnType), paramSetter);
    }

    default <T> T parseExpression(String expression, Type returnType) {
        return parseExpression(expression, ResolvableType.forType(returnType), arg -> {});
    }

    default <T> T parseExpression(String expression, Consumer<ContextParamWrapper> paramSetter) {
        return parseExpression(expression, ResolvableType.forClass(Object.class), paramSetter);
    }

    default <T> T parseExpression(String expression) {
        return parseExpression(expression, ResolvableType.forClass(Object.class));
    }

    default ContextParamWrapper initContextParamWrapper() {
        return new ContextParamWrapper();
    }
}
