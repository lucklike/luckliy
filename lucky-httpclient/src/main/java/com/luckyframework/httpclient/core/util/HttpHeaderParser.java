package com.luckyframework.httpclient.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HttpHeaderParser {

    /**
     * 解析HTTP头部（只使用分号作为分隔符，逗号视为普通字符）
     * 适用于 Cookie 等逗号出现在值中的场景
     */
    public static Map<String, String> parseHeaderSimple(String headerValue) {
        return parseHeaderSimple(headerValue, StandardCharsets.UTF_8.name());
    }

    /**
     * 解析HTTP头部（只使用分号作为分隔符，逗号视为普通字符）
     * @param headerValue 头部值
     * @param charset 字符集（用于URL解码）
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
            } else if (c == ';' && !inQuotes) {
                // 只使用分号作为分隔符，逗号不作为分隔符
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
            // 没有等号的情况
            String token = param.trim();
            if (token.isEmpty()) {
                return;
            }

            // 如果是一个简单的 token（只包含字母、数字、连字符、下划线、点号），则忽略
            if (isSimpleToken(token)) {
                return;  // 忽略 form-data、keep-alive 这种单独的 token
            }

            // 否则（如包含特殊字符），整个作为key，value为空
            result.put(token, "");
        } else {
            String key = param.substring(0, equalsPos).trim();
            String val = param.substring(equalsPos + 1).trim();

            // 去除引号（支持嵌套引号和转义）
            val = unquoteValue(val);

            result.put(key, val);
        }
    }

    /**
     * 判断是否是一个简单的 token
     * 简单 token：只包含字母、数字、连字符、下划线、点号，不包含空格、引号、分号、逗号等
     */
    private static boolean isSimpleToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            // 允许字母、数字、连字符、下划线、点号
            if (!((c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }
        return true;
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

    public static void main(String[] args) {
        System.out.println("========== HttpHeaderParser 测试用例 ==========\n");

        // 1. 基本参数解析（分号分隔）
        test("基本参数（分号）", "name=John; age=30; city=NewYork",
                map("name", "John", "age", "30", "city", "NewYork"));

        // 2. 带引号的值
        test("带引号的值", "filename=\"test.txt\"; type=\"text/plain\"",
                map("filename", "test.txt", "type", "text/plain"));

        // 3. 引号内的分号应该被忽略
        test("引号内包含分号", "message=\"Hello; world; test=ok\"; flag=true",
                map("message", "Hello; world; test=ok", "flag", "true"));

        // 4. 转义引号
        test("转义引号", "text=\"He said \\\"Hello\\\" to me\"; escaped=ok",
                map("text", "He said \"Hello\" to me", "escaped", "ok"));

        // 5. 转义字符（\t, \n, \r, \\）
        test("转义字符", "data=\"line1\\nline2\\t\\ttabbed\"; backslash=\"path\\\\to\\\\file\"",
                map("data", "line1\nline2\t\ttabbed", "backslash", "path\\to\\file"));

        // 6. URL 编码的分隔符（%3B = ;）
        test("URL编码的分号", "attachment%3B%20filename%3Dtest.pdf",
                map("attachment", "", "filename", "test.pdf"));

        // 7. 真实用例（DevSidecar）
        test("真实用例（DevSidecar）", "attachment%3B%20filename%3DDevSidecar-2.0.1-windows-x86_64.exe",
                map("attachment", "", "filename", "DevSidecar-2.0.1-windows-x86_64.exe"));

        // 8. URL 编码的中文
        test("URL编码的中文", "name=%E5%BC%A0%E4%B8%89; city=%E5%8C%97%E4%BA%AC",
                map("name", "张三", "city", "北京"));

        // 9. 没有值的参数（只有 key）- 简单标识符会被过滤
        test("无值参数（多个）", "no-value; with-value=123; just-key",
                map("with-value", "123"));

        // 10. 空格处理
        test("空格处理", "  name  =  John  ;  age  =  30  ",
                map("name", "John", "age", "30"));

        // 11. 空值或 null
        test("null输入", null, new HashMap<>());
        test("空字符串输入", "", new HashMap<>());
        test("空白字符串输入", "   ", new HashMap<>());

        // 12. 单引号作为引号
        test("单引号引用", "filename='test.txt'; type='text/plain'",
                map("filename", "test.txt", "type", "text/plain"));

        // 13. URL编码 + 引号混合
        test("URL编码 + 引号混合", "value=\"%22quoted%20inside%22\"; simple=test",
                map("value", "\"quoted inside\"", "simple", "test"));

        // 14. 等号被URL编码（%3D）
        test("等号被URL编码", "encodedKey%3Dvalue",
                map("encodedKey", "value"));

        // 15. Content-Disposition 真实场景
        test("Content-Disposition 真实场景",
                "form-data; name=\"field1\"; filename=\"example\\\"file\\\".txt\"",
                map("name", "field1", "filename", "example\"file\".txt"));

        // 16. Cookie 格式测试（逗号在值中，不作为分隔符）
        test("Cookie 格式", "BAIDUID=B9117A7C0EBB37564BA51C40F406B3AD; expires=Sat, 24-Apr-27 01:36:39 GMT; max-age=31536000; path=/; domain=.baidu.com; version=1",
                map("BAIDUID", "B9117A7C0EBB37564BA51C40F406B3AD",
                        "expires", "Sat, 24-Apr-27 01:36:39 GMT",
                        "max-age", "31536000",
                        "path", "/",
                        "domain", ".baidu.com",
                        "version", "1"));

        // 17. 多个等号的情况
        test("多个等号", "url=https://example.com?q=test&x=1; size=100",
                map("url", "https://example.com?q=test&x=1", "size", "100"));

        // 18. 简单标识符（无等号）应该返回空Map
        test("简单标识符-keep-alive", "keep-alive", new HashMap<>());
        test("简单标识符-close", "close", new HashMap<>());
        test("简单标识符-gzip", "gzip", new HashMap<>());

        // 19. 边界情况：包含特殊字符的无等号参数（应该被保留）
        test("包含特殊字符的无等号参数", "\"quoted value\"", map("\"quoted value\"", ""));
        test("包含空格的无等号参数", "hello world", map("hello world", ""));

        // 20. 大小写不敏感验证
        System.out.println("\n--- 大小写不敏感测试 ---");
        Map<String, String> caseInsensitiveResult = parseHeaderSimple("Content-Type=application/json; charset=utf-8");
        boolean caseInsensitivePassed = "application/json".equals(caseInsensitiveResult.get("content-type")) &&
                "utf-8".equals(caseInsensitiveResult.get("CHARSET"));
        System.out.println((caseInsensitivePassed ? "✓ PASS" : "✗ FAIL") + " - 大小写不敏感");
        if (!caseInsensitivePassed) {
            System.out.println("   实际: " + caseInsensitiveResult);
        }

        System.out.println("\n========== 所有测试用例执行完毕 ==========");
    }

    // Java 8 兼容的 Map 构建方法
    private static Map<String, String> map(String... keyValues) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    private static void test(String caseName, String input, Map<String, String> expected) {
        Map<String, String> actual = parseHeaderSimple(input);
        boolean passed = actual.equals(expected);
        System.out.println((passed ? "✓ PASS" : "✗ FAIL") + " - " + caseName);
        if (!passed) {
            System.out.println("   输入: " + input);
            System.out.println("   期望: " + expected);
            System.out.println("   实际: " + actual);
        }
    }
}