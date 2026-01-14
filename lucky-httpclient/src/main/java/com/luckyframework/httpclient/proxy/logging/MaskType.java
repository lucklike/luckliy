package com.luckyframework.httpclient.proxy.logging;

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
     * 大数据脱敏<br/>
     * 很短（length <= 8）：完全脱敏<br/>
     * 较短（length <= 16）：保留首2尾2<br/>
     * 中等（length <= 32）：保留首4尾4<br/>
     * 较长（length <= 64）：保留首6尾6<br/>
     * 很长（length > 64）：保留首8尾8<br/>
     */
    BIG_DATA(MaskType::maskBigData),

    /**
     * 完全脱敏，无论内容是什么，都用********替换，长度固定为8个*
     */
    FULL(MaskType::maskFull),

    /**
     * 不脱敏
     */
    NON(s -> s);

    private final CustomMasker masker;

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
        if (idCard.length() <= 10) {
            return "********";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return String.valueOf(phone);
        phone = phone.replaceAll("\\s+", "");
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
        if (bankCard.length() <= 10) {
            return "****";
        }
        return bankCard.substring(0, 6) + "******" + bankCard.substring(bankCard.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return String.valueOf(email);
        int atIndex = email.indexOf('@');
        if (atIndex == -1) return email; // 不是有效的邮箱格式
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

    /**
     * 根据长度动态脱敏
     */
    private static String maskBigData(String base64) {
        int length = base64.length();

        if (length <= 8) {
            // 很短：完全脱敏
            return "********";
        } else if (length <= 16) {
            // 较短：保留首2尾2
            return base64.substring(0, 2) + generateMaskString( 4) +
                    base64.substring(length - 2);
        } else if (length <= 32) {
            // 中等：保留首4尾4
            return base64.substring(0, 4) + generateMaskString( 8) +
                    base64.substring(length - 4);
        } else if (length <= 64) {
            // 较长：保留首6尾6
            return base64.substring(0, 6) + generateMaskString( 12) +
                    base64.substring(length - 6);
        } else {
            // 很长：保留首8尾8
            int keep = Math.min(8, length / 10); // 最多保留8个字符
            // 至少保留4个字符
            return base64.substring(0, keep) + generateMaskString(16) +
                    base64.substring(length - keep);
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
        if (ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
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

        // 无法识别的格式，返回原始值
        return ip;
    }


}
