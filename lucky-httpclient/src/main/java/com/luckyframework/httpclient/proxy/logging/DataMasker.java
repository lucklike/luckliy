package com.luckyframework.httpclient.proxy.logging;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类
 * @author DeepSeek
 */
public class DataMasker {

    private static final Map<String, CustomMasker> CUSTOM_MASKERS = new LinkedCaseInsensitiveMap<>();

    public static void addMasker(String fieldName, CustomMasker masker) {
        CUSTOM_MASKERS.put(fieldName, masker);
    }

    public static String maskSensitiveData(String content) {
        return maskSensitiveData(CUSTOM_MASKERS, content);
    }

    /**
     * 脱敏核心方法
     */
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

        // 修复：改进正则表达式，更精确匹配
        String regex = "(\u001B\\[[;\\d]*[A-Za-z])([\"']?)([\\w\\-_]+)([\"']?)\\s*([:=])\\s*(\u001B\\[[;\\d]*[A-Za-z])?([^\\n\\r]+?(?=\u001B|\\s*[;&,\\n\\r})]|\\s*$))";
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

                // 提取值并分离剩余部分
                String[] extracted = extractValueWithRemainingForAnsi(valueWithPossibleColor);
                String pureValue = extracted[0];
                String remaining = extracted[1];

                // 查找脱敏器（支持正则匹配）
                CustomMasker masker = findMaskerByRegex(maskTypeMap, key);
                if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                    String maskedValue = masker.mask(pureValue);

                    // 修复：只在值发生变化时才进行替换，避免重复脱敏
                    if (maskedValue.equals(pureValue)) {
                        continue;
                    }

                    StringBuilder replacement = new StringBuilder();

                    // 修复：确保ANSI颜色代码的完整性
                    replacement.append(keyColor);
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

                    // 修复：确保valueColor的完整性
                    if (valueColor != null) {
                        replacement.append(valueColor);
                    }

                    boolean needsQuotes = false;
                    char quoteChar = '"';
                    if (valueWithPossibleColor != null) {
                        String trimmedValue = valueWithPossibleColor.trim();
                        if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
                            needsQuotes = true;
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

        int boundaryIndex = -1;
        String remaining = "";

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // 修复：正确处理ANSI颜色代码
            if (c == '\u001B') {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                } else {
                    // 如果ANSI代码在开头，跳过这个字符
                    continue;
                }
                break;
            }

            if (isSpecialWhitespaceChar(c)) {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }

            if (c == ')') {
                if (i > 0) {
                    boundaryIndex = i;
                    remaining = value.substring(i);
                }
                break;
            }

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

                        // 提取值并分离剩余部分
                        String[] extracted = extractValueWithRemaining(value, originalMatch);
                        String pureValue = extracted[0];
                        String remaining = extracted[1];

                        // 查找脱敏器（支持正则匹配）
                        CustomMasker masker = findMaskerByRegex(maskTypeMap, key);
                        if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                            String maskedValue = masker.mask(pureValue);

                            // 修复：只在值发生变化时才进行替换
                            if (maskedValue.equals(pureValue)) {
                                continue;
                            }

                            StringBuilder replacement = new StringBuilder();

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

                            assert originalMatch != null;
                            String separator = originalMatch.contains("=") ? "=" : ":";
                            replacement.append(separator);

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

                            boolean valueHasQuotes = false;
                            char valueQuoteChar = '"';

