package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;


/**
 * 上下文SpEL表达式执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/8 11:10
 */
@FunctionalInterface
public interface ContextSpELExecution {

    <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter);

    default <T> T parseExpression(String expression, ResolvableType returnType) {
        return parseExpression(expression, returnType, pw -> {
        });
    }

    default <T> T parseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(returnType), setter);
    }

    default <T> T parseExpression(String expression, Class<T> returnType) {
        return parseExpression(expression, returnType, pw -> {
        });
    }

    default <T> T parseExpression(String expression, Type returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forType(returnType), setter);
    }

    default <T> T parseExpression(String expression, Type returnType) {
        return parseExpression(expression, returnType, pw -> {
        });
    }

    default <T> T parseExpression(String expression, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(Object.class), setter);
    }

    default <T> T parseExpression(String expression) {
        return parseExpression(expression, pw -> {
        });
    }

    @FunctionalInterface
    interface ParamWrapperSetter {

        void setting(MapRootParamWrapper paramWrapper);
    }
}
