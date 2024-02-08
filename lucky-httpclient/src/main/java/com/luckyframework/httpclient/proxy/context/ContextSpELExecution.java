package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.spel.SpELUtils;
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

    <T> T parseExpression(String expression, ResolvableType returnType, Consumer<SpELUtils.ExtraSpELArgs> argSetter);

    default <T> T parseExpression(String expression, ResolvableType returnType) {
        return parseExpression(expression, returnType, arg -> {});
    }


    default <T> T parseExpression(String expression, Class<T> returnType, Consumer<SpELUtils.ExtraSpELArgs> argSetter) {
        return parseExpression(expression, ResolvableType.forClass(returnType), argSetter);
    }

    default <T> T parseExpression(String expression, Class<T> returnType) {
        return parseExpression(expression, ResolvableType.forClass(returnType), arg -> {});
    }

    default <T> T parseExpression(String expression, Type returnType, Consumer<SpELUtils.ExtraSpELArgs> argSetter) {
        return parseExpression(expression, ResolvableType.forType(returnType), argSetter);
    }

    default <T> T parseExpression(String expression, Type returnType) {
        return parseExpression(expression, ResolvableType.forType(returnType), arg -> {});
    }

    default <T> T parseExpression(String expression, Consumer<SpELUtils.ExtraSpELArgs> argSetter) {
        return parseExpression(expression, ResolvableType.forClass(Object.class), argSetter);
    }

    default <T> T parseExpression(String expression) {
        return parseExpression(expression, ResolvableType.forClass(Object.class));
    }


    default SpELUtils.ExtraSpELArgs getSpELArgs() {
        return SpELUtils.createSpELArgs().extractSpELEnv();
    }
}
