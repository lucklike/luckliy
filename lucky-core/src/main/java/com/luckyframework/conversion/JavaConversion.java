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
     * <p>3. 整型（Integer/Long/Byte/Short）解析顺序：原始字符串 -> 表达式计算结果 -> Double降级截断（如"1.5"转换为1），
     * 降级值超出目标类型取值范围时会抛出异常；</p>
     * <p>4. char取字符串首字符（空字符串会抛出异常）；Date及其子类使用{@link DateParseUtil}解析，
     * java.sql.Date/java.sql.Time/java.sql.Timestamp会适配为目标类型，无法识别的时间字符串会抛出异常；</p>
     * <p>5. 未支持的目标类型原样返回字符串（表达式计算开启时为计算结果）。</p>
     *
     * @param str         字符串类型的待转换值
     * @param type        目标类型
     * @param isCalculate 是否开启表达式计算（原始字符串无法直接解析时尝试使用计算结果）
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
            result = doubleCompatibleProcess(str, str0, type, Integer::parseInt, Double::intValue);
        } else if (type == Double.class || type == double.class) {
            result = Double.parseDouble(str0);
        } else if (type == Boolean.class || type == boolean.class) {
            // 宽松解析：仅"true"（忽略大小写）返回true，其余任何输入均返回false，不抛出异常
            result = Boolean.parseBoolean(str0);
        } else if (type == Long.class || type == long.class) {
            result = doubleCompatibleProcess(str, str0, type, Long::parseLong, Double::longValue);
        } else if (type == Float.class || type == float.class) {
            result = doubleCompatibleProcess(str, str0, type, Float::parseFloat, Double::floatValue);
        } else if (type == Byte.class || type == byte.class) {
            result = doubleCompatibleProcess(str, str0, type, Byte::parseByte, Double::byteValue);
        } else if (type == Short.class || type == short.class) {
            result = doubleCompatibleProcess(str, str0, type, Short::parseShort, Double::shortValue);
        } else if (type == char.class) {
            result = str0.charAt(0);
        } else if (type == char[].class) {
            result = str0.toCharArray();
        } else if (type == BigInteger.class) {
            result = new BigInteger(str0);
        } else if (type == BigDecimal.class) {
            result = new BigDecimal(str0);
        } else if (Date.class.isAssignableFrom(type)) {
            Date date = DateParseUtil.parse(str0);
            if (date == null) {
                throw new IllegalArgumentException("Cannot parse the value ['" + str0 + "'] to date type '" + type.getName() + "'");
            }
            result = adaptDateType(date, type);
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
     * <p>解析顺序：原始字符串精确解析 -> 表达式计算结果精确解析 -> 按Double降级解析再截断（如"1.5"转换为1）；
     * 降级值超出目标类型取值范围时抛出异常，不会静默截断或饱和。</p>
     *
     * @param numStr        数字字符串（原始输入）
     * @param calculatedStr 表达式计算结果（未开启表达式计算时与原始字符串相同）
     * @param targetType    目标类型，用于降级时的值域校验
     * @param tryFunction   原始转换逻辑
     * @param catchFunction Double兼容处理逻辑
     * @param <T>           返回值泛型
     * @return 返回值
     */
    private static <T> T doubleCompatibleProcess(String numStr, String calculatedStr, Class<?> targetType,
                                                 Function<String, T> tryFunction, Function<Double, T> catchFunction) {
        try {
            return tryFunction.apply(numStr);
        } catch (Exception ignored) {
            // 原始字符串无法直接解析，尝试使用表达式计算结果
        }
        try {
            return tryFunction.apply(calculatedStr);
        } catch (Exception ignored) {
            // 表达式计算结果也无法直接解析，降级为Double解析
        }
        double compatibleValue;
        try {
            compatibleValue = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            compatibleValue = Double.parseDouble(calculatedStr);
        }
        if (isOutOfRange(compatibleValue, targetType)) {
            throw new NumberFormatException("Value out of range for type '" + targetType.getName() + "': '" + numStr + "'");
        }
        return catchFunction.apply(compatibleValue);
    }

    /**
     * 判断降级后的Double值是否超出目标类型的取值范围（NaN视为超出范围）
     *
     * @param value      降级后的Double值
     * @param targetType 目标类型
     * @return 是否超出取值范围
     */
    private static boolean isOutOfRange(double value, Class<?> targetType) {
        if (Double.isNaN(value)) {
            return true;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE;
        }
        if (targetType == Long.class || targetType == long.class) {
            return value < Long.MIN_VALUE || value > Long.MAX_VALUE;
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return value < Byte.MIN_VALUE || value > Byte.MAX_VALUE;
        }
        if (targetType == Short.class || targetType == short.class) {
            return value < Short.MIN_VALUE || value > Short.MAX_VALUE;
        }
        return false;
    }

    /**
     * 将解析出的Date适配为目标日期类型
     * <p>java.sql.Date/java.sql.Time/java.sql.Timestamp转换为对应的子类型，其他类型原样返回。</p>
     *
     * @param date       解析出的日期
     * @param targetType 目标类型
     * @return 适配后的日期对象
     */
    private static Date adaptDateType(Date date, Class<?> targetType) {
        if (targetType == java.sql.Timestamp.class) {
            return new java.sql.Timestamp(date.getTime());
        }
        if (targetType == java.sql.Date.class) {
            return new java.sql.Date(date.getTime());
        }
        if (targetType == java.sql.Time.class) {
            return new java.sql.Time(date.getTime());
        }
        return date;
    }
}
