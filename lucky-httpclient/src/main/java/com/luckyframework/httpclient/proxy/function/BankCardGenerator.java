package com.luckyframework.httpclient.proxy.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 银行卡号生成器
 *
 * @author DeepSeek
 */
public class BankCardGenerator {

    // 银行信息
    public enum Bank {
        ICBC("工商银行", "622202", "622203", "622208"),
        ABC("农业银行", "622848", "622845", "622846"),
        CCB("建设银行", "622700", "622280", "622700"),
        BOC("中国银行", "456351", "456352", "456353"),
        COMM("交通银行", "622258", "622260", "622262"),
        CMB("招商银行", "622588", "622575", "622576"),
        CIB("兴业银行", "622909", "622908", "622902"),
        CEB("光大银行", "622658", "622655", "622656"),
        CMBC("民生银行", "622622", "622615", "622616"),
        PINGAN("平安银行", "622155", "622156", "622157"),

        // 国际卡组织
        VISA("Visa", "4"),
        MASTERCARD("MasterCard", "51", "52", "53", "54", "55"),
        AMEX("American Express", "34", "37"),
        DISCOVER("Discover", "6011", "65"),
        JCB("JCB", "35"),
        UNIONPAY("中国银联", "62");

        private final String name;
        private final List<String> bins;

        Bank(String name, String... bins) {
            this.name = name;
            this.bins = Arrays.asList(bins);
        }

        public String getName() {
            return name;
        }

        public List<String> getBins() {
            return bins;
        }

        public String getRandomBin() {
            return bins.get(new Random().nextInt(bins.size()));
        }

        /**
         * 根据卡号获取银行信息
         */
        public static Bank getByCardNumber(String cardNumber) {
            if (cardNumber == null || cardNumber.isEmpty()) {
                return null;
            }

            String cleanNumber = cardNumber.replaceAll("[\\s-]+", "");

            for (Bank bank : Bank.values()) {
                for (String bin : bank.bins) {
                    if (cleanNumber.startsWith(bin)) {
                        return bank;
                    }
                }
            }

            return null;
        }
    }

    // 卡类型
    public enum CardType {
        DEBIT_CARD("储蓄卡", 16, 19),      // 储蓄卡通常16-19位
        CREDIT_CARD("信用卡", 13, 16),    // 信用卡通常13-16位
        PREPAID_CARD("预付卡", 16, 19),    // 预付卡通常16-19位
        CORPORATE_CARD("公司卡", 16, 16); // 公司卡通常16位

        private final String name;
        private final int minLength;
        private final int maxLength;

        CardType(String name, int minLength, int maxLength) {
            this.name = name;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public String getName() {
            return name;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        /**
         * 获取随机长度
         */
        public int getRandomLength() {
            Random random = ThreadLocalRandom.current();
            if (minLength == maxLength) {
                return minLength;
            }
            return minLength + random.nextInt(maxLength - minLength + 1);
        }
    }

    private static final Random RANDOM = ThreadLocalRandom.current();

    /**
     * 无参生成方法 - 银行和卡类型都随机
     *
     * @return 随机生成的银行卡号
     */
    public static String generateBankCardNumber() {
        // 随机选择银行和卡类型
        Bank[] allBanks = Bank.values();
        CardType[] allCardTypes = CardType.values();

        Bank randomBank = allBanks[RANDOM.nextInt(allBanks.length)];
        CardType randomCardType = allCardTypes[RANDOM.nextInt(allCardTypes.length)];

        return generateBankCardNumber(randomBank, randomCardType);
    }

    /**
     * 生成指定银行和类型的银行卡号
     *
     * @param bank     银行
     * @param cardType 卡类型
     * @return 银行卡号
     */
    public static String generateBankCardNumber(Bank bank, CardType cardType) {
        // 获取BIN
        String bin = bank.getRandomBin();

        // 确定长度（随机在卡类型允许的长度范围内）
        int totalLength = cardType.getRandomLength();

        // 确保长度合理（最少13位，最多19位）
        if (totalLength < 13) totalLength = 13;
        if (totalLength > 19) totalLength = 19;

        // 生成中间数字
        int randomLength = totalLength - bin.length() - 1;
        if (randomLength <= 0) {
            // 如果BIN太长，调整长度
            totalLength = bin.length() + 6; // BIN + 随机数 + 校验位
            randomLength = 5;
        }

        StringBuilder cardNumber = new StringBuilder(bin);

        for (int i = 0; i < randomLength; i++) {
            cardNumber.append(RANDOM.nextInt(10));
        }

        // 计算Luhn校验位
        char checkDigit = calculateLuhnCheckDigit(cardNumber.toString());
        cardNumber.append(checkDigit);

        return cardNumber.toString();
    }

    /**
     * 生成指定银行的随机类型银行卡号
     *
     * @param bank 指定银行
     * @return 银行卡号
     */
    public static String generateBankCardNumber(Bank bank) {
        CardType[] allCardTypes = CardType.values();
        CardType randomCardType = allCardTypes[RANDOM.nextInt(allCardTypes.length)];

        return generateBankCardNumber(bank, randomCardType);
    }

    /**
     * 生成指定卡类型的随机银行银行卡号
     *
     * @param cardType 指定卡类型
     * @return 银行卡号
     */
    public static String generateBankCardNumber(CardType cardType) {
        Bank[] allBanks = Bank.values();
        Bank randomBank = allBanks[RANDOM.nextInt(allBanks.length)];

        return generateBankCardNumber(randomBank, cardType);
    }

    /**
     * 批量生成银行卡号
     *
     * @param count 生成数量
     * @return 银行卡号列表
     */
    public static List<String> generateBankCardNumbers(int count) {
        return generateBankCardNumbers(count, null, null);
    }

    /**
     * 批量生成指定条件的银行卡号
     *
     * @param count    生成数量
     * @param bank     指定银行（null表示随机）
     * @param cardType 指定卡类型（null表示随机）
     * @return 银行卡号列表
     */
    public static List<String> generateBankCardNumbers(int count, Bank bank, CardType cardType) {
        List<String> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            if (bank != null && cardType != null) {
                cards.add(generateBankCardNumber(bank, cardType));
            } else if (bank != null) {
                cards.add(generateBankCardNumber(bank));
            } else if (cardType != null) {
                cards.add(generateBankCardNumber(cardType));
            } else {
                cards.add(generateBankCardNumber());
            }
        }

        return cards;
    }

