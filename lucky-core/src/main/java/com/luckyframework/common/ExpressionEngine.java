package com.luckyframework.common;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 表达式解析引擎
 * @author fk-7075
 *
 */
public abstract class ExpressionEngine {

	private final static ExpressionParser parser = new SpelExpressionParser();
	
	public static String calculate(String expression) {
		return calculate(expression, String.class);
    }

	public static <T> T calculate(String expression, Class<T> type) {
		Expression exp = parser.parseExpression(expression);
		return exp.getValue(type);
	}

	public static void main(String[] args) {
		System.out.println(calculate("1024*2+'M'"));
	}
}
