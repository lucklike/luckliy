package com.luckyframework.common;

import com.luckyframework.exception.LuckyFormatException;
import org.springframework.lang.NonNull;

import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基本工具类
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/14 1:56 下午
 */
public abstract class StringUtils extends org.springframework.util.StringUtils {

    /**
     * {}
     */
    private final static Pattern BRACE_PATTERN = Pattern.compile("\\{(?!\\{)(?!})[\\S\\s]+?}");

    private final static Pattern BRACE_NUM_PATTERN = Pattern.compile("\\{\\d*}");

    private final static Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]*$");

    /**
     * 判断该类型是否为java类型
     *
     * @param clzz
     * @return
     */
    public static boolean isJavaClass(Class<?> clzz) {
        return clzz != null && clzz.getClassLoader() == null;
    }

    /**
     * 小数转百分数
     *
     * @param d              待转化的小数
     * @param integerDigits  小数点前保留几位
     * @param fractionDigits 小数点后保留几位
     * @return 小数对应的百分数
     */
    public static String decimalToPercent(double d, int integerDigits, int fractionDigits) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumIntegerDigits(integerDigits);//小数点前保留几位
        nf.setMinimumFractionDigits(fractionDigits);// 小数点后保留几位
        return nf.format(d);
    }

    /**
     * 小数转百分数，格式(xx.xxx%)
     *
     * @param d 待转化的小数 99.333%
     * @return 最大3位整数部分三位小数部分的百分数，例如：99.234%
     */
    public static String decimalToPercent(double d) {
        return decimalToPercent(d, 3, 3);
    }

    /**
     * 生成一个10000以内的随机数
     *
     * @return 10000以内的随机数
     */
    public static int getRandomNumber() {
        return (int) (Math.random() * 10000);
    }

    /**
     * 字符串拷贝连接，将某个字符串连续拷贝n次后并用制定的字符连接<br/>
     * 例如：<br/>
     * str         -> ?<br/>
     * copyNum     -> 3<br/>
     * joinChar    -> ,<br/>
     * return      -> ?,?,?<br/>
     *
     * @param str      待复制的字符串
     * @param copyNum  复制次数
     * @param joinChar 连接符
     * @return 拷贝拼接后的字符串
     */
    public static String stringCopy(String str, int copyNum, String joinChar) {
        return String.join(joinChar, Collections.nCopies(copyNum, str));
    }

    /**
     * 判断一个对象是否可以转换为数字
     *
     * @param object 待判断的对象
     * @return 是否可以转换为数字
     */
    public static boolean isNumber(Object object) {
        if (object == null) {
            return false;
        }
        return NUMBER_PATTERN.matcher(object.toString()).matches();
    }

    /**
     * 字符串格式化方法[可变参方式]<br/>
     * eg:<br/>
     * tempStr    : ftp://localhost:8008/lucky/{}?id={}<br/>
     * params     : [httpClient,1234jyh54-lucky-w322]<br/>
     * return     : ftp:/localhost:8008/lucky/httpClient?id=1234jyh54-lucky-w322<br/>
     *
     * @param stringTemp 字符串模版
     * @param args       参数值
     * @return 格式化后的字符串
     */
    public static String format(String stringTemp, Object... args) {
        if (ContainerUtils.isEmptyArray(args)) {
            return stringTemp;
        }
        TempPair<String[], List<String>> cutPair = regularCut(stringTemp, BRACE_NUM_PATTERN);
        List<String> exList = cutPair.getTwo();

        // 校验表达式，要么都带数字标识，要么都不带数字标识
        if (!exList.isEmpty() && exList.size() != 1) {
            // 第一个元素是否为数字花括号
            boolean firstEntryIsBraceNum = exList.get(0).length() > 2;
            for (int i = 1; i < exList.size(); i++) {
                boolean currEntryIsBraceNum = exList.get(i).length() > 2;
                if (firstEntryIsBraceNum != currEntryIsBraceNum) {
                    throw new LuckyFormatException("Inconsistent style of brace placeholders in expression '" + stringTemp + "' : '" + exList.get(0) + "';'" + exList.get(i) + "'");
                }
            }

            // 花括号数字风格
            if (firstEntryIsBraceNum) {
                List<Object> realArgs = new ArrayList<>();
                for (String ex : exList) {
                    int index = Integer.parseInt(ex.substring(1, ex.length() - 1));
                    if (index < 0 || index > args.length - 1) {
                        throw new LuckyFormatException("Expression exception, index indication bit in curly brace placeholder '" + ex + "' is out of range!");
                    }
                    realArgs.add(args[index]);
                }
                return misalignedSplice(cutPair.getOne(), realArgs.toArray(new Object[0]));
            }
            // 单一挂括号风格
            else {
                return misalignedSplice(cutPair.getOne(), args);
            }

        } else {
            return misalignedSplice(cutPair.getOne(), args);
        }

    }


    /**
     * 字符串格式化方法[名称指定方式]<br/>
     * eg:<br/>
     * tempStr    : ftp://localhost:8008/lucky/{project}/{userName}?user={userName}&project={project}<br/>
     * paramMap   : [project = httpclient , userName = Jack]<br/>
     * return     : ftp://localhost:8008/lucky/httpclient/Jack?user=Jack&project=httpclient<br/>
     *
     * @param stringTemp 字符串模版
     * @param paramMap   参数Map
     * @return 格式化后的字符串
     */
    public static String format(String stringTemp, Map<String, Object> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) {
            return stringTemp;
        }
        TempPair<String[], List<String>> cutPair = regularCut(stringTemp, BRACE_PATTERN);
        List<String> exList = cutPair.getTwo();

        List<Object> realArgs = new ArrayList<>();
        for (String ex : exList) {
            String key = ex.substring(1, ex.length() - 1);
            Object value = paramMap.get(key);
            if (value == null) {
                throw new LuckyFormatException("The parameter name of '" + ex + "' in curly brace placeholder in expression '" + stringTemp + "' could not be found in parameter Map: " + paramMap + "!");
            }
            realArgs.add(value);
        }
        return misalignedSplice(cutPair.getOne(), realArgs.toArray(new Object[0]));
    }

    /**
     * 正则替换，将原字符串中符合正则表达式的部分替换为给定的参数值
     *
     * @param sourceString 原字符串
     * @param pattern      正则表达式
     * @param args         替换正则表达式部分的参数列表
     * @return 替换后的字符串
     */
    public static String regularReplacement(String sourceString, String pattern, Object... args) {
        String[] split = sourceString.split(pattern, -1);
        return misalignedSplice(split, args);
    }

    /**
     * 正则表达式切割，
     * 例如：
     * pattern -> "\\{[\\S\\s]*?}"
     * string  -> ftp://localhost:8008/lucky/{}?id={}<br/>
     * return  -> {["ftp://localhost:8008/lucky/",?id=],["{}","{}"]}
     *
     * @param pattern 正则表达式
     * @param string  待切割的字符串
     * @return 切割后的数组对
     */
    public static TempPair<String[], List<String>> regularCut(String string, Pattern pattern) {
        String[] split = string.split(pattern.pattern(), -1);
        Matcher matcher = pattern.matcher(string);
        List<String> matchList = new ArrayList<>();
        while (matcher.find()) {
            matchList.add(matcher.group());
        }
        return TempPair.of(split, matchList);
    }

    /**
     * 错位拼接<br/>
     * 例如：<br/>
     * arrayOne -> ["Hello" , "I Love"]<br/>
     * arrayTwo -> [Jack , "You"]<br/>
     * return "Hello Jack I Love You"<br/>
     *
     * @param arrayOne 第一个数组参数
     * @param arrayTwo 第一个数组参数
     * @return 错位拼接后的结果
     */
    public static String misalignedSplice(@NonNull String[] arrayOne, @NonNull Object... arrayTwo) {
        if (arrayOne.length - arrayTwo.length != 1) {
            throw new IllegalArgumentException("Dislocation splicing exception! The length of the first parameter must be one larger than the second parameter, the length of parameter one [" + arrayOne.length + "], the length of parameter two [" + arrayTwo.length + "]");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayOne.length - 1; i++) {
            sb.append(arrayOne[i]).append(arrayTwo[i]);
        }
        sb.append(arrayOne[arrayOne.length - 1]);
        return sb.toString();
    }

    /**
     * 驼峰转其他
     *
     * @param str       驼峰字符串
     * @param delimiter 分隔符
     * @return 转换后的字符串
     */
    public static String humpToOtherFormats(String str, String delimiter) {
        if (!hasText(str)) {
            return str;
        }
        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            char currChar = chars[i];

            // 是小写字母
            if (Character.isLowerCase(currChar)) {
                sb.append(currChar);
            }
            // 是数字
            else if (isNumber(currChar)) {
                if (i == 0 || isNumber(chars[i - 1])) {
                    sb.append(currChar);
                } else {
                    sb.append(delimiter).append(currChar);
                }
            }
            // 大写字母或其他
            else {

                char lowerCaseChar = Character.toLowerCase(currChar);

                // 是第一位
                if (i == 0) {
                    sb.append(lowerCaseChar);
                } else if (isNumber(chars[i - 1]) || Character.isLowerCase(chars[i - 1])) {
                    sb.append(delimiter).append(lowerCaseChar);
                } else {
                    sb.append(lowerCaseChar);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 其他格式转驼峰
     *
     * @param param     其他字符串
     * @param delimiter 分隔符
     * @return 驼峰格式的字符串
     */
    public static String otherFormatsToCamel(String param, char delimiter) {
        if (!hasText(param)) {
            return param;
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(param.charAt(i));
            if (c == delimiter) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static <T> T toType(Object object, Class<T> toType) {
        return object == null ? null : (T) toType;
    }

    public static <T> T getValue(T value, T valueIsNullDefaultValue) {
        return value == null ? valueIsNullDefaultValue : value;
    }

    public static String getDefaultValueIfNonText(String text, String defaultValue) {
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    public static String getString(String str, String defaultValue) {
        return hasText(str) ? str : defaultValue;
    }

    /**
     * 是否包含全角字符
     *
     * @param str 待检测字符
     * @return 是否包含全角字符
     */
    public static boolean containsFullWidth(String str) {
        if (str == null) {
            return false;
        }
        return str.length() != str.getBytes().length;
    }

    /**
     * 半角转全角
     *
     * @param val 半角字符串
     * @return 全角字符串
     */
    public static String toFullWidth(String val) {
        char[] chars = val.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                chars[i] = '\u3000';
            } else if (chars[i] < '\177') {
                chars[i] = (char) (chars[i] + 65248);
            }
        }
        return new String(chars);
    }

    /**
     * 全角转半角
     *
     * @param val 全角字符串
     * @return 半角字符串
     */
    public static String toHalfWidth(String val) {
        char[] chars = val.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\u3000') {
                chars[i] = ' ';
            } else if (chars[i] > '\uFF00' && chars[i] < '\uFF5F') {
                chars[i] = (char) (chars[i] - 65248);
            }
        }
        return new String(chars);
    }

    public static String trimBothEndsChars(String srcStr, String splitter) {
        String regex = "^" + splitter + "*|" + splitter + "*$";
        return srcStr.replaceAll(regex, "");
    }

    public static String arrayToString(Object[] array) {
        return arrayToString(array, ",");
    }

    public static String arrayToString(Object[] array, String separator) {
        if (ContainerUtils.isEmptyArray(array)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj).append(separator);
        }
        return sb.substring(0, sb.length() - separator.length());
    }

    public static String joinUrlPath(String pathPrefix, String pathSuffix) {
        if (!StringUtils.hasText(pathPrefix)) {
            return pathSuffix;
        }
        if (!StringUtils.hasText(pathSuffix)) {
            return pathPrefix;
        }
        if (pathSuffix.startsWith("http://") || pathSuffix.startsWith("https://")) {
            return pathSuffix;
        }
        pathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        pathSuffix = pathSuffix.startsWith("/") ? pathSuffix.substring(1) : pathSuffix;
        return pathPrefix + pathSuffix;
    }

    public static String joinUrlAndParams(String url, String paramStr) {
        if (!hasText(url)) {
            throw new IllegalArgumentException("url is null.");
        }
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        if (!hasText(paramStr)) {
            return url;
        }
        if (url.endsWith("&")) {
            return url + paramStr;
        }
        if (url.contains("?")) {
            return url + "&" + paramStr;
        }
        return url + "?" + paramStr;
    }

    public static String getUrlResourceName(String url) {
        String filename = StringUtils.getFilename(url);

        int i = filename.indexOf("?");
        if (i != -1) {
            filename = filename.substring(0, i);
        }

        int j = filename.indexOf("#");
        if (j != -1) {
            filename = filename.substring(0, j);
        }
        return filename;
    }

    public static String getClassName(Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();
        return (canonicalName != null ? canonicalName : clazz.getName());
    }

    public static String toString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return '"' + value.toString() + '"';
        }
        if (value instanceof Character) {
            return '\'' + value.toString() + '\'';
        }
        if (value instanceof Byte) {
            return String.format("(byte) 0x%02X", value);
        }
        if (value instanceof Long) {
            return Long.toString(((Long) value)) + 'L';
        }
        if (value instanceof Float) {
            return Float.toString(((Float) value)) + 'f';
        }
        if (value instanceof Double) {
            return Double.toString(((Double) value)) + 'd';
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Class) {
            return getClassName((Class<?>) value) + ".class";
        }
        if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder("{");
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(toString(Array.get(value, i)));
            }
            builder.append('}');
            return builder.toString();
        }
        return String.valueOf(value);
    }



    public static String join(Object elements, CharSequence start, CharSequence delimiter, CharSequence end) {
        String context;
        if (ContainerUtils.isIterable(elements)) {
            Iterator<Object> iterator = ContainerUtils.getIterator(elements);
            context = String.join(delimiter, () -> new Iterator<CharSequence>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public CharSequence next() {
                    return String.valueOf(iterator.next());
                }
            });
        } else {
            context =  String.valueOf(elements);
        }
        return start + context + end;
    }

    public static String join(Object elements, CharSequence delimiter) {
        return join(elements, "", delimiter, "");
    }


    public static void main(String[] args) {
        Object[] objects = {1,2,4,"hello", "fukang"};
        System.out.println(join(objects, "[", ", ", "]"));
    }
}
