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
     *
     * @param headerValue 头部值
     * @param charset     字符集（用于URL解码）
     */
    public static Map<String, String> parseHeaderSimple(String headerValue, String charset) {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (headerValue == null || headerValue.trim().isEmpty()) {
            return result;
        }

        // 先对整个字符串进行URL解码（使用指定的字符集）
        String decodedValue = urlDecodeFull(headerValue, charset);

        int length = decodedValue.length();
        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < length; i++) {
            char c = decodedValue.charAt(i);

            if (c == '"') {
                // 处理转义引号
                if (i > 0 && decodedValue.charAt(i - 1) == '\\') {
                    continue;
                }
                inQuotes = !inQuotes;
            } else if ((c == ';' || c == ',') && !inQuotes) {
                // 处理一个完整的参数
                String param = decodedValue.substring(start, i).trim();
                if (!param.isEmpty()) {
                    parseParameter(param, result);
                }
                start = i + 1;
            }
        }

        // 处理最后一个参数
        if (start < length) {
            String param = decodedValue.substring(start).trim();
            if (!param.isEmpty()) {
                parseParameter(param, result);
            }
        }

        return result;
    }

    /**
     * 完整URL解码（使用标准URLDecoder）
     */
    private static String urlDecodeFull(String str, String charset) {
        if (str == null || !str.contains("%")) {
            return str;
        }

        try {
            return URLDecoder.decode(str, charset);
        } catch (UnsupportedEncodingException e) {
            // 降级处理
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

    /**
     * 解析单个参数
     */
    private static void parseParameter(String param, Map<String, String> result) {
        if (param.isEmpty()) {
            return;
        }

        // 查找第一个不在引号内的等号
        int equalsPos = findEqualsOutsideQuotes(param);

        if (equalsPos == -1) {
            // 没有等号，整个作为key，value为空
            String key = param.trim();
            if (!key.isEmpty()) {
                result.put(key, "");
            }
        } else {
            String key = param.substring(0, equalsPos).trim();
            String val = param.substring(equalsPos + 1).trim();

            // 去除引号（支持嵌套引号和转义）
            val = unquoteValue(val);

            result.put(key, val);
        }
    }

    /**
     * 去除值的外层引号，并处理转义字符
     */
    private static String unquoteValue(String val) {
        if (val == null || val.length() < 2) {
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
     * 查找不在引号内的等号位置
     */
    private static int findEqualsOutsideQuotes(String str) {
        boolean inQuotes = false;
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

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
}