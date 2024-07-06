package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 表达式解析引擎
 * @author fk-7075
 *
 */
public abstract class ExpressionEngine {

	private final static ScriptEngineManager manager = new ScriptEngineManager();
	
	public static String calculate(String expression) {
		return calculate(expression, String.class);
    }

	public static <T> T calculate(String expression, Class<T> type) {

        try {
			ScriptEngine engine = manager.getEngineByName("javascript");
            Object eval = engine.eval(expression);
			return ConversionUtils.conversion(eval, type);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
	}
}
