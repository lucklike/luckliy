package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.Namespace;

import java.util.Random;

import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.MOCK_FUNCTION_SPACE;

/**
 * Mock相关的函数
 */
@Namespace(MOCK_FUNCTION_SPACE)
public class MockFunctions {

    private static final Random random = new Random();

    // 中文姓氏
    private static final String[] SURNAMES = {
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈",
            "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许",
            "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏",
            "陶", "姜", "戚", "谢", "邹", "喻", "柏", "水", "窦", "章",
            "云", "苏", "潘", "葛", "奚", "范", "彭", "郎", "鲁", "韦",
            "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳", "鲍"
    };

    // 中文名字常用字
    private static final String[] NAME_CHARS = {
            "伟", "勇", "军", "磊", "洋", "超", "强", "鹏", "杰", "建",
            "宇", "浩", "晨", "婷", "敏", "静", "丽", "艳", "娜", "芳",
            "丹", "洁", "琳", "颖", "雪", "慧", "倩", "雯", "欣", "怡",
            "雨", "鑫", "明", "亮", "俊", "飞", "凯", "华", "平", "刚",
            "辉", "龙", "健", "博", "阳", "帅", "毅", "涛", "文", "波"
    };

    // 英文名
    private static final String[] ENGLISH_FIRST_NAMES = {
            "James", "John", "Robert", "Michael", "William", "David", "Richard", "Charles", "Joseph", "Thomas",
            "Christopher", "Daniel", "Paul", "Mark", "Donald", "George", "Kenneth", "Steven", "Edward", "Brian",
            "Mary", "Patricia", "Linda", "Barbara", "Elizabeth", "Jennifer", "Maria", "Susan", "Margaret", "Dorothy",
            "Lisa", "Nancy", "Karen", "Betty", "Helen", "Sandra", "Donna", "Carol", "Ruth", "Sharon"
    };

    // 英文姓氏
    private static final String[] ENGLISH_LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Rodriguez", "Wilson",
            "Martinez", "Anderson", "Taylor", "Thomas", "Hernandez", "Moore", "Martin", "Jackson", "Thompson", "White",
            "Lopez", "Lee", "Gonzalez", "Harris", "Clark", "Lewis", "Robinson", "Walker", "Perez", "Hall",
            "Young", "Allen", "Sanchez", "Wright", "King", "Scott", "Green", "Baker", "Adams", "Nelson"
    };

    // 邮箱域名
    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "qq.com", "163.com", "126.com", "sina.com", "sohu.com"
    };

    // 省份地区代码（前6位）
    private static final String[] AREA_CODES = {
            "110101", "110102", "110105", "110106", "110108", // 北京
            "310101", "310104", "310105", "310106", "310107", // 上海
            "440101", "440103", "440104", "440105", "440106", // 广州
            "440301", "440303", "440304", "440305", "440306", // 深圳
            "330102", "330103", "330104", "330105", "330106"  // 杭州
    };

    // 身份证校验码
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 生成随机中文姓名（2-3个字符）
     *
     * @return 随机中文姓名
     */
    @FunctionAlias("m_ch_name")
    public static String generateChineseName() {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];

        // 随机生成1-2个名字字符，确保总长度为2-3个字符
        int nameLength = random.nextInt(2) + 1; // 1或2
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < nameLength; i++) {
            nameBuilder.append(NAME_CHARS[random.nextInt(NAME_CHARS.length)]);
        }

        return surname + nameBuilder.toString();
    }

    /**
     * 生成随机电话号码
     *
     * @return 随机手机号码
     */
    @FunctionAlias("m_tel")
    public static String generatePhoneNumber() {
        // 手机号前缀
        String[] prefixes = {"13", "14", "15", "16", "17", "18", "19"};
        String prefix = prefixes[random.nextInt(prefixes.length)];

        // 生成后9位数字
        StringBuilder phoneBuilder = new StringBuilder(prefix);
        for (int i = 0; i < 9; i++) {
            phoneBuilder.append(random.nextInt(10));
        }

        return phoneBuilder.toString();
    }

    /**
     * 生成随机邮箱地址
     *
     * @return 随机邮箱地址
     */
    @FunctionAlias("m_email")
    public static String generateEmail() {
        // 生成用户名部分（6-12位字母数字组合）
        int usernameLength = random.nextInt(7) + 6;
        StringBuilder usernameBuilder = new StringBuilder();

        for (int i = 0; i < usernameLength; i++) {
            if (random.nextBoolean()) {
                // 生成字母
                char c = (char) (random.nextBoolean() ?
                        random.nextInt(26) + 'a' : random.nextInt(26) + 'A');
                usernameBuilder.append(c);
            } else {
                // 生成数字
                usernameBuilder.append(random.nextInt(10));
            }
        }

        String domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
        return usernameBuilder.toString() + "@" + domain;
    }

    /**
     * 生成随机身份证号码
     *
     * @return 随机身份证号码（18位）
     */
    @FunctionAlias("m_id_card")
    public static String generateIdCard() {
        StringBuilder idCardBuilder = new StringBuilder();

        // 1. 前6位：地区码
        idCardBuilder.append(AREA_CODES[random.nextInt(AREA_CODES.length)]);

        // 2. 中间8位：出生日期（1960-2005年）
        int year = random.nextInt(46) + 1960; // 1960-2005
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1; // 简单处理，避免2月30日等情况

        idCardBuilder.append(String.format("%04d%02d%02d", year, month, day));

        // 3. 顺序码：3位
        idCardBuilder.append(String.format("%03d", random.nextInt(1000)));

        // 4. 校验码：1位
        String idCard17 = idCardBuilder.toString();
        char checkCode = calculateCheckCode(idCard17);
        idCardBuilder.append(checkCode);

        return idCardBuilder.toString();
    }


    /**
     * 生成随机英文名
     *
     * @return 随机英文名（格式：FirstName LastName）
     */
    @FunctionAlias("m_en_name")
    public static String generateEnglishName() {
        String firstName = ENGLISH_FIRST_NAMES[random.nextInt(ENGLISH_FIRST_NAMES.length)];
        String lastName = ENGLISH_LAST_NAMES[random.nextInt(ENGLISH_LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    /**
     * 生成随机日期（格式：xxxx年xx月xx日）
     *
     * @return 随机日期字符串
     */
    @FunctionAlias("m_date")
    public static String generateRandomDate() {
        // 生成1980-2023年之间的随机日期
        int year = random.nextInt(44) + 1980;
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1; // 简单处理，避免2月30日等情况

        return String.format("%d年%02d月%02d日", year, month, day);
    }

    /**
     * 生成指定范围内的随机日期
     *
     * @param startYear 开始年份
     * @param endYear   结束年份
     * @return 随机日期字符串
     */
    @FunctionAlias("m_range_date")
    public static String generateRandomDate(int startYear, int endYear) {
        if (startYear > endYear) {
            throw new IllegalArgumentException("开始年份不能大于结束年份");
        }

        int year = random.nextInt(endYear - startYear + 1) + startYear;
        int month = random.nextInt(12) + 1;
        int day = random.nextInt(28) + 1;

        return String.format("%d年%02d月%02d日", year, month, day);
    }


    /**
     * 计算身份证校验码
     *
     * @param idCard17 前17位身份证号码
     * @return 校验码
     */
    @FunctionFilter
    private static char calculateCheckCode(String idCard17) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        int sum = 0;

        for (int i = 0; i < 17; i++) {
            sum += (idCard17.charAt(i) - '0') * weights[i];
        }

        return CHECK_CODES[sum % 11];
    }
}
