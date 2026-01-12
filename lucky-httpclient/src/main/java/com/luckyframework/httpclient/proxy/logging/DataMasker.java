package com.luckyframework.httpclient.proxy.logging;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类 - 完整修复版
 */
public class DataMasker {

    private static final Map<String, CustomMasker> CUSTOM_MASKERS = new LinkedCaseInsensitiveMap<>();

    public static void addMasker(String fieldName, CustomMasker masker) {
        CUSTOM_MASKERS.put(fieldName, masker);
    }

    public static String maskSensitiveData(String content) {
        return maskSensitiveData(CUSTOM_MASKERS, content);
    }

    public static String maskSensitiveData(Map<String, CustomMasker> maskTypeMap, String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        final Map<String, CustomMasker> finalMaskTypeMap = new LinkedCaseInsensitiveMap<>();
        finalMaskTypeMap.putAll(CUSTOM_MASKERS);
        if (maskTypeMap != null) {
            finalMaskTypeMap.putAll(maskTypeMap);
        }

        try {
            String result = maskContentWithAnsi(finalMaskTypeMap, content);
            result = maskXmlContent(finalMaskTypeMap, result);
            result = maskJsonContent(finalMaskTypeMap, result);
            result = maskUrlParams(finalMaskTypeMap, result);
            result = maskKeyValuePairs(finalMaskTypeMap, result);
            return result;
        } catch (Exception e) {
            System.err.println("DataMasker脱敏异常: " + e.getMessage());
            return content;
        }
    }

