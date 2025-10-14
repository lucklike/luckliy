package com.luckyframework.httpclient.proxy.spel;

/**
 * 嵌套表达式
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/9/23 00:53
 */
public class NestExpression {
    /**
     * 嵌套解析次数
     */
    private final int nestCount;

    /**
     * SpEL 表达式
     */
    private final String expression;

    private NestExpression(int nestCount, String expression) {
        this.nestCount = nestCount;
        this.expression = expression;
    }

    public static NestExpression not(String expression) {
        return new NestExpression(0, expression);
    }

    public static NestExpression infinite(String expression) {
        return new NestExpression(Integer.MAX_VALUE, expression);
    }

    public static NestExpression of(String expression, int nestCount) {
        return new NestExpression(nestCount, expression);
    }

    public int getNestCount() {
        return nestCount;
    }

    public String getExpression() {
        return expression;
    }

    public boolean needsNest() {
        return nestCount > 0;
    }

    public boolean isInfinite() {
        return nestCount == Integer.MAX_VALUE;
    }
}