                            if (value != null) {
                                String trimmedValue = value.trim();
                                if (trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
                                    valueHasQuotes = true;
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

        boolean hasColorCode = value.contains("\u001B");

        if (hasColorCode) {
            int colorIndex = value.indexOf('\u001B');
            if (colorIndex > 0) {
                extractedValue = value.substring(0, colorIndex);
                remaining = value.substring(colorIndex);
            } else if (colorIndex == 0) {
                // 修复：如果颜色代码在开头，跳过它
                int endColorIndex = value.indexOf('m', colorIndex);
                if (endColorIndex > 0) {
                    remaining = value.substring(0, endColorIndex + 1);
                    extractedValue = value.substring(endColorIndex + 1);
                }
            }
        } else {
            int parenIndex = value.indexOf(')');
            if (parenIndex > 0 && (originalMatch != null && (originalMatch.endsWith(")") ||
                    (originalMatch.contains(")") && !originalMatch.contains("(" + value))))) {
                extractedValue = value.substring(0, parenIndex);
                remaining = value.substring(parenIndex);
            }

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
            String regex = "([\\w\\-_]+)=([^&\\n\\r]*?)(?=&|\\n|\\r|$)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                try {
                    String key = matcher.group(1);
                    String value = matcher.group(2);

                    String[] extracted = extractValueWithRemaining(value, null);
                    String pureValue = extracted[0];
                    String remaining = extracted[1];

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMaskerByRegex(maskTypeMap, key);
                    if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                        String maskedValue = masker.mask(pureValue);

                        // 修复：只在值发生变化时才进行替换
                        if (maskedValue.equals(pureValue)) {
                            continue;
                        }

                        matcher.appendReplacement(buffer, Matcher.quoteReplacement(key + "=" + maskedValue + remaining));
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

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMaskerByRegex(maskTypeMap, tagName);
                    if (masker != null && value != null && !value.isEmpty()) {
                        String maskedValue = masker.mask(value);

                        // 修复：只在值发生变化时才进行替换
                        if (maskedValue.equals(value)) {
                            continue;
                        }

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

                    String pureValue = extractJsonValue(value);

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMaskerByRegex(maskTypeMap, key);
                    if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                        String maskedValue = masker.mask(pureValue);

                        // 修复：只在值发生变化时才进行替换
                        if (maskedValue.equals(pureValue)) {
                            continue;
                        }

                        StringBuilder replacement = new StringBuilder();
                        replacement.append("\"").append(key).append("\"");

                        int colonIndex = originalMatch.indexOf(':');
                        if (colonIndex > 0) {
                            replacement.append(":");
                            if (originalMatch.charAt(colonIndex - 1) == ' ' || colonIndex + 1 < originalMatch.length() && originalMatch.charAt(colonIndex + 1) == ' ') {
                                replacement.append(" ");
                            }
                        }

                        if (value.trim().startsWith("\"")) {
                            replacement.append("\"").append(maskedValue).append("\"");
                        } else {
                            replacement.append(maskedValue);
                        }

                        if (value.trim().endsWith(",")) {
                            replacement.append(",");
                        } else if (originalMatch.endsWith(",")) {
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
     * 查找脱敏器 - 支持正则表达式匹配
     */
    private static CustomMasker findMaskerByRegex(Map<String, CustomMasker> maskTypeMap, String key) {
        if (maskTypeMap == null || key == null) return null;

        // 1. 首先尝试精确匹配
        CustomMasker masker = maskTypeMap.get(key);
        if (masker != null) return masker;

        // 2. 尝试小写版本
        masker = maskTypeMap.get(key.toLowerCase());
        if (masker != null) return masker;

        // 3. 尝试标准化后的键名（移除连字符和下划线）
        String normalizedKey = normalizeKey(key);
        masker = maskTypeMap.get(normalizedKey);
        if (masker != null) return masker;

        // 4. 尝试移除连字符和下划线的版本
        String keyWithoutSeparators = key.replaceAll("[-_]", "");
        masker = maskTypeMap.get(keyWithoutSeparators);
        if (masker != null) return masker;

        // 5. 尝试小写且移除分隔符的版本
        masker = maskTypeMap.get(keyWithoutSeparators.toLowerCase());
        if (masker != null) return masker;

        // 6. 尝试正则表达式匹配
        for (Map.Entry<String, CustomMasker> entry : maskTypeMap.entrySet()) {
            String patternKey = entry.getKey();

            // 判断是否为正则表达式（包含特殊正则字符）
            if (isRegexPattern(patternKey)) {
                try {
                    Pattern pattern = Pattern.compile(patternKey, Pattern.CASE_INSENSITIVE);
                    if (pattern.matcher(key).matches()) {
                        return entry.getValue();
                    }

                    // 也尝试匹配标准化后的键名
                    if (pattern.matcher(normalizedKey).matches()) {
                        return entry.getValue();
                    }
                } catch (Exception e) {
                    // 正则表达式无效，跳过
                }
            }
        }

        return null;
    }

    /**
     * 判断字符串是否为正则表达式模式
     */
    private static boolean isRegexPattern(String str) {
        if (str == null || str.length() < 2) return false;

        // 检查是否包含正则表达式特殊字符
        String regexSpecialChars = ".*+?^${}()|[]\\";
        for (int i = 0; i < regexSpecialChars.length(); i++) {
            if (str.indexOf(regexSpecialChars.charAt(i)) >= 0) {
                return true;
            }
        }

        // 检查是否是常见的正则模式
        return str.startsWith("^") || str.endsWith("$") || str.contains(".*") ||
                str.contains("\\d") || str.contains("\\w") || str.contains("\\s") ||
                str.contains("[") || str.contains("]") || str.contains("(") || str.contains(")");
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
        return c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\uFEFF' ||
                c == '\u200E' || c == '\u200F' || c == '\u202A' || c == '\u202B' ||
                c == '\u202C' || c == '\u202D' || c == '\u202E';
    }

    /**
     * 标准化键名
     */
    private static String normalizeKey(String key) {
        if (key == null) return null;
        return key.toLowerCase().replaceAll("[-_]", "").trim();
    }
}