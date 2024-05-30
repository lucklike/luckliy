package com.luckyframework.common;

import com.luckyframework.spel.SpELRuntime;
import org.springframework.expression.Expression;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 表达式解析引擎
 * @author fk-7075
 *
 */
public abstract class ExpressionEngine {

	private final static SpELRuntime spelRuntime = new SpELRuntime();
	
	public static String calculate(String expression) {
		return calculate(expression, String.class);
    }

	public static <T> T calculate(String expression, Class<T> type) {
		return spelRuntime.getValueForType(expression, type);
	}

	public static void main(String[] args) throws ScriptException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");
		Object eval = engine.eval("String.valueOf(10324736*10000)");
		System.out.println(eval);
//		System.out.println(calculate("103284736*100 + 'm'", String.class));
	}
}
