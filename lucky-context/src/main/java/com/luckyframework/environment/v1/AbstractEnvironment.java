package com.luckyframework.environment.v1;

import com.luckyframework.conversion.ConversionUtils;
import org.springframework.core.ResolvableType;

import java.util.function.Function;

/**
 * 环境变量的基本抽象，抽取了一些公共的方法
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/18 下午8:22
 */
public abstract class AbstractEnvironment implements Environment {

    private final Function<Object,Object> ENV_FUNCTION = this::parsExpression;

    @Override
    public Object getProperty(String key, ResolvableType resolvableType) {
        Object value = getProperty(key);
        return ConversionUtils.conversion(value, resolvableType,ENV_FUNCTION);
    }

    @Override
    public Object parsSingleExpression(String single$Expression, ResolvableType resolvableType) {
        Object value = parsSingleExpression(single$Expression);
        return ConversionUtils.conversion(value, resolvableType,ENV_FUNCTION);
    }

    @Override
    public Object parsExpression(Object $Expression, ResolvableType resolvableType) {
        Object value = parsExpression($Expression);
        return ConversionUtils.conversion(value, resolvableType,ENV_FUNCTION);
    }
}