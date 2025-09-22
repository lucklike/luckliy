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
    //                               Parse Expression
    //----------------------------------------------------------------------------------------

    <T> T parseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter);

    default <T> T parseExpression(String expression, ResolvableType returnType) {
        return parseExpression(expression, returnType, null);
    }

    default <T> T parseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(returnType), setter);
    }

    default <T> T parseExpression(String expression, Class<T> returnType) {
        return parseExpression(expression, returnType, null);
    }

    default <T> T parseExpression(String expression, Type returnType, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forType(returnType), setter);
    }

    default <T> T parseExpression(String expression, Type returnType) {
        return parseExpression(expression, returnType, null);
    }

    default <T> T parseExpression(String expression, ParamWrapperSetter setter) {
        return parseExpression(expression, ResolvableType.forClass(Object.class), setter);
    }

    default <T> T parseExpression(String expression) {
        return parseExpression(expression, (ParamWrapperSetter) null);
    }

    //----------------------------------------------------------------------------------------
    //                              Nest Parse Expression
    //----------------------------------------------------------------------------------------

    <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter, int nestCount);

    default <T> T nestParseExpression(String expression, ResolvableType returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    default <T> T nestParseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forClass(returnType), setter, nestCount);
    }

    default <T> T nestParseExpression(String expression, Class<T> returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    default <T> T nestParseExpression(String expression, Type returnType, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forType(returnType), setter, nestCount);
    }

    default <T> T nestParseExpression(String expression, Type returnType, int nestCount) {
        return nestParseExpression(expression, returnType, null, nestCount);
    }

    default <T> T nestParseExpression(String expression, ParamWrapperSetter setter, int nestCount) {
        return nestParseExpression(expression, ResolvableType.forClass(Object.class), setter, nestCount);
    }

    default <T> T nestParseExpression(String expression, int nestCount) {
        return nestParseExpression(expression, (ParamWrapperSetter) null, nestCount);
    }


    default <T> T nestParseExpression(String expression, ResolvableType returnType, ParamWrapperSetter setter){
        return nestParseExpression(expression, returnType, setter, Integer.MAX_VALUE);
    }

    default <T> T nestParseExpression(String expression, ResolvableType returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    default <T> T nestParseExpression(String expression, Class<T> returnType, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forClass(returnType), setter);
    }

    default <T> T nestParseExpression(String expression, Class<T> returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    default <T> T nestParseExpression(String expression, Type returnType, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forType(returnType), setter);
    }

    default <T> T nestParseExpression(String expression, Type returnType) {
        return nestParseExpression(expression, returnType, null);
    }

    default <T> T nestParseExpression(String expression, ParamWrapperSetter setter) {
        return nestParseExpression(expression, ResolvableType.forClass(Object.class), setter);
    }

    default <T> T nestParseExpression(String expression) {
        return nestParseExpression(expression, (ParamWrapperSetter) null);
    }

    //------------


    default <T> T nestParseExpression(NestExpression nestExpression, ResolvableType returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression.getExpression(), returnType, setter, nestExpression.getNestCount());
    }

    default <T> T nestParseExpression(NestExpression nestExpression, ResolvableType returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    default <T> T nestParseExpression(NestExpression nestExpression, Class<T> returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forClass(returnType), setter);
    }

    default <T> T nestParseExpression(NestExpression nestExpression,Class<T> returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    default <T> T nestParseExpression(NestExpression nestExpression,Type returnType, ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forType(returnType), setter);
    }

    default <T> T nestParseExpression(NestExpression nestExpression,Type returnType) {
        return nestParseExpression(nestExpression, returnType, null);
    }

    default <T> T nestParseExpression(NestExpression nestExpression,ParamWrapperSetter setter) {
        return nestParseExpression(nestExpression, ResolvableType.forClass(Object.class), setter);
    }

    default <T> T nestParseExpression(NestExpression nestExpression) {
        return nestParseExpression(nestExpression, (ParamWrapperSetter) null);
    }


}
