package com.luckyframework.httpclient.proxy.logging;

import java.util.regex.Pattern;

public enum MaskType implements CustomMasker {

    /**
     * 身份证脱敏<br/>
     * 保留前6位（地区代码），中间8位用*替换，保留最后4位<br/>
     * （510123199001011234 → 510123********1234）
     */
    ID_CARD(MaskType::maskIdCard),

    /**
     * 手机号脱敏<br/>
     * 保留前3位（运营商号段）,中间4位用*替换,保留最后4位<br/>
     * （13800138000 → 138****8000）
     */
    PHONE(MaskType::maskPhone),

    /**
     * 姓名脱敏<br/>
     * 单字姓名：替换为*，两字及以上姓名：保留第一个字，后面用**替换 <br/>
     * （张三 → 张**，诸葛亮 → 诸**）
     */
    NAME(MaskType::maskName),

    /**
     * 银行卡脱敏<br/>
     * 保留前6位（发卡行标识），中间6位用*替换，保留最后4位<br/>
     * （6225888888888888 → 622588******8888）
     */
    BANK_CARD(MaskType::maskBankCard),

    /**
     * 邮箱脱敏<br/>
     * 保留邮箱前缀的前3个字符，前缀剩余部分用***替换，保留@及域名部分 <br/>
     * （test@example.com → tes***@example.com）
     */
    EMAIL(MaskType::maskEmail),

    /**
     * 地址脱敏<br/>
     * 显示前6个字符，剩余部分用****替换<br/>
     * （北京市海淀区中关村大街1号 → 北京市海淀区****）
     */
    ADDRESS(MaskType::maskAddress),

    /**
     * IP脱敏<br/>
     * 192.168.1.100 → 192.*.*.100<br/>
     * 2001:0db8:85a3:0000:0000:8a2e:0370:7334 → 2001:*:*:*:*:*:*:7334<br/>
     */
    IP(MaskType::maskIp),

    /**
     * JWT令牌脱敏<br/>
     * 兼容Bearer前缀，对三段式令牌的每一段各保留前3个字符，其余部分用***替换<br/>
     * （Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.dBjftJeZ4CVP → Bearer eyJ***.eyJ***.dBj***）<br/>
     * 非标准JWT格式时退化为保留前3后4脱敏
     */
    JWT(MaskType::maskJwt),

    /**
     * MAC地址脱敏<br/>
     * 保留第一组与最后一组，中间每组用*替换<br/>
     * （00:1A:2B:3C:4D:5E → 00:*:*:*:*:5E）<br/>
     * 非标准MAC格式时退化为保留前3后4脱敏
     */
    MAC(MaskType::maskMac),

    /**
     * URL脱敏<br/>
     * 保留协议与主机（隐藏主机中可能携带的用户名密码），路径/查询串/锚点用****替换<br/>
     * （https://api.example.com/v1/users?token=abc → https://api.example.com/****）<br/>
     * 只有主机、没有路径与查询串时不做处理；非标准URL格式时退化为保留前3后4脱敏
     */
    URL(MaskType::maskUrl),

    /**
     * 车牌号脱敏<br/>
     * 保留前2位（省份简称+发牌机关代号）与最后2位，其余部分用*替换<br/>
     * （京A12345 → 京A***45，沪AD12345 → 沪A****45）<br/>
     * 非标准车牌格式时退化为保留前3后4脱敏
     */
    PLATE(MaskType::maskPlate),

    /**
     * 完全脱敏，无论内容是什么，都用********替换，长度固定为8个*
     */
    FULL(MaskType::maskFull),

    /**
     * 保留前4后4脱敏
     */
    FIRST4_LAST4(MaskType::maskFirst4Last4),

    /**
     * 保留前3后4脱敏
     */
    FIRST3_LAST4(MaskType::maskFirst3Last4),

    /**
     * 保留前6后4脱敏
     */
    FIRST6_LAST4(MaskType::maskFirst6Last4),

