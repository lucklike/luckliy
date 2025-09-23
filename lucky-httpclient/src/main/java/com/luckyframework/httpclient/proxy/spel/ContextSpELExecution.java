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
public interface ContextSpELExecution {

    //----------------------------------------------------------------------------------------
    //                              parse expression auto model
    //----------------------------------------------------------------------------------------

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter);

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, ResolvableType returnType) {
        return parseExpression(expression, returnType, null);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(returnType), setter);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, Class<T> returnType) {
        return parseExpression(expression, returnType, null);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, Type returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forType(returnType), setter);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, Type returnType) {
        return parseExpression(expression, returnType, null);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(Object.class), setter);
    }

    /**
     * 执行SpEL表达式并返回指定类型的结果
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value SpELConvert#DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}                 ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``            ->  表示需要使用嵌套解析}
     * {@code ``@max(n): #{expression}``    -> 表示需要嵌套解析，并且限定最大嵌套解析次数为 n}
     * </pre>
     *
     * @param expression SpEL表达式
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T parseExpression(String expression) {
        return parseExpression(expression, (ParamWrapperSetter) null);
    }

    //----------------------------------------------------------------------------------------
    //                nest parse expression specify the number of parsing times
    //----------------------------------------------------------------------------------------

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter, int nestCount);

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, ResolvableType returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forClass(returnType), setter, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Class<T> returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Type returnType, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forType(returnType), setter, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Type returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param setter     上下文参数设置器
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forClass(Object.class), setter, nestCount);
    }

    /**
     * 使用指定嵌套解析次数的嵌套解析方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param nestCount  最大嵌套解析次数
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, int nestCount) {
        return nestParseExpression(expression, (ParamWrapperSetter) null, nestCount);
    }

    //----------------------------------------------------------------------------------------
    //               nest parse expression use the maximum number of parsing times
    //----------------------------------------------------------------------------------------


    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter) {
        return nestParseExpression(expression, returnType, setter, Integer.MAX_VALUE);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, ResolvableType returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forClass(returnType), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Class<T> returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Type returnType, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forType(returnType), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param returnType 预期的返回值类型
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, Type returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param setter     上下文参数设置器
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forClass(Object.class), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param expression SpEL表达式
     * @param <T>        返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(String expression) {
        return nestParseExpression(expression, (ParamWrapperSetter) null);
    }

    //----------------------------------------------------------------------------------------
    //                  nest parse expression use nested expressions
    //----------------------------------------------------------------------------------------


    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param setter         上下文参数设置器
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, ResolvableType returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression.getExpression(), returnType, setter, nestExpression.getNestCount());
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, ResolvableType returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param setter         上下文参数设置器
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, Class<T> returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forClass(returnType), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, Class<T> returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param setter         上下文参数设置器
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, Type returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forType(returnType), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param returnType     预期的返回值类型
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, Type returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param setter         上下文参数设置器
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forClass(Object.class), setter);
    }

    /**
     * 使用嵌套解析的方式执行SpEL表达式并返回指定类型的结果
     *
     * @param nestExpression SpEL嵌套表达式
     * @param <T>            返回值类型
     * @return SpEL表达式的执行结果
     */
    default <T> T nestParseExpression(NestExpression nestExpression) {
        return nestParseExpression(nestExpression, (ParamWrapperSetter) null);
    }


}
