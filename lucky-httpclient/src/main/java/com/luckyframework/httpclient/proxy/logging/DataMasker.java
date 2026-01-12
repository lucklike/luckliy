package com.luckyframework.httpclient.proxy.logging;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类 - 修复版（保持JSON结构）
 *
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
     * 脱敏核心方法 - 统一处理所有格式
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

        // 先处理带有ANSI颜色代码的JSON格式
        String result = maskJsonWithAnsi(finalMaskTypeMap, content);

        // 然后处理标准JSON格式（无ANSI颜色代码）
        result = maskStandardJson(finalMaskTypeMap, result);

        // 最后处理其他格式（URL参数、键值对等）
        result = maskOtherFormats(finalMaskTypeMap, result);

        return result;
    }

    /**
     * 处理带有ANSI颜色代码的JSON格式
     */
    private static String maskJsonWithAnsi(Map<String, CustomMasker> maskTypeMap, String content) {
        if (!content.contains("\u001B")) {
            return content; // 没有ANSI颜色代码，直接返回
        }

        // 匹配带有ANSI颜色代码的JSON键值对
        // 格式：[1;32m"key" : [0m"value"
        String regex = "(\u001B\\[[;\\d]*[A-Za-z])\"?([\\w_]+)\"?\\s*:\\s*(\u001B\\[[;\\d]*[A-Za-z])?([^,\\n\\r}]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String keyColor = matcher.group(1);
            String key = matcher.group(2);
            String valueColor = matcher.group(3);
            String valueWithPossibleColor = matcher.group(4);

            // 提取完整的值（包括引号和可能的ANSI重置代码）
            String fullValue = valueWithPossibleColor;
            String pureValue = extractJsonValue(valueWithPossibleColor);

            CustomMasker masker = maskTypeMap.get(key);
            if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                String maskedValue = masker.mask(pureValue);

                // 重建原始格式，保持引号和空格
                String originalMatch = matcher.group(0);
                StringBuilder replacement = new StringBuilder();

                // 重建键部分
                replacement.append(keyColor);
                if (originalMatch.contains("\"" + key + "\"")) {
                    replacement.append("\"").append(key).append("\"");
                } else {
                    replacement.append(key);
                }

                // 重建分隔符和空格
                if (originalMatch.contains(" : ")) {
                    replacement.append(" : ");
                } else if (originalMatch.contains(":")) {
                    int colonIndex = originalMatch.indexOf(':');
                    String beforeColon = originalMatch.substring(0, colonIndex);
                    String afterColon = originalMatch.substring(colonIndex);
                    replacement.append(":");
                    if (beforeColon.endsWith(" ") || afterColon.startsWith(" ")) {
                        replacement.append(" ");
                    }
                }

                // 重建值部分
                if (valueColor != null) {
                    replacement.append(valueColor);
                }

                // 保持原始值的引号
                if (fullValue != null) {
                    if (fullValue.trim().startsWith("\"")) {
                        replacement.append("\"");
                    }
                    replacement.append(maskedValue);
                    if (fullValue.trim().endsWith("\"")) {
                        replacement.append("\"");
                    }

                    // 保持尾部逗号
                    if (fullValue.trim().endsWith(",")) {
                        replacement.append(",");
                    }

                    // 保持ANSI重置代码
                    if (fullValue.contains("\u001B[0m")) {
                        replacement.append("\u001B[0m");
                    } else if (valueColor != null) {
                        // 如果值有颜色但没有重置代码，添加一个
                        replacement.append("\u001B[0m");
                    }
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 提取JSON值（去除引号和ANSI颜色代码）
     */
    private static String extractJsonValue(String valueWithColor) {
        if (valueWithColor == null) return null;

        String trimmed = valueWithColor.trim();

        // 去除ANSI颜色代码
        String noColor = trimmed.replaceAll("\u001B\\[[;\\d]*[A-Za-z]", "");
        noColor = noColor.trim();

        // 去除引号
        if (noColor.startsWith("\"") && noColor.endsWith("\"")) {
            noColor = noColor.substring(1, noColor.length() - 1);
        }

        // 去除尾部逗号
        if (noColor.endsWith(",")) {
            noColor = noColor.substring(0, noColor.length() - 1);
        }

        return noColor;
    }

    /**
     * 处理标准JSON格式（无ANSI颜色代码）
     */
    private static String maskStandardJson(Map<String, CustomMasker> maskTypeMap, String content) {
        // 匹配标准JSON键值对，保持格式
        String regex = "\"([\\w_]+)\"\\s*:\\s*(\"[^\"]*\"|[^,\\n\\r}]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            String originalMatch = matcher.group(0);

            // 提取纯值
            String pureValue = value.trim();
            boolean isQuoted = pureValue.startsWith("\"") && pureValue.endsWith("\"");

            if (isQuoted) {
                pureValue = pureValue.substring(1, pureValue.length() - 1);
            }

            // 去除尾部逗号
            boolean hasTrailingComma = pureValue.endsWith(",");
            if (hasTrailingComma) {
                pureValue = pureValue.substring(0, pureValue.length() - 1);
            }

            CustomMasker masker = maskTypeMap.get(key);
            if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                String maskedValue = masker.mask(pureValue);

                // 重建JSON键值对，保持原始格式
                StringBuilder replacement = new StringBuilder();
                replacement.append("\"").append(key).append("\"");

                // 保持原始的空格格式
                int colonIndex = originalMatch.indexOf(':');
                String beforeColon = originalMatch.substring(0, colonIndex + 1);
                String afterColon = originalMatch.substring(colonIndex + 1);

                // 重建空格
                if (beforeColon.endsWith(" ") || afterColon.startsWith(" ")) {
                    replacement.append(" : ");
                } else {
                    replacement.append(":");
                }

                // 添加值
                if (isQuoted) {
                    replacement.append("\"").append(maskedValue).append("\"");
                } else {
                    replacement.append(maskedValue);
                }

                // 保持尾部逗号
                if (hasTrailingComma || originalMatch.endsWith(",")) {
                    replacement.append(",");
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理其他格式（URL参数、键值对等）
     */
    private static String maskOtherFormats(Map<String, CustomMasker> maskTypeMap, String content) {
        String[] regexPatterns = {
                // URL参数格式：key=value
                "([\\w_]+)=([^&\\s]+)",
                // 键值对格式：key: value（无引号）
                "([\\w_]+)\\s*:\\s*([^\\s;,\\n\\r]+)"
        };

        String currentContent = content;

        for (String regex : regexPatterns) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(currentContent);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                String originalMatch = matcher.group(0);

                CustomMasker masker = maskTypeMap.get(key);
                if (masker != null && value != null && !value.isEmpty()) {
                    String maskedValue = masker.mask(value);

                    // 根据原始格式重建
                    String replacement;
                    if (originalMatch.contains("=")) {
                        replacement = key + "=" + maskedValue;
                    } else {
                        replacement = key + ":" + maskedValue;
                    }

                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                }
            }

            matcher.appendTail(buffer);
            currentContent = buffer.toString();
        }

        return currentContent;
    }
}