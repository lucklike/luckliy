package com.luckyframework.conversion;

import com.luckyframework.common.ExpressionEngine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;

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
			result = Integer.parseInt(str0);
		}
		else if(type == Double.class || type == double.class){
			result = Double.parseDouble(str0);
		}
		else if(type == Boolean.class || type == boolean.class){
			result = Boolean.parseBoolean(str0);
		}
		else if(type == Long.class || type == long.class){
			result = Long.parseLong(str0);
		}
		else if(type == Float.class || type == float.class){
			result = Float.parseFloat(str0);
		}
		else if(type == Byte.class || type == byte.class){
			result = Byte.parseByte(str0);
		}
		else if(type == Short.class || type == short.class){
			result = Short.parseShort(str0);
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
}