    /**
     * 处理带有ANSI颜色代码的内容
     */
    private static String maskContentWithAnsi(Map<String, CustomMasker> maskTypeMap, String content) {
        if (!content.contains("\u001B")) {
            return content;
        }

        // 改进正则：正确处理括号和颜色代码
        String regex = "(\u001B\\[[;\\d]*[A-Za-z])([\"']?)([\\w\\-_]+)([\"']?)\\s*([:=])\\s*(\u001B\\[[;\\d]*[A-Za-z])?([^\\n\\r]+?(?=(?:\u001B|\\s*[;&,\\n\\r}\\)]|\\s*$)))";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            try {
                String keyColor = matcher.group(1);
                String keyQuoteBefore = matcher.group(2);
                String key = matcher.group(3);
                String keyQuoteAfter = matcher.group(4);
                String separator = matcher.group(5);
                String valueColor = matcher.group(6);
                String valueWithPossibleColor = matcher.group(7);
                String originalMatch = matcher.group(0);

                String normalizedKey = normalizeKey(key);

                // 提取纯值，正确处理括号和颜色代码
                String[] extracted = extractValueWithRemainingForAnsi(valueWithPossibleColor);
                String pureValue = extracted[0];
                String remaining = extracted[1];

                CustomMasker masker = findMasker(maskTypeMap, key, normalizedKey);
                if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                    String maskedValue = masker.mask(pureValue);

                    StringBuilder replacement = new StringBuilder();

                    if (keyColor != null) {
                        replacement.append(keyColor);
                    }
                    if (keyQuoteBefore != null && !keyQuoteBefore.isEmpty()) {
                        replacement.append(keyQuoteBefore);
                    }
                    replacement.append(key);
                    if (keyQuoteAfter != null && !keyQuoteAfter.isEmpty()) {
                        replacement.append(keyQuoteAfter);
                    }

                    replacement.append(separator);
                    if (originalMatch.contains(separator + " ") ||
                            (originalMatch.indexOf(separator) > 0 && originalMatch.charAt(originalMatch.indexOf(separator) - 1) == ' ')) {
                        replacement.append(" ");
                    }

                    if (valueColor != null) {
                        replacement.append(valueColor);
                    }

                    boolean needsQuotes = false;
                    char quoteChar = '"';
                    if (valueWithPossibleColor != null) {
                        String trimmedValue = valueWithPossibleColor.trim();
                        if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
                            needsQuotes = true;
                            quoteChar = '"';
                        } else if (trimmedValue.startsWith("'") && trimmedValue.endsWith("'")) {
                            needsQuotes = true;
                            quoteChar = '\'';
                        }
                    }

                    if (needsQuotes) {
                        replacement.append(quoteChar);
                    }

                    replacement.append(maskedValue);

                    if (needsQuotes) {
                        replacement.append(quoteChar);
                    }

                    // 添加剩余部分
                    replacement.append(remaining);

                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
                }
            } catch (Exception e) {
                // 跳过当前匹配
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 提取值并分离剩余部分（针对ANSI格式）
     */
    private static String[] extractValueWithRemainingForAnsi(String valueWithPossibleColor) {
        if (valueWithPossibleColor == null) return new String[]{"", ""};

        String value = valueWithPossibleColor;

        // 查找第一个特殊字符或颜色代码的位置
        int boundaryIndex = -1;
        String remaining = "";

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // 遇到颜色代码开始
            if (c == '\u001B') {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }

            // 遇到特殊空白字符
            if (isSpecialWhitespaceChar(c)) {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }

            // 遇到结束括号
            if (c == ')') {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }

            // 遇到常见分隔符
            if (c == '&' || c == ';' || c == ',') {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }
        }

        String extracted;
        if (boundaryIndex > 0) {
            extracted = value.substring(0, boundaryIndex);
        } else {
            extracted = value;
        }

        extracted = extracted.trim();

        // 去除引号
        if ((extracted.startsWith("\"") && extracted.endsWith("\"")) ||
                (extracted.startsWith("'") && extracted.endsWith("'"))) {
            extracted = extracted.substring(1, extracted.length() - 1);
        }

        return new String[]{extracted.trim(), remaining};
    }

    /**
     * 处理键值对格式
     */
    private static String maskKeyValuePairs(Map<String, CustomMasker> maskTypeMap, String content) {
        try {
            // 改进正则：支持以)结束
            String[] regexPatterns = {
                    "([\\w\\-_]+)=\\s*(\"[^\"]*\"|'[^']*')",
                    "([\\w\\-_]+)\\s*:\\s*(\"[^\"]*\"|'[^']*')",
                    "([\\w\\-_]+)=\\s*([^\\n\\r;&,)]+?(?=\\s*(?:[\\n\\r;&,)]|$)))",
                    "([\\w\\-_]+)\\s*:\\s*([^\\n\\r;,)]+?(?=\\s*(?:[\\n\\r;,)]|$)))",
                    "[\"']([\\w\\-_]+)[\"']\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\n\\r\\s;&,][^\\n\\r;&,]*)"
            };

            String currentContent = content;

            for (String regex : regexPatterns) {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(currentContent);
                StringBuffer buffer = new StringBuffer();

                while (matcher.find()) {
                    try {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        String originalMatch = matcher.group(0);

                        String normalizedKey = normalizeKey(key);

                        // 提取值并分离剩余部分
                        String[] extracted = extractValueWithRemaining(value, originalMatch);
                        String pureValue = extracted[0];
                        String remaining = extracted[1];

                        CustomMasker masker = findMasker(maskTypeMap, key, normalizedKey);
                        if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                            String maskedValue = masker.mask(pureValue);

                            StringBuilder replacement = new StringBuilder();

                            // 判断键是否有引号
                            boolean keyHasQuotes = false;
                            char keyQuoteChar = '"';
                            if (originalMatch != null && !originalMatch.isEmpty()) {
                                String trimmedMatch = originalMatch.trim();
                                if (trimmedMatch.startsWith("\"") || trimmedMatch.startsWith("'")) {
                                    keyHasQuotes = true;
                                    keyQuoteChar = trimmedMatch.charAt(0);
                                    replacement.append(keyQuoteChar).append(key).append(keyQuoteChar);
                                } else {
                                    replacement.append(key);
                                }
                            } else {
                                replacement.append(key);
                            }

                            // 分隔符
                            String separator = originalMatch.contains("=") ? "=" : ":";
                            replacement.append(separator);

                            // 保持原始空格
                            int sepIndex = originalMatch.indexOf(separator);
                            if (sepIndex > 0 && sepIndex < originalMatch.length()) {
                                char beforeSep = originalMatch.charAt(sepIndex - 1);
                                if (beforeSep == ' ') {
                                    replacement.insert(replacement.length() - 1, " ");
                                }
                                if (sepIndex + 1 < originalMatch.length()) {
                                    char afterSep = originalMatch.charAt(sepIndex + 1);
                                    if (afterSep == ' ') {
                                        replacement.append(" ");
                                    }
                                }
                            }

                            // 值部分
                            boolean valueHasQuotes = false;
                            char valueQuoteChar = '"';

                            if (value != null) {
                                String trimmedValue = value.trim();
                                if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
                                    valueHasQuotes = true;
                                    valueQuoteChar = '"';
                                } else if (trimmedValue.startsWith("'") && trimmedValue.endsWith("'")) {
                                    valueHasQuotes = true;
                                    valueQuoteChar = '\'';
                                }
                            }

                            if (valueHasQuotes) {
                                replacement.append(valueQuoteChar).append(maskedValue).append(valueQuoteChar);
                            } else {
                                replacement.append(maskedValue);
                            }

                            // 添加剩余部分
                            replacement.append(remaining);

                            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
                        }
                    } catch (Exception e) {
                        // 跳过当前匹配
                    }
                }

                matcher.appendTail(buffer);
                currentContent = buffer.toString();
            }

            return currentContent;
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 提取值并分离剩余部分
     */
    private static String[] extractValueWithRemaining(String value, String originalMatch) {
        if (value == null) return new String[]{"", ""};

        String remaining = "";
        String extractedValue = value;

        // 检查是否有颜色代码
        boolean hasColorCode = value.contains("\u001B");

        if (hasColorCode) {
            // 找到第一个颜色代码的位置
            int colorIndex = value.indexOf('\u001B');
            if (colorIndex > 0) {
                extractedValue = value.substring(0, colorIndex);
                remaining = value.substring(colorIndex);
            }
        } else {
            // 检查是否有结束括号
            int parenIndex = value.indexOf(')');
            if (parenIndex > 0 && (originalMatch.endsWith(")") || originalMatch.contains(")") && !originalMatch.contains("(" + value))) {
                extractedValue = value.substring(0, parenIndex);
                remaining = value.substring(parenIndex);
            }

            // 检查特殊空白字符
            if (remaining.isEmpty()) {
                for (int i = 0; i < extractedValue.length(); i++) {
                    if (isSpecialWhitespaceChar(extractedValue.charAt(i))) {
                        remaining = extractedValue.substring(i);
                        extractedValue = extractedValue.substring(0, i);
                        break;
                    }
                }
            }
        }

        extractedValue = extractedValue.trim();

        // 去除引号
        if ((extractedValue.startsWith("\"") && extractedValue.endsWith("\"")) ||
                (extractedValue.startsWith("'") && extractedValue.endsWith("'"))) {
            extractedValue = extractedValue.substring(1, extractedValue.length() - 1);
        }

        return new String[]{extractedValue.trim(), remaining};
    }

    /**
     * 处理URL查询参数格式
     */
    private static String maskUrlParams(Map<String, CustomMasker> maskTypeMap, String content) {
        try {
            String regex = "([\\w\\-_]+)=([^&\\n\\r]*?)(?=(?:&|\\n|\\r|$))";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                try {
                    String key = matcher.group(1);
                    String value = matcher.group(2);

                    String normalizedKey = normalizeKey(key);

                    String[] extracted = extractValueWithRemaining(value, null);
                    String pureValue = extracted[0];
                    String remaining = extracted[1];

                    CustomMasker masker = findMasker(maskTypeMap, key, normalizedKey);
                    if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                        String maskedValue = masker.mask(pureValue);

                        StringBuilder replacement = new StringBuilder();
                        replacement.append(key).append("=").append(maskedValue).append(remaining);

                        matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
                    }
                } catch (Exception e) {
                    // 跳过当前匹配
                }
            }

            matcher.appendTail(buffer);
            return buffer.toString();
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 处理XML格式
     */
    private static String maskXmlContent(Map<String, CustomMasker> maskTypeMap, String content) {
        try {
            String regex = "<([\\w\\-_]+)>([^<]+)</\\1>";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                try {
                    String tagName = matcher.group(1);
                    String value = matcher.group(2);
                    if (value != null) {
                        value = value.trim();
                    }

                    String normalizedKey = normalizeKey(tagName);

                    CustomMasker masker = findMasker(maskTypeMap, tagName, normalizedKey);
                    if (masker != null && value != null && !value.isEmpty()) {
                        String maskedValue = masker.mask(value);
                        String replacement = "<" + tagName + ">" + maskedValue + "</" + tagName + ">";
                        matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                    }
                } catch (Exception e) {
                    // 跳过当前匹配
                }
            }

            matcher.appendTail(buffer);
            return buffer.toString();
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 处理JSON格式
     */
    private static String maskJsonContent(Map<String, CustomMasker> maskTypeMap, String content) {
        try {
            String regex = "\"([\\w\\-_]+)\"\\s*:\\s*(\"[^\"]*\"|[^,\\n\\r}]+)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                try {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    String originalMatch = matcher.group(0);

                    String normalizedKey = normalizeKey(key);

                    String pureValue = extractJsonValue(value);

                    CustomMasker masker = findMasker(maskTypeMap, key, normalizedKey);
                    if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                        String maskedValue = masker.mask(pureValue);

                        StringBuilder replacement = new StringBuilder();
                        replacement.append("\"").append(key).append("\"");

                        int colonIndex = originalMatch.indexOf(':');
                        if (colonIndex > 0 && colonIndex < originalMatch.length()) {
                            replacement.append(":");
                            if ((colonIndex > 0 && originalMatch.charAt(colonIndex - 1) == ' ') ||
                                    (colonIndex + 1 < originalMatch.length() && originalMatch.charAt(colonIndex + 1) == ' ')) {
                                replacement.append(" ");
                            }
                        }

                        if (value != null && value.trim().startsWith("\"")) {
                            replacement.append("\"").append(maskedValue).append("\"");
                        } else {
                            replacement.append(maskedValue);
                        }

                        if (value != null && value.trim().endsWith(",")) {
                            replacement.append(",");
                        } else if (originalMatch != null && originalMatch.endsWith(",")) {
                            replacement.append(",");
                        }

                        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
                    }
                } catch (Exception e) {
                    // 跳过当前匹配
                }
            }

            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 提取JSON值
     */
    private static String extractJsonValue(String value) {
        if (value == null) return null;

        String trimmed = value.trim();

        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed.trim();
    }

    /**
     * 判断是否为特殊空白字符
     */
    private static boolean isSpecialWhitespaceChar(char c) {
        return c == '\u200B' || // 零宽空格
                c == '\u200C' || // 零宽非连接符
                c == '\u200D' || // 零宽连接符
                c == '\uFEFF' || // 字节顺序标记
                c == '\u200E' || // 从左至右标记
                c == '\u200F' || // 从右至左标记
                c == '\u202A' || // 从左至右嵌入
                c == '\u202B' || // 从右至左嵌入
                c == '\u202C' || // 弹出方向格式
                c == '\u202D' || // 从左至右覆盖
                c == '\u202E';   // 从右至左覆盖
    }

    /**
     * 标准化键名
     */
    private static String normalizeKey(String key) {
        if (key == null) return null;
        return key.toLowerCase().replaceAll("[-_]", "").trim();
    }

    /**
     * 查找脱敏器
     */
    private static CustomMasker findMasker(Map<String, CustomMasker> maskTypeMap, String originalKey, String normalizedKey) {
        if (maskTypeMap == null || originalKey == null) return null;

        CustomMasker masker = maskTypeMap.get(originalKey);
        if (masker != null) return masker;

        masker = maskTypeMap.get(originalKey.toLowerCase());
        if (masker != null) return masker;

        masker = maskTypeMap.get(normalizedKey);
        if (masker != null) return masker;

        String keyWithoutSeparators = originalKey.replaceAll("[-_]", "");
        masker = maskTypeMap.get(keyWithoutSeparators);
        if (masker != null) return masker;

        return maskTypeMap.get(keyWithoutSeparators.toLowerCase());
    }
}