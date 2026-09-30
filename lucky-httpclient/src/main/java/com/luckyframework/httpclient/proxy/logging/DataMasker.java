package com.luckyframework.httpclient.proxy.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类
 * <p>
 * 以"日志文本后处理"的方式工作，支持从 ANSI 彩色键值对、XML、JSON、通用键值对、
 * URL 参数等文本格式中识别字段并执行脱敏；另外还支持"裸值模式"脱敏（不依赖字段名），
 * 用于覆盖自由文本、URL路径、multipart裸值等无键名结构的场景。
 *
 * @author DeepSeek
 */
public class DataMasker {

    private static final Logger log = LoggerFactory.getLogger(DataMasker.class);

    /**
     * 通用键名字符类：支持中文等 Unicode 字符
     */
    private static final String KEY_CHARS = "[\\p{L}\\p{N}_\\-]";

    //------------------------------------------------------------------------------------------------------
    //                                     预编译正则（性能关键）
    //------------------------------------------------------------------------------------------------------

    /**
     * ANSI 彩色文本中的键值对
     */
    private static final Pattern ANSI_PATTERN = Pattern.compile(
            "(\u001B\\[[;\\d]*[A-Za-z])([\"']?)(" + KEY_CHARS + "+)([\"']?)\\s*([:=])\\s*(\u001B\\[[;\\d]*[A-Za-z])?([^\\n\\r]+?(?=\u001B|\\s*[;&,\\n\\r})]|\\s*$))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * XML 标签：&lt;tag&gt;value&lt;/tag&gt;
     */
    private static final Pattern XML_PATTERN = Pattern.compile(
            "<(" + KEY_CHARS + "+)>([^<]+)</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * JSON 键值对："key": value（字符串值支持 \\" 转义）
     */
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\"(" + KEY_CHARS + "+)\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|[^,\\n\\r}]+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * URL 查询参数：key=value
     */
    private static final Pattern URL_PARAM_PATTERN = Pattern.compile(
            "(" + KEY_CHARS + "+)=([^&\\n\\r]*?)(?=&|\\n|\\r|$)",
            Pattern.CASE_INSENSITIVE);

    /**
     * 通用键值对（按数组顺序依次应用）
     */
    private static final Pattern[] KV_PATTERNS = {
            // 0. \\"key\\":\\"value\\"（JSON 字符串被再次序列化的场景，值支持 \\ 转义）
            Pattern.compile("\\\\\"(" + KEY_CHARS + "+)\\\\\"\\s*:\\s*\\\\\"((?:\\\\.|[^\\\\\"])*)\\\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 1. key="value" / key='value'（值支持 \ 转义）
            Pattern.compile("(" + KEY_CHARS + "+)=\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 2. key:"value" / key:'value'（值支持 \ 转义）
            Pattern.compile("(" + KEY_CHARS + "+)\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 3. key=value（不带引号）
            Pattern.compile("(" + KEY_CHARS + "+)=\\s*([^\\n\\r;&,)]+?(?=\\s*(?:[\\n\\r;&,)]|$)))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 4. key:value（不带引号）
            Pattern.compile("(" + KEY_CHARS + "+)\\s*:\\s*([^\\n\\r;,)]+?(?=\\s*(?:[\\n\\r;,)]|$)))", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 5. "key"=value / 'key'=value
            Pattern.compile("[\"'](" + KEY_CHARS + "+)[\"']\\s*=\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|[^\\n\\r\\s;&,][^\\n\\r;&,]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    };

    /**
     * 各键值对模式使用的分隔符（与 KV_PATTERNS 一一对应）
     */
    private static final String[] KV_SEPARATORS = {":", "=", ":", "=", ":", "="};

    /**
     * KV_PATTERNS 中值已被引号限定为纯值的模式下标（无需再做边界二次提取）
     */
    private static final int KV_ESCAPED_JSON_INDEX = 0;

    /**
     * 保留 Basic 前缀、仅对凭据部分打码的脱敏器
     */
    private static final CustomMasker BASIC_AUTH_MASKER = value -> {
        int index = value.indexOf(' ');
        return (index < 0 ? "" : value.substring(0, index + 1)) + "********";
    };

    /**
     * 全局脱敏器（字段名 → 脱敏器），线程安全，支持动态增删
     */
    private static final Map<String, CustomMasker> CUSTOM_MASKERS = Collections.synchronizedMap(new LinkedCaseInsensitiveMap<>());

    /**
     * 全局裸值脱敏器（值模式 → 脱敏器），默认无配置，不依赖字段名
     */
    private static final List<ValueMaskerEntry> VALUE_MASKERS = new CopyOnWriteArrayList<>();

    /**
     * 添加一个全局字段脱敏器
     *
     * @param fieldName 字段名（大小写不敏感，支持正则表达式）
     * @param masker    脱敏器
     */
    public static void addMasker(String fieldName, CustomMasker masker) {
        if (fieldName == null || masker == null) {
            return;
        }
        CUSTOM_MASKERS.put(fieldName, masker);
    }

    /**
     * 批量添加全局字段脱敏器
     *
     * @param maskers 字段名 → 脱敏器 的映射
     */
    public static void addMaskers(Map<String, CustomMasker> maskers) {
        if (maskers == null || maskers.isEmpty()) {
            return;
        }
        CUSTOM_MASKERS.putAll(maskers);
    }

    /**
     * 移除一个全局字段脱敏器
     *
     * @param fieldName 字段名
     */
    public static void removeMasker(String fieldName) {
        if (fieldName != null) {
            CUSTOM_MASKERS.remove(fieldName);
        }
    }

    /**
     * 清空所有全局字段脱敏器
     */
    public static void clearMaskers() {
        CUSTOM_MASKERS.clear();
    }

    /**
     * 添加一个全局裸值脱敏器：对文本中匹配 pattern 的内容直接脱敏，不依赖字段名。
     * <p>
     * 用于覆盖自由文本（短信内容、错误消息）、URL路径、multipart裸值等无键名结构的场景。
     * <p>
     * 注意：正则中的 \b 边界在中文语境下可能失效（Java 的 \b 基于 Character.isLetterOrDigit
     * 判定词字符，中文会被视为"单词字符"），当目标值可能紧邻中文字符时，
     * 建议改用 (?&lt;!\d)/(?!\d) 等负向断言界定边界。
     *
     * @param pattern 值模式（对文本执行 find 查找）
     * @param masker  脱敏器（作用于匹配到的整段子串）
     */
    public static void addValueMasker(Pattern pattern, CustomMasker masker) {
        if (pattern == null || masker == null) {
            return;
        }
        ValueMaskerEntry entry = new ValueMaskerEntry(pattern, masker);
        for (int i = 0; i < VALUE_MASKERS.size(); i++) {
            if (VALUE_MASKERS.get(i).pattern.pattern().equals(pattern.pattern())) {
                VALUE_MASKERS.set(i, entry);
                return;
            }
        }
        VALUE_MASKERS.add(entry);
    }

    /**
     * 移除一个全局裸值脱敏器
     *
     * @param pattern 值模式
     */
    public static void removeValueMasker(Pattern pattern) {
        if (pattern == null) {
            return;
        }
        VALUE_MASKERS.removeIf(entry -> entry.pattern.pattern().equals(pattern.pattern()));
    }

    /**
     * 清空所有全局裸值脱敏器
     */
    public static void clearValueMaskers() {
        VALUE_MASKERS.clear();
    }

    /**
     * 一键开启常用字段无关的裸值脱敏（手机号、身份证、邮箱、Basic认证、JWT）。
     * <p>
     * 注意：裸值脱敏按"值的格式特征"匹配，对无键名结构的自由文本很有效，
     * 但存在误伤可能（如恰好符合手机号格式的订单号），请按需开启。
     */
    public static void enableCommonValueMaskers() {
        // 边界不能使用 \b：Java 的 \b 基于 Character.isLetterOrDigit 判定"单词字符"，
        // 中文会命中该判定，导致"至13812345678"这类中文紧邻场景匹配失败；
        // 这里统一使用负向断言界定边界，既避免匹配更长数字/字符序列的一部分，又兼容中文紧邻。
        addValueMasker(Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"), MaskType.PHONE);
        addValueMasker(Pattern.compile("(?<!\\d)[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:[0-2][1-9]|10|20|30|31)\\d{3}[\\dXx](?![0-9Xx])"), MaskType.ID_CARD);
        addValueMasker(Pattern.compile("(?<![A-Za-z0-9._%+\\-])[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}(?![A-Za-z0-9])"), MaskType.EMAIL);
        addValueMasker(Pattern.compile("(?i)(?<![A-Za-z0-9])Basic\\s+[A-Za-z0-9+/=]{4,}(?![A-Za-z0-9+/=])"), BASIC_AUTH_MASKER);
        addValueMasker(Pattern.compile("(?<![A-Za-z0-9_\\-])eyJ[A-Za-z0-9_\\-]*\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+(?![A-Za-z0-9_\\-])"), MaskType.FULL);
    }

    public static String maskSensitiveData(String content) {
        return maskSensitiveData(CUSTOM_MASKERS, content);
    }

    /**
     * 脱敏核心方法
     * <p>
     * 处理工序（依次串联，上一步的输出是下一步的输入）：
     * <pre>
     *     1.ANSI彩色文本中的键值对
     *     2.XML标签
     *     3.JSON
     *     4.通用键值对
     *     5.URL查询参数
     *     6.裸值模式（字段名无关，默认无配置）
     * </pre>
     *
     * @param maskTypeMap 字段名 → 脱敏器 的映射
     * @param content     待脱敏文本
     * @return 脱敏后的文本
     */
    public static String maskSensitiveData(Map<String, CustomMasker> maskTypeMap, String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        final Map<String, CustomMasker> finalMaskTypeMap = new LinkedCaseInsensitiveMap<>();
        synchronized (CUSTOM_MASKERS) {
            finalMaskTypeMap.putAll(CUSTOM_MASKERS);
        }
        if (maskTypeMap != null) {
            finalMaskTypeMap.putAll(maskTypeMap);
        }

        if (finalMaskTypeMap.isEmpty() && VALUE_MASKERS.isEmpty()) {
            return content;
        }

        try {
            String result = maskContentWithAnsi(finalMaskTypeMap, content);
            result = maskXmlContent(finalMaskTypeMap, result);
            result = maskJsonContent(finalMaskTypeMap, result);
            result = maskKeyValuePairs(finalMaskTypeMap, result);
            result = maskUrlParams(finalMaskTypeMap, result);
            result = maskBareValues(result);
            return result;
        } catch (Exception e) {
            log.debug("Lucky-DataMasker: 脱敏过程发生异常，已返回原始内容", e);
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

        Matcher matcher = ANSI_PATTERN.matcher(content);
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
                CustomMasker masker = findMasker(maskTypeMap, key);
                if (masker != null && pureValue != null && !pureValue.isEmpty() && !isLikelyContainerValue(pureValue)) {
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
            String currentContent = content;
    
            for (int index = 0; index < KV_PATTERNS.length; index++) {
                Pattern pattern = KV_PATTERNS[index];
                String separator = KV_SEPARATORS[index];
    
                Matcher matcher = pattern.matcher(currentContent);
                StringBuffer buffer = new StringBuffer();
    
                while (matcher.find()) {
                    try {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        String originalMatch = matcher.group(0);
    
                        // 提取值并分离剩余部分；转义JSON模式的值已被引号限定为纯值，无需再做边界提取
                        String pureValue;
                        String remaining;
                        if (index == KV_ESCAPED_JSON_INDEX) {
                            pureValue = value == null ? "" : value.trim();
                            remaining = "";
                        } else {
                            String[] extracted = extractValueWithRemaining(value, originalMatch);
                            pureValue = extracted[0];
                            remaining = extracted[1];
                        }
    
                        // 查找脱敏器（支持正则匹配）
                        CustomMasker masker = findMasker(maskTypeMap, key);
                        if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                            // 裸值形态下识别到容器结构（数组/对象）时跳过，避免半脱敏破坏文本
                            if (!isQuotedLimitedValue(index, value) && isLikelyContainerValue(pureValue)) {
                                continue;
                            }
    
                            String maskedValue = masker.mask(pureValue);
    
                            // 修复：只在值发生变化时才进行替换
                            if (maskedValue.equals(pureValue)) {
                                continue;
                            }
    
                            StringBuilder replacement = new StringBuilder();
    
                            // 判断键是否有转义引号
                            boolean keyHasEscapeQuotes = originalMatch.startsWith("\\\"");
    
                            if (keyHasEscapeQuotes) {
                                replacement.append("\\\"").append(key).append("\\\"");
                            } else {
                                char keyQuoteChar;
                                if (!originalMatch.isEmpty()) {
                                    String trimmedMatch = originalMatch.trim();
                                    if (trimmedMatch.startsWith("\"") || trimmedMatch.startsWith("'")) {
                                        keyQuoteChar = trimmedMatch.charAt(0);
                                        replacement.append(keyQuoteChar).append(key).append(keyQuoteChar);
                                    } else {
                                        replacement.append(key);
                                    }
                                } else {
                                    replacement.append(key);
                                }
                            }
    
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
    
                            // 判断原始值是否有引号（包括转义引号）
                            boolean valueHasEscapeQuotes = false;
                            boolean valueHasQuotes = false;
                            char valueQuoteChar = '"';
    
                            // 查找等号或冒号后的第一个非空格字符
                            int sepPos = originalMatch.indexOf(separator);
                            if (sepPos >= 0) {
                                String afterSeparator = originalMatch.substring(sepPos + 1).trim();
                                if (!afterSeparator.isEmpty()) {
                                    char firstChar = afterSeparator.charAt(0);
                                    if (firstChar == '"' || firstChar == '\'') {
                                        valueHasQuotes = true;
                                        valueQuoteChar = firstChar;
                                    } else if (afterSeparator.startsWith("\\\"")) {
                                        valueHasEscapeQuotes = true;
                                    }
                                }
                            }
    
                            // 确保引号正确保留
                            if (valueHasEscapeQuotes) {
                                replacement.append("\\\"").append(maskedValue).append("\\\"");
                            } else if (valueHasQuotes) {
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
     * 判断某个键值对模式捕获到的值是否是"引号限定"的完整值（不会被结构字符截断）
     */
    private static boolean isQuotedLimitedValue(int patternIndex, String value) {
        if (patternIndex == 3 || patternIndex == 4) {
            return false;
        }
        if (patternIndex == 5) {
            String trimmed = value == null ? "" : value.trim();
            return trimmed.startsWith("\"") || trimmed.startsWith("'");
        }
        return true;
    }

    /**
     * 提取值并分离剩余部分
     */
    private static String[] extractValueWithRemaining(String value, String originalMatch) {
        if (value == null) return new String[]{"", ""};

        String remaining = "";
        String extractedValue = value.trim();

        boolean hasColorCode = value.contains("\u001B");

        if (hasColorCode) {
            int colorIndex = value.indexOf('\u001B');
            if (colorIndex > 0) {
                extractedValue = value.substring(0, colorIndex).trim();
                remaining = value.substring(colorIndex);
            } else if (colorIndex == 0) {
                int endColorIndex = value.indexOf('m', colorIndex);
                if (endColorIndex > 0) {
                    remaining = value.substring(0, endColorIndex + 1);
                    extractedValue = value.substring(endColorIndex + 1).trim();
                }
            }
        }

        // 检查是否以引号开始（闭合引号查找需跳过被 \ 转义的引号）
        if (extractedValue.startsWith("\"") || extractedValue.startsWith("'")) {
            char quoteChar = extractedValue.charAt(0);
            int endQuoteIndex = findClosingQuote(extractedValue, quoteChar, 1);

            if (endQuoteIndex > 0) {
                // 找到匹配的结束引号
                remaining = extractedValue.substring(endQuoteIndex + 1);
                extractedValue = extractedValue.substring(1, endQuoteIndex);
            } else {
                // 没有找到结束引号，整个作为值
                extractedValue = extractedValue.substring(1);
            }
        } else {
            // 对于不带引号的值，需要查找边界
            if (remaining.isEmpty()) {
                int parenIndex = extractedValue.indexOf(')');
                if (parenIndex > 0 && (originalMatch != null && (originalMatch.endsWith(")") ||
                        (originalMatch.contains(")") && !originalMatch.contains("(" + extractedValue))))) {
                    remaining = extractedValue.substring(parenIndex);
                    extractedValue = extractedValue.substring(0, parenIndex);
                }
            }

            if (remaining.isEmpty()) {
                for (int i = 0; i < extractedValue.length(); i++) {
                    char c = extractedValue.charAt(i);
                    if (isSpecialWhitespaceChar(c) || c == ' ' || c == '\t') {
                        remaining = extractedValue.substring(i);
                        extractedValue = extractedValue.substring(0, i);
                        break;
                    }
                }
            }
        }

        return new String[]{extractedValue.trim(), remaining};
    }

    /**
     * 处理URL查询参数格式
     */
    private static String maskUrlParams(Map<String, CustomMasker> maskTypeMap, String content) {
        try {
            Matcher matcher = URL_PARAM_PATTERN.matcher(content);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                try {
                    String key = matcher.group(1);
                    String value = matcher.group(2);

                    String[] extracted = extractValueWithRemaining(value, null);
                    String pureValue = extracted[0];
                    String remaining = extracted[1];

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMasker(maskTypeMap, key);
                    if (masker != null && pureValue != null && !pureValue.isEmpty() && !isLikelyContainerValue(pureValue)) {
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
            Matcher matcher = XML_PATTERN.matcher(content);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                try {
                    String tagName = matcher.group(1);
                    String value = matcher.group(2);
                    if (value != null) {
                        value = value.trim();
                    }

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMasker(maskTypeMap, tagName);
                    if (masker != null && value != null && !value.isEmpty() && !isLikelyContainerValue(value)) {
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
            Matcher matcher = JSON_PATTERN.matcher(content);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                try {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    String originalMatch = matcher.group(0);

                    boolean quotedValue = value != null && value.trim().startsWith("\"");
                    String pureValue = extractJsonValue(value);

                    // 查找脱敏器（支持正则匹配）
                    CustomMasker masker = findMasker(maskTypeMap, key);
                    if (masker != null && pureValue != null && !pureValue.isEmpty()) {
                        // 裸值形态下识别到容器结构（数组/对象）时跳过，避免半脱敏破坏 JSON 结构
                        if (!quotedValue && isLikelyContainerValue(pureValue)) {
                            continue;
                        }

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

                        if (quotedValue) {
                            replacement.append("\"").append(maskedValue).append("\"");
                        } else {
                            replacement.append(maskedValue);
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
     * 查找脱敏器 - 支持正则表达式匹配（公开 API，处理器对单字段值脱敏时可直接调用）
     *
     * @param maskTypeMap 字段名 → 脱敏器 的映射
     * @param key         字段名
     * @return 命中的脱敏器；未命中返回 {@code null}
     */
    public static CustomMasker findMasker(Map<String, CustomMasker> maskTypeMap, String key) {
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

        // 仅当包含正则元字符时才视为正则模式（"." 属于常见字段名字符，不参与判定）
        String regexSpecialChars = "*+?^${}()|[]\\";
        for (int i = 0; i < regexSpecialChars.length(); i++) {
            if (str.indexOf(regexSpecialChars.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
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

    /**
     * 裸值模式脱敏（字段名无关）：对文本中匹配值模式的内容直接脱敏，
     * 用于覆盖自由文本、URL路径、multipart裸值等无键名结构的场景
     */
    private static String maskBareValues(String content) {
        String result = content;
        for (ValueMaskerEntry entry : VALUE_MASKERS) {
            try {
                result = applyValueMasker(entry, result);
            } catch (Exception e) {
                // 跳过当前值模式
            }
        }
        return result;
    }

    /**
     * 应用单个裸值脱敏器（只在匹配内容发生变化时才进行替换）
     */
    private static String applyValueMasker(ValueMaskerEntry entry, String content) {
        Matcher matcher = entry.pattern.matcher(content);
        StringBuffer buffer = new StringBuffer();
        boolean replaced = false;

        while (matcher.find()) {
            String matched = matcher.group();
            String masked = entry.masker.mask(matched);
            if (masked != null && !masked.equals(matched)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(masked));
                replaced = true;
            }
        }

        if (!replaced) {
            return content;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * 对单个字段值执行脱敏（供日志处理器在非标准文本场景中使用，例如 multipart 字段值）。
     * <p>
     * 先按字段名查找脱敏器，未命中时依次应用裸值脱敏器兜底；任何异常都不会影响原始值返回。
     *
     * @param maskTypeMap 字段名 → 脱敏器 的映射
     * @param fieldName   字段名
     * @param value       原始值
     * @return 脱敏后的值；无命中或异常时返回原始值
     */
    public static String maskFieldValue(Map<String, CustomMasker> maskTypeMap, String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            CustomMasker masker = findMasker(maskTypeMap, fieldName);
            if (masker != null) {
                String masked = masker.mask(value);
                return masked == null ? value : masked;
            }
            String masked = value;
            for (ValueMaskerEntry entry : VALUE_MASKERS) {
                masked = applyValueMasker(entry, masked);
            }
            return masked;
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 判断值是否具有"容器"结构特征（JSON 数组/对象），
     * 容器值不做整体脱敏，避免产生半脱敏的残缺数据
     */
    private static boolean isLikelyContainerValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("[") || trimmed.startsWith("{");
    }

    /**
     * 查找未转义的闭合引号（跳过被 \ 转义的引号）
     *
     * @param str       目标字符串
     * @param quoteChar 引号字符
     * @param fromIndex 起始查找位置
     * @return 闭合引号下标；未找到时返回 -1
     */
    private static int findClosingQuote(String str, char quoteChar, int fromIndex) {
        for (int i = Math.max(fromIndex, 0); i < str.length(); i++) {
            if (str.charAt(i) == quoteChar) {
                // 计算前方连续反斜杠数量，偶数表示该引号未被转义
                int backslashCount = 0;
                int j = i - 1;
                while (j >= 0 && str.charAt(j) == '\\') {
                    backslashCount++;
                    j--;
                }
                if (backslashCount % 2 == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 裸值脱敏器条目：值模式 + 脱敏器
     */
    private static class ValueMaskerEntry {
        private final Pattern pattern;
        private final CustomMasker masker;

        private ValueMaskerEntry(Pattern pattern, CustomMasker masker) {
            this.pattern = pattern;
            this.masker = masker;
        }
    }
}