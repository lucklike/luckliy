package com.luckyframework.httpclient.proxy.logging;

import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类 - 修复版（保持原始分隔符）
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

        // 使用通用正则匹配处理所有格式（包含ANSI颜色代码）
        return maskWithPattern(finalMaskTypeMap, content);
    }

    /**
     * 使用通用正则匹配处理各种格式（包含ANSI颜色代码）
     * 支持：
     * 1. [1;31mAccesstoken: [0m43395B397F2543A6
     * 2. [1;31mAccesstoken= [0m43395B397F2543A6
     * 3. [1;31m"Accesstoken": [0m43395B397F2543A6
     * 4. [1;31m"Accesstoken"= [0m43395B397F2543A6
     * 5. "Accesstoken":"43395B397F2543A6"
     * 6. Accesstoken=43395B397F2543A6
     * 7. Accesstoken:43395B397F2543A6
     */
    private static String maskWithPattern(Map<String, CustomMasker> maskTypeMap, String content) {
        // 增强的正则表达式，匹配多种格式（包含ANSI颜色代码）
        // 格式：ANSI颜色代码 + (可选引号) + 键名 + (可选引号) + 空格* + [:或=] + 空格* + ANSI颜色代码 + 值
        String regex = "(\u001B\\[[;\\d]*[A-Za-z])?([\"']?)([\\w_]+)([\"']?)\\s*([:=])\\s*(\u001B\\[[;\\d]*[A-Za-z])?([^\\n\\r]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String keyPrefixColor = matcher.group(1); // 键前面的ANSI颜色代码
            String keyQuoteBefore = matcher.group(2); // 键前面的引号
            String key = matcher.group(3); // 键名
            String keyQuoteAfter = matcher.group(4); // 键后面的引号
            String separator = matcher.group(5); // 分隔符 : 或 =
            String valuePrefixColor = matcher.group(6); // 值前面的ANSI颜色代码
            String valueWithPossibleColor = matcher.group(7); // 值（可能包含更多颜色代码）

            // 提取纯值（去除尾部的ANSI重置代码等）
            String value = extractValue(valueWithPossibleColor);

            CustomMasker maskType = maskTypeMap.get(key);
            if (maskType != null && value != null && !value.isEmpty()) {
                String maskedValue = maskType.mask(value);

                // 构建替换字符串，保持ANSI颜色代码和原始格式
                StringBuilder replacement = new StringBuilder();

                // 键部分
                if (keyPrefixColor != null) {
                    replacement.append(keyPrefixColor);
                }
                if (keyQuoteBefore != null && !keyQuoteBefore.isEmpty()) {
                    replacement.append(keyQuoteBefore);
                }
                replacement.append(key);
                if (keyQuoteAfter != null && !keyQuoteAfter.isEmpty()) {
                    replacement.append(keyQuoteAfter);
                }

                // 分隔符和空格
                replacement.append(separator);
                if (matcher.group(0).contains(separator + " ")) {
                    replacement.append(" ");
                }

                // 值部分
                if (valuePrefixColor != null) {
                    replacement.append(valuePrefixColor);
                }
                replacement.append(maskedValue);

                // 添加ANSI重置代码（如果需要）
                boolean hasValueColor = valuePrefixColor != null;
                boolean hasResetInOriginal = valueWithPossibleColor.contains("\u001B[0m");
                if (hasValueColor && !hasResetInOriginal) {
                    replacement.append("\u001B[0m");
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
            }
        }

        matcher.appendTail(result);

        // 也处理没有ANSI颜色代码的标准格式
        return maskStandardFormats(maskTypeMap, result.toString());
    }

    /**
     * 从可能包含ANSI代码的字符串中提取纯值
     */
    private static String extractValue(String valueWithColor) {
        if (valueWithColor == null) return null;

        // 去除首尾空格
        String trimmed = valueWithColor.trim();

        // 如果值以ANSI重置代码结尾，提取之前的部分
        if (trimmed.endsWith("\u001B[0m")) {
            int resetIndex = trimmed.lastIndexOf("\u001B[0m");
            if (resetIndex > 0) {
                return trimmed.substring(0, resetIndex).trim();
            }
        }

        // 如果值中间包含ANSI重置代码，可能有问题，暂时返回整个字符串
        if (trimmed.contains("\u001B[0m") && trimmed.indexOf("\u001B[0m") < trimmed.length() - 4) {
            // 复杂情况，可能有多段颜色代码，暂时返回整个字符串
            return trimmed;
        }

        return trimmed;
    }

    /**
     * 处理标准格式（没有ANSI颜色代码）
     */
    private static String maskStandardFormats(Map<String, CustomMasker> maskTypeMap, String content) {
        // 处理各种标准格式
        String[] regexPatterns = {
                // 格式1: "key"="value" (带双引号的键和值，等号分隔)
                "\"([\\w_]+)\"\\s*=\\s*\"([^\"]*)\"",
                // 格式2: "key":"value" (带双引号的键和值，冒号分隔，JSON格式)
                "\"([\\w_]+)\"\\s*:\\s*\"([^\"]*)\"",
                // 格式3: 'key'='value' (带单引号的键和值，等号分隔)
                "'([\\w_]+)'\\s*=\\s*'([^']*)'",
                // 格式4: 'key':'value' (带单引号的键和值，冒号分隔)
                "'([\\w_]+)'\\s*:\\s*'([^']*)'",
                // 格式5: key="value" (键无引号，值带双引号)
                "([\\w_]+)\\s*=\\s*\"([^\"]*)\"",
                // 格式6: key='value' (键无引号，值带单引号)
                "([\\w_]+)\\s*=\\s*'([^']*)'",
                // 格式7: key=value (键值都无引号，等号分隔)
                "([\\w_]+)\\s*=\\s*([^\\s;&,\\n\\r\"']+)",
                // 格式8: key:value (键值都无引号，冒号分隔)
                "([\\w_]+)\\s*:\\s*([^\\s;&,\\n\\r\"']+)",
                // 格式9: "key":value (键带双引号，值无引号，JSON非字符串值)
                "\"([\\w_]+)\"\\s*:\\s*([^\\s,\\n\\r\"']+)",
                // 格式10: 'key':value (键带单引号，值无引号)
                "'([\\w_]+)'\\s*:\\s*([^\\s,\\n\\r\"']+)"
        };

        String currentContent = content;

        for (String regex : regexPatterns) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(currentContent);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                if (value != null) {
                    value = value.trim();
                    // 去除可能的值尾部逗号（JSON数组或对象中）
                    if (value.endsWith(",")) {
                        value = value.substring(0, value.length() - 1);
                    }
                }

                CustomMasker maskType = maskTypeMap.get(key);
                if (maskType != null && value != null && !value.isEmpty()) {
                    String maskedValue = maskType.mask(value);

                    // 重建原始格式
                    String originalMatch = matcher.group(0);
                    String replacement;

                    if (originalMatch.contains("\"")) {
                        if (originalMatch.startsWith("\"")) {
                            // 键带双引号
                            if (originalMatch.contains("\"=\"")) {
                                // "key"="value" 格式
                                replacement = "\"" + key + "\"=\"" + maskedValue + "\"";
                            } else if (originalMatch.contains("\":\"")) {
                                // "key":"value" 格式
                                replacement = "\"" + key + "\":\"" + maskedValue + "\"";
                            } else if (originalMatch.contains("\":")) {
                                // "key":value 格式（值无引号）
                                replacement = "\"" + key + "\":" + maskedValue;
                            } else {
                                // 其他格式，保持原样
                                replacement = originalMatch;
                            }
                        } else {
                            // key="value" 格式
                            replacement = key + "=\"" + maskedValue + "\"";
                        }
                    } else if (originalMatch.contains("'")) {
                        if (originalMatch.startsWith("'")) {
                            // 键带单引号
                            if (originalMatch.contains("'='")) {
                                // 'key'='value' 格式
                                replacement = "'" + key + "'='" + maskedValue + "'";
                            } else if (originalMatch.contains("':'")) {
                                // 'key':'value' 格式
                                replacement = "'" + key + "':'" + maskedValue + "'";
                            } else if (originalMatch.contains("':")) {
                                // 'key':value 格式（值无引号）
                                replacement = "'" + key + "':" + maskedValue;
                            } else {
                                // 其他格式，保持原样
                                replacement = originalMatch;
                            }
                        } else {
                            // key='value' 格式
                            replacement = key + "='" + maskedValue + "'";
                        }
                    } else if (originalMatch.contains(":")) {
                        // key:value 格式
                        replacement = key + ":" + maskedValue;
                    } else {
                        // key=value 格式
                        replacement = key + "=" + maskedValue;
                    }

                    // 如果原始值有尾部逗号，保持它
                    if (matcher.group(2) != null && matcher.group(2).endsWith(",")) {
                        replacement += ",";
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