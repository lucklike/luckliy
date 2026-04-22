package com.luckyframework.httpclient.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class HttpHeaderParser {

    /**
     * 更简单的实现：基于引号状态机的解析
     * 增强版本：支持转义引号、URL编码分隔符、逗号分隔
     */
    public static Map<String, String> parseHeaderSimple(String headerValue) {
        return parseHeaderSimple(headerValue, StandardCharsets.UTF_8.name());
    }

    /**
     * 解析HTTP头部，支持URL编码的分隔符
     * @param headerValue 头部值
     * @param charset 字符集（用于URL解码）
     */
    public static Map<String, String> parseHeaderSimple(String headerValue, String charset) {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (headerValue == null || headerValue.trim().isEmpty()) {
            return result;
        }

        String value = headerValue.trim();
        int length = value.length();
        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            // 检查是否是URL编码的分隔符
            if (!inQuotes && c == '%' && i + 2 < length) {
                String encoded = value.substring(i, i + 3);
                char decoded = decodeUrlChar(encoded);

                // 如果解码后是分隔符，则按分隔符处理
                if (isSeparator(decoded)) {
                    String param = value.substring(start, i).trim();
                    if (!param.isEmpty()) {
                        parseParameter(param, result, charset);
                    }
                    start = i + 3; // 跳过URL编码的3个字符
                    i += 2; // 循环会再+1，所以+2即可跳过整个编码
                    continue;
                }
            }

            if (c == '"') {
                // 处理转义引号
                if (i > 0 && value.charAt(i - 1) == '\\') {
                    continue;
                }
                inQuotes = !inQuotes;
            } else if ((c == ';' || c == ',') && !inQuotes) {
                // 处理一个完整的参数
                String param = value.substring(start, i).trim();
                if (!param.isEmpty()) {
                    parseParameter(param, result, charset);
                }
                start = i + 1;
            }
        }

        // 处理最后一个参数
        if (start < length) {
            String param = value.substring(start).trim();
            if (!param.isEmpty()) {
                parseParameter(param, result, charset);
            }
        }

        return result;
    }

    /**
     * 判断字符是否为分隔符
     */
    private static boolean isSeparator(char c) {
        return c == '=' || c == ';' || c == ',' || c == '(' || c == ')';
    }

    /**
     * 解码URL编码的单个字符（%XX格式）
     */
    private static char decodeUrlChar(String encoded) {
        if (encoded == null || encoded.length() != 3 || encoded.charAt(0) != '%') {
            return 0;
        }
        try {
            int code = Integer.parseInt(encoded.substring(1), 16);
            return (char) code;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析单个参数
     */
    private static void parseParameter(String param, Map<String, String> result, String charset) {
        if (param.isEmpty()) {
            return;
        }

        // 查找第一个不在引号内的等号（支持URL编码的等号）
        int equalsPos = findEqualsOutsideQuotesWithUrlEncoding(param, charset);

        if (equalsPos == -1) {
            // 没有等号，整个作为key，value为空
            String decodedKey = urlDecodeSafe(param, charset);
            result.put(decodedKey, "");
        } else {
            String key = param.substring(0, equalsPos).trim();
            String val = param.substring(equalsPos + 1).trim();

            // 去除引号（支持嵌套引号和转义）
            val = unquoteValue(val, charset);

            // 解码key和value
            String decodedKey = urlDecodeSafe(key, charset);
            String decodedVal = urlDecodeSafe(val, charset);

            result.put(decodedKey, decodedVal);
        }
    }

    /**
     * 去除值的外层引号，并处理转义字符
     */
    private static String unquoteValue(String val, String charset) {
        if (val.length() < 2) {
            return val;
        }

        // 检查是否以引号开头和结尾
        if ((val.startsWith("\"") && val.endsWith("\"")) ||
                (val.startsWith("'") && val.endsWith("'"))) {

            // 去除外层引号
            String unquoted = val.substring(1, val.length() - 1);

            // 处理转义字符
            return unescapeString(unquoted);
        }

        return val;
    }

    /**
     * 处理转义字符（如 \" \\ \t \n \r）
     */
    private static String unescapeString(String str) {
        if (str == null || !str.contains("\\")) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '\\' && i + 1 < length) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 查找不在引号内的等号位置（支持URL编码的等号）
     */
    private static int findEqualsOutsideQuotesWithUrlEncoding(String str, String charset) {
        boolean inQuotes = false;
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            // 检查是否是URL编码的等号 %3D
            if (!inQuotes && c == '%' && i + 2 < length) {
                String encoded = str.substring(i, i + 3);
                if (encoded.equalsIgnoreCase("%3D")) {
                    return i; // URL编码的等号位置
                }
            }

            if (c == '"') {
                // 检查是否是转义引号
                if (i > 0 && str.charAt(i - 1) == '\\') {
                    continue;
                }
                inQuotes = !inQuotes;
            } else if (c == '=' && !inQuotes) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 安全的URL解码
     */
    private static String urlDecodeSafe(String str, String charset) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // 检查是否包含URL编码
        if (!str.contains("%")) {
            return str;
        }

        try {
            return URLDecoder.decode(str, charset);
        } catch (UnsupportedEncodingException e) {
            // 降级处理：手动解码常见编码
            return manualUrlDecode(str);
        }
    }

    /**
     * 手动URL解码（降级方案）
     */
    private static String manualUrlDecode(String str) {
        if (str == null || !str.contains("%")) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c == '%' && i + 2 < length) {
                String hex = str.substring(i + 1, i + 3);
                try {
                    int code = Integer.parseInt(hex, 16);
                    result.append((char) code);
                    i += 2;
                } catch (NumberFormatException e) {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static void main(String[] args) {
        Map<String, String> stringStringMap = parseHeaderSimple("attachment%3B%20filename%3DDevSidecar-2.0.1-windows-x86_64.exe");
        System.out.println(stringStringMap);
    }
}