    /**
     * 保留前3后4、中间固定4个*脱敏（星号数量不随值长度变化）<br/>
     * （13812345678 → 138****5678）<br/>
     * 值长度不足8位时退化为完全脱敏
     */
    FIRST3_STAR4_LAST4(v -> maskFixedStar(v, 3, 4, 4)),

    /**
     * 保留前4后4、中间固定4个*脱敏（星号数量不随值长度变化）<br/>
     * （dev-9f8e7d6c5b4a → dev-****5b4a）<br/>
     * 值长度不足8位时退化为完全脱敏
     */
    FIRST4_STAR4_LAST4(v -> maskFixedStar(v, 4, 4, 4)),

    /**
     * 保留前6后4、中间固定6个*脱敏（星号数量不随值长度变化）<br/>
     * （6225888888888888 → 622588******8888）<br/>
     * 值长度不足10位时退化为完全脱敏
     */
    FIRST6_STAR6_LAST4(v -> maskFixedStar(v, 6, 6, 4)),

    /**
     * 保留前6后4、中间固定8个*脱敏（星号数量不随值长度变化）<br/>
     * （510123199001011234 → 510123********1234）<br/>
     * 值长度不足10位时退化为完全脱敏
     */
    FIRST6_STAR8_LAST4(v -> maskFixedStar(v, 6, 8, 4)),

    /**
     * 只保留后4位，前面固定4个*脱敏（星号数量不随值长度变化）<br/>
     * （6225888888888888 → ****8888）<br/>
     * 值长度不足4位时退化为完全脱敏
     */
    STAR4_LAST4(v -> maskFixedStar(v, 0, 4, 4)),

    /**
     * 动态数据脱敏<br/>
     * 很短（length <= 8）：完全脱敏<br/>
     * 较短（length <= 16）：保留首2尾2<br/>
     * 中等（length <= 32）：保留首4尾4<br/>
     * 较长（length <= 64）：保留首6尾6<br/>
     * 很长（length > 64）：保留首8尾8<br/>
     */
    DYNAMIC(MaskType::maskBigData),


    /**
     * 不脱敏
     */
    NON(s -> s);

    private final CustomMasker masker;

