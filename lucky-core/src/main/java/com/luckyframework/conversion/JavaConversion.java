package com.luckyframework.conversion;

import com.luckyframework.common.ExpressionEngine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.function.Function;

/**
 * Java基础标量类型转换工具
 * <p>提供String到各类标量类型（基本类型及其包装类、枚举、BigInteger/BigDecimal、Date等）的转换，
 * 以及基本类型默认值的获取，是{@link ConversionUtils}基础转换能力的组成部分。</p>
 */
@SuppressWarnings("unchecked")
public class JavaConversion {

    /**
     * 将String类型的字符串转换为其他基本类型
     *
     * <p>能力边界说明：</p>
     * <p>1. 枚举类型按常量名精确匹配（区分大小写，匹配失败抛出异常）；</p>
     * <p>2. Boolean采用宽松解析：仅"true"（忽略大小写）返回true，其余任何输入均返回false；</p>
     * <p>3. 整型（Integer/Long/Byte/Short）支持小数兼容：整数解析失败时按Double解析后截断；</p>
     * <p>4. char取字符串首字符（空字符串会抛出异常）；Date及其子类使用{@link DateParseUtil}解析；</p>
     * <p>5. 未支持的目标类型原样返回字符串（表达式计算开启时为计算结果）。</p>
     *
     * @param str         字符串类型的待转换值
     * @param type        目标类型
     * @param isCalculate 是否先进行SpEL表达式计算
     * @param <T>         目标类型
     * @return 转换后的值
     */
    public static <T> T fromString(String str, Class<T> type, boolean isCalculate) {
        if (str == null) {
            return null;
        }
        String str0 = isCalculate ? ExpressionEngine.calculate(str) : str;
        Object result = str0;

        if (type.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
            return (T) Enum.valueOf(enumClass, str0);
        }

        if (type == Integer.class || type == int.class) {
            result = doubleCompatibleProcess(str, Integer::parseInt, Double::intValue);
        } else if (type == Double.class || type == double.class) {
            result = Double.parseDouble(str0);
        } else if (type == Boolean.class || type == boolean.class) {
            // 宽松解析：仅"true"（忽略大小写）返回true，其余任何输入均返回false，不抛出异常
            result = Boolean.parseBoolean(str0);
        } else if (type == Long.class || type == long.class) {
            result = doubleCompatibleProcess(str, Long::parseLong, Double::longValue);
        } else if (type == Float.class || type == float.class) {
            result = doubleCompatibleProcess(str, Float::parseFloat, Double::floatValue);
        } else if (type == Byte.class || type == byte.class) {
            result = doubleCompatibleProcess(str, Byte::parseByte, Double::byteValue);
        } else if (type == Short.class || type == short.class) {
            result = doubleCompatibleProcess(str, Short::parseShort, Double::shortValue);
        } else if (type == char.class) {
            result = str0.charAt(0);
        } else if (type == char[].class) {
            result = str0.toCharArray();
        } else if (type == BigInteger.class) {
            result = new BigInteger(str0);
        } else if (type == BigDecimal.class) {
            result = new BigDecimal(str0);
        } else if (Date.class.isAssignableFrom(type)) {
            result = DateParseUtil.parse(str0);
        }
        return (T) result;
    }

    /**
     * 获取基本类型的默认值
     * <p>仅支持8种基本类型（byte/short/int/long/float/double/char/boolean），其他类型一律返回null。</p>
     *
     * @param type 类型
     * @return 默认值
     */
    public static Object getTypeDefaultValue(Class<?> type) {
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\u0000';
        if (type == boolean.class) return false;
        return null;
    }

    /**
     * 整数的小数兼容处理
     * <p>先执行原始转换逻辑，失败时按Double解析字符串再使用降级函数转换（如"1.5"转换为1）。</p>
     *
     * @param numStr        数字字符串
     * @param tryFunction   原始转换逻辑
     * @param catchFunction Double兼容处理逻辑
     * @param <T>           返回值泛型
     * @return 返回值
     */
    private static <T> T doubleCompatibleProcess(String numStr, Function<String, T> tryFunction, Function<Double, T> catchFunction) {
        try {
            return tryFunction.apply(numStr);
        } catch (Exception e) {
            return catchFunction.apply(Double.parseDouble(numStr));
        }
    }
}
