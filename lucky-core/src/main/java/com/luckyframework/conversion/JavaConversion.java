package com.luckyframework.conversion;

import com.luckyframework.common.ExpressionEngine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class JavaConversion {

	public static <T> T fromString(String str, Class<T> type, boolean isCalculate){
		if(str == null){
			return null;
		}
		String str0 = isCalculate ? ExpressionEngine.calculate(str) : str;
		Object result = str0;

		if(type.isEnum()){
			Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
			return (T) Enum.valueOf(enumClass, str0);
		}

		if (type == Integer.class || type == int.class){
			result = doubleCompatibleProcess(str, Integer::parseInt, Double::intValue);
		}
		else if(type == Double.class || type == double.class){
			result = Double.parseDouble(str0);
		}
		else if(type == Boolean.class || type == boolean.class){
			result = Boolean.parseBoolean(str0);
		}
		else if(type == Long.class || type == long.class){
			result = doubleCompatibleProcess(str, Long::parseLong, Double::longValue);
		}
		else if(type == Float.class || type == float.class){
			result = doubleCompatibleProcess(str, Float::parseFloat, Double::floatValue);
		}
		else if(type == Byte.class || type == byte.class){
			result = doubleCompatibleProcess(str, Byte::parseByte, Double::byteValue);
		}
		else if(type == Short.class || type == short.class){
			result = doubleCompatibleProcess(str, Short::parseShort, Double::shortValue);
		}
		else if(type == char.class){
			result = str0.charAt(0);
		}
		else if(type == char[].class){
			result = str0.toCharArray();
		}
		else if(type == BigInteger.class){
			result = new BigInteger(str0);
		}
		else if(type == BigDecimal.class){
			result = new BigDecimal(str0);
		}
		else if(Date.class.isAssignableFrom(type)){
			result = Timestamp.valueOf(str0);
		}
		return (T) result;
	}

	/**
	 * 整数的小数兼容处理
     *
	 * @param numStr 数字字符串
	 * @param tryFunction 原始转换逻辑
	 * @param catchFunction Double兼容处理逻辑
	 * @return 返回值
	 * @param <T> 返回值泛型
	 */
	private static <T> T doubleCompatibleProcess(String numStr, Function<String, T> tryFunction, Function<Double, T> catchFunction) {
		try {
			return tryFunction.apply(numStr);
		} catch (Exception e) {
			return catchFunction.apply(Double.parseDouble(numStr));
		}
	}
}