    // 预编译常用正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^[1-9]\\d{9,29}$");
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|wss?|ftp)://[^\\s/?#]+.*$");
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5][A-Za-z][A-Za-z0-9]{4,6}$");

    MaskType(CustomMasker masker) {
        this.masker = masker;
    }

    @Override
    public String mask(String value) {
        return masker.mask(value);
    }


    // 脱敏方法实现
    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) return String.valueOf(idCard);

        // 验证身份证格式
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            // 如果不是标准身份证，使用通用脱敏
            return maskFirst6Last4(idCard);
        }

        if (idCard.length() <= 10) {
            return "********";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return String.valueOf(phone);
        phone = phone.replaceAll("\\s+", "");

        // 验证手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            // 如果不是标准手机号，使用通用脱敏
            return maskFirst3Last4(phone);
        }

        if (phone.length() <= 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskName(String name) {
        if (name == null || name.isEmpty()) return String.valueOf(name);
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "**";
    }

    private static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.isEmpty()) return String.valueOf(bankCard);
        bankCard = bankCard.replaceAll("\\s+", "");

        // 验证银行卡格式
        if (!BANK_CARD_PATTERN.matcher(bankCard).matches()) {
            // 如果不是标准银行卡，使用通用脱敏
            return maskFirst6Last4(bankCard);
        }

        if (bankCard.length() <= 10) {
            return "****";
        }
        return bankCard.substring(0, 6) + "******" + bankCard.substring(bankCard.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return String.valueOf(email);

        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            // 非标准邮箱格式时退化为通用脱敏，避免原始数据泄露
            return maskFirst3Last4(email);
        }

        int atIndex = email.indexOf('@');
        if (atIndex == -1) return email;
        if (atIndex <= 3) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }

    private static String maskAddress(String address) {
        if (address == null || address.isEmpty()) return String.valueOf(address);
        if (address.length() <= 6) {
            return address.charAt(0) + "****";
        }
        return address.substring(0, 6) + "****";
    }

    private static String maskFull(String value) {
        if (value == null || value.isEmpty()) return String.valueOf(value);
        return "********";
    }

    private static String maskFirst4Last4(String value) {
        if (value == null || value.isEmpty()) return String.valueOf(value);
        if (value.length() <= 8) {
            return maskFull(value);
        }
        return value.substring(0, 4) + generateMaskString(value.length() - 8) +
                value.substring(value.length() - 4);
    }

    private static String maskFirst3Last4(String value) {
        if (value == null || value.isEmpty()) return String.valueOf(value);
        if (value.length() <= 7) {
            return maskFull(value);
        }
        return value.substring(0, 3) + generateMaskString(value.length() - 7) +
                value.substring(value.length() - 4);
    }

    private static String maskFirst6Last4(String value) {
        if (value == null || value.isEmpty()) return String.valueOf(value);
        if (value.length() <= 10) {
            return maskFull(value);
        }
        return value.substring(0, 6) + generateMaskString(value.length() - 10) +
                value.substring(value.length() - 4);
    }

    /**
     * 首尾保留位数与中间星号数量均固定的脱敏
     * <p>值长度不足以同时保留首尾时退化为完全脱敏（与FIRST4_LAST4等自适应方案的短值处理保持一致）</p>
     *
     * @param value       待脱敏的值
     * @param headLength  保留的首部字符数
     * @param starLength  中间星号数量
     * @param tailLength  保留的尾部字符数
     * @return 脱敏后的值
     */
    private static String maskFixedStar(String value, int headLength, int starLength, int tailLength) {
        if (value == null || value.isEmpty()) return String.valueOf(value);
        if (value.length() <= headLength + tailLength) {
            return maskFull(value);
        }
        return value.substring(0, headLength) + generateMaskString(starLength) +
                value.substring(value.length() - tailLength);
    }

    /**
     * 根据长度动态脱敏
     */
    private static String maskBigData(String base64) {
        if (base64 == null || base64.isEmpty()) return String.valueOf(base64);

        int length = base64.length();

        if (length <= 8) {
            // 很短：完全脱敏
            return "********";
        } else if (length <= 16) {
            // 较短：保留首2尾2
            return base64.substring(0, 2) + generateMaskString(length - 4) +
                    base64.substring(length - 2);
        } else if (length <= 32) {
            // 中等：保留首4尾4
            return base64.substring(0, 4) + generateMaskString(length - 8) +
                    base64.substring(length - 4);
        } else if (length <= 64) {
            // 较长：保留首6尾6
            return base64.substring(0, 6) + generateMaskString(length - 12) +
                    base64.substring(length - 6);
        } else {
            // 很长：保留首8尾8
            return base64.substring(0, 8) + generateMaskString(length - 16) +
                    base64.substring(length - 8);
        }
    }

    /**
     * 生成脱敏字符串
     */
    private static String generateMaskString(int length) {
        if (length <= 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('*');
        }
        return sb.toString();
    }


    /**
     * IP地址脱敏
     * 示例：
     * 192.168.1.100 → 192.*.*.100
     * 2001:0db8:85a3:0000:0000:8a2e:0370:7334 → 2001:*:*:*:*:*:*:7334
     */
    private static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) return String.valueOf(ip);

        ip = ip.trim();

        // IPv4处理
        if (IPV4_PATTERN.matcher(ip).matches()) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + ".*.*." + parts[3];
            }
        }

        // IPv6处理
        if (ip.contains(":")) {
            // 简化的IPv6处理
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                // 保留第一段和最后一段
                StringBuilder masked = new StringBuilder();
                masked.append(parts[0]);
                for (int i = 1; i < parts.length - 1; i++) {
                    masked.append(":*");
                }
                masked.append(":").append(parts[parts.length - 1]);
                return masked.toString();
            }
        }

        // 无法识别的格式时退化为通用脱敏，避免原始数据泄露
        return maskFirst3Last4(ip);
    }

    /**
     * JWT令牌脱敏
     * 兼容Bearer前缀，对三段式令牌的每一段各保留前3个字符，其余部分用***替换
     * 示例：eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.dBjftJeZ4CVP → eyJ***.eyJ***.dBj***
     */
    private static String maskJwt(String jwt) {
        if (jwt == null || jwt.isEmpty()) return String.valueOf(jwt);
        String trimmed = jwt.trim();

        // 兼容常见的Bearer前缀写法
        String prefix = "";
        String token = trimmed;
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0 && trimmed.substring(0, spaceIndex).equalsIgnoreCase("Bearer")) {
            prefix = trimmed.substring(0, spaceIndex + 1);
            token = trimmed.substring(spaceIndex + 1);
        }

        // 验证JWT格式（三段式，由点号分隔）
        if (!JWT_PATTERN.matcher(token).matches()) {
            // 如果不是标准JWT，使用通用脱敏
            return maskFirst3Last4(trimmed);
        }

        String[] segments = token.split("\\.");
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                masked.append('.');
            }
            String segment = segments[i];
            masked.append(segment.length() <= 3 ? "***" : segment.substring(0, 3) + "***");
        }
        return masked.toString();
    }

    /**
     * MAC地址脱敏
     * 保留第一组与最后一组，中间每组用*替换
     * 示例：00:1A:2B:3C:4D:5E → 00:*:*:*:*:5E
     */
    private static String maskMac(String mac) {
        if (mac == null || mac.isEmpty()) return String.valueOf(mac);
        String trimmed = mac.trim();

        // 验证MAC地址格式
        if (!MAC_PATTERN.matcher(trimmed).matches()) {
            // 如果不是标准MAC地址，使用通用脱敏
            return maskFirst3Last4(trimmed);
        }

        String separator = trimmed.contains("-") ? "-" : ":";
        String[] parts = trimmed.split("[:-]");
        StringBuilder masked = new StringBuilder(parts[0]).append(separator);
        for (int i = 1; i < parts.length - 1; i++) {
            masked.append('*').append(separator);
        }
        masked.append(parts[parts.length - 1]);
        return masked.toString();
    }

    /**
     * URL脱敏
     * 保留协议与主机（隐藏主机中可能携带的用户名密码），路径/查询串/锚点用****替换
     * 示例：https://api.example.com/v1/users?token=abc → https://api.example.com/****
     */
    private static String maskUrl(String url) {
        if (url == null || url.isEmpty()) return String.valueOf(url);
        String trimmed = url.trim();

        // 验证URL格式
        if (!URL_PATTERN.matcher(trimmed).matches()) {
            // 如果不是标准URL，使用通用脱敏
            return maskFirst3Last4(trimmed);
        }

        int schemeEnd = trimmed.indexOf("://");
        int pathStart = trimmed.length();
        for (int i = schemeEnd + 3; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                pathStart = i;
                break;
            }
        }

        // 只有主机、没有路径与查询串时不做处理
        if (pathStart == trimmed.length()) {
            return trimmed;
        }

        String authority = trimmed.substring(schemeEnd + 3, pathStart);
        // 隐藏主机中可能携带的用户名密码（如 http://user:pass@host/xxx）
        if (authority.contains("@")) {
            authority = "****@" + authority.substring(authority.indexOf('@') + 1);
        }
        return trimmed.substring(0, schemeEnd + 3) + authority + "/****";
    }

    /**
     * 车牌号脱敏
     * 保留前2位（省份简称+发牌机关代号）与最后2位，其余部分用*替换
     * 示例：京A12345 → 京A***45，沪AD12345 → 沪A****45
     */
    private static String maskPlate(String plate) {
        if (plate == null || plate.isEmpty()) return String.valueOf(plate);
        String trimmed = plate.trim();

        // 验证车牌格式（1位省份简称 + 1位发牌机关代号 + 5~6位序号）
        if (!PLATE_PATTERN.matcher(trimmed).matches()) {
            // 如果不是标准车牌，使用通用脱敏
            return maskFirst3Last4(trimmed);
        }

        return trimmed.substring(0, 2) + generateMaskString(trimmed.length() - 4) +
                trimmed.substring(trimmed.length() - 2);
    }
}