    /**
     * 计算Luhn校验位
     *
     * @param number 不含校验位的数字
     * @return 校验位字符
     */
    private static char calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return (char) ('0' + checkDigit);
    }

    /**
     * 验证银行卡号是否符合Luhn算法
     *
     * @param cardNumber 银行卡号
     * @return 是否有效
     */
    public static boolean isValidBankCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }

        String cleanNumber = cardNumber.replaceAll("[\\s-]+", "");

        if (!cleanNumber.matches("\\d+")) {
            return false;
        }

        if (cleanNumber.length() < 13 || cleanNumber.length() > 19) {
            return false;
        }

        // 计算Luhn校验
        int sum = 0;
        boolean alternate = false;

        for (int i = cleanNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cleanNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }

    /**
     * 获取银行卡详细信息
     *
     * @param cardNumber 银行卡号
     * @return 包含银行、卡类型等信息的字符串
     */
    public static String getCardInfo(String cardNumber) {
        if (cardNumber == null) {
            return "无效卡号";
        }

        if (!isValidBankCardNumber(cardNumber)) {
            return "无效卡号（Luhn校验失败）";
        }

        Bank bank = Bank.getByCardNumber(cardNumber);
        String bankName = bank != null ? bank.getName() : "未知银行";

        int length = cardNumber.replaceAll("[\\s-]+", "").length();
        String cardType;

        if (length >= 16 && length <= 19) {
            cardType = "可能是储蓄卡";
        } else if (length >= 13 && length <= 16) {
            cardType = "可能是信用卡";
        } else {
            cardType = "未知卡类型";
        }

        return String.format("银行: %s, 卡类型: %s, 长度: %d位, 有效: 是",
                bankName, cardType, length);
    }

    /**
     * 格式化银行卡号（每4位空格分隔）
     *
     * @param cardNumber 银行卡号
     * @return 格式化后的卡号
     */
    public static String formatBankCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }

        String cleanNumber = cardNumber.replaceAll("[\\s-]+", "");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < cleanNumber.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(cleanNumber.charAt(i));
        }

        return formatted.toString();
    }

    /**
     * 生成不同银行的测试卡号
     *
     * @param banks 银行列表
     * @return 映射关系：银行 -> 卡号列表
     */
    public static Map<Bank, List<String>> generateTestCardsForBanks(Bank... banks) {
        Map<Bank, List<String>> result = new LinkedHashMap<>();

        for (Bank bank : banks) {
            List<String> cards = generateBankCardNumbers(3, bank, null);
            result.put(bank, cards);
        }

        return result;
    }
}