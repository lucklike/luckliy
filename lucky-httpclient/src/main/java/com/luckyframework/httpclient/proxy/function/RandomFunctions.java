package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;
import com.luckyframework.httpclient.proxy.spel.Namespace;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.RANDOM_FUNCTION_SPACE;

/**
 * 随机相关的函数
 *
 * @author DeepSeek
 */
@Namespace(RANDOM_FUNCTION_SPACE)
public class RandomFunctions {

    // 随机种子
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


    // 地址相关数据
    private static final String[] PROVINCES = {
            "北京市", "上海市", "天津市", "重庆市", "河北省", "山西省", "辽宁省",
            "吉林省", "黑龙江省", "江苏省", "浙江省", "安徽省", "福建省", "江西省",
            "山东省", "河南省", "湖北省", "湖南省", "广东省", "海南省", "四川省",
            "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "香港特别行政区", "澳门特别行政区"
    };

    private static final String[] CITIES = {
            "朝阳区", "海淀区", "东城区", "西城区", "浦东新区", "黄浦区", "徐汇区",
            "天河区", "福田区", "南山区", "江岸区", "武昌区", "锦江区", "青羊区",
            "秦淮区", "鼓楼区", "西湖区", "下城区", "历下区", "市中区", "河西区"
    };

    private static final String[] STREETS = {
            "中山路", "解放路", "人民路", "建设路", "和平路", "新华路", "胜利路",
            "延安路", "东风路", "滨海路", "长江路", "黄河路", "南京路", "北京路",
            "上海路", "广州路", "深圳路", "成都路", "武汉路", "西安路"
    };

    private static final String[] COMMUNITY_SUFFIXES = {
            "小区", "花园", "公寓", "大厦", "广场", "苑", "城", "湾", "里", "巷"
    };

    private static final String[] BUILDING_NUMBERS = {
            "1号", "2号", "3号", "5号", "10号", "15号", "20号", "25号", "30号",
            "甲1号", "乙2号", "丙3号", "A座", "B座", "C座", "D座"
    };

    private static final String[] ROOM_NUMBERS = {
            "101室", "201室", "301室", "401室", "501室", "102室", "202室", "302室",
            "单元201", "单元301", "单元401", "A栋101", "B栋202", "C栋303"
    };

    // 公司相关数据
    private static final String[] COMPANY_TYPES = {
            "科技有限公司", "信息技术有限公司", "软件有限公司", "网络科技有限公司",
            "电子科技有限公司", "智能科技有限公司", "数码科技有限公司", "系统工程有限公司",
            "咨询有限公司", "贸易有限公司", "实业有限公司", "发展有限公司", "集团股份有限公司"
    };

    private static final String[] COMPANY_PREFIXES = {
            "东方", "华夏", "中兴", "华为", "联想", "腾讯", "阿里巴巴", "百度",
            "京东", "小米", "字节跳动", "美团", "滴滴", "拼多多", "网易", "搜狐",
            "新浪", "金山", "用友", "金蝶", "神州", "长城", "海尔", "格力", "美的",
            "苏宁", "国美", "顺丰", "圆通", "申通", "中通", "韵达"
    };

    private static final String[] INDUSTRIES = {
            "互联网", "电子商务", "软件开发", "人工智能", "大数据", "云计算", "物联网",
            "区块链", "金融科技", "智能制造", "新能源", "生物医药", "教育培训",
            "文化传媒", "旅游服务", "房地产", "建筑工程", "物流运输", "零售批发"
    };

    private static final String[] COMPANY_SUFFIXES = {
            "(中国)有限公司", "有限公司", "有限责任公司", "股份有限公司", "集团"
    };


    // URL相关数据
    private static final String[] PROTOCOLS = {"http://", "https://"};
    private static final String[] DOMAINS = {
            "www", "blog", "shop", "news", "mail", "drive", "docs", "photos",
            "video", "music", "map", "translate", "search", "cloud", "api"
    };

    private static final String[] DOMAIN_SUFFIXES = {
            "com", "cn", "net", "org", "edu", "gov", "io", "ai", "tech",
            "store", "online", "site", "xyz", "top", "club", "info", "biz"
    };

    private static final String[] SUBDOMAINS = {
            "en", "zh", "jp", "kr", "de", "fr", "es", "ru", "it",
            "us", "uk", "ca", "au", "in", "br", "mx", "sg", "hk",
            "m", "mobile", "app", "static", "cdn", "img", "video", "download"
    };

    private static final String[] URL_PATHS = {
            "index", "home", "about", "contact", "products", "services",
            "blog", "news", "article", "post", "page", "category",
            "user", "profile", "account", "login", "register", "search",
            "download", "upload", "file", "document", "image", "video",
            "api", "admin", "dashboard", "settings", "help", "faq"
    };

    private static final String[] FILE_EXTENSIONS = {
            "html", "php", "jsp", "aspx", "do", "action",
            "json", "xml", "txt", "pdf", "doc", "xls"
    };

    private static final String[] URL_PARAMS = {
            "id", "page", "page_size", "sort", "order", "keyword",
            "category", "type", "status", "date", "year", "month",
            "user", "author", "tag", "search", "q", "filter"
    };

    // 本地文件相关数据
    private static final String[] DRIVES = {"C:", "D:", "E:", "F:", "Z:"};

    private static final String[] FOLDER_NAMES = {
            "Documents", "Downloads", "Desktop", "Pictures", "Music",
            "Videos", "Projects", "Work", "Study", "Temp", "Backup",
            "Config", "Logs", "Data", "Database", "Cache", "Trash",
            "My Documents", "My Music", "My Pictures", "My Videos",
            "Program Files", "Program Files (x86)", "Windows", "System32"
    };

    private static final String[] FILE_NAMES = {
            "report", "document", "presentation", "spreadsheet", "budget",
            "plan", "schedule", "meeting", "notes", "todo", "shopping",
            "invoice", "receipt", "contract", "agreement", "proposal",
            "resume", "cv", "letter", "application", "form",
            "photo", "image", "picture", "screenshot", "wallpaper",
            "video", "movie", "clip", "recording", "audio",
            "song", "music", "podcast", "book", "novel", "article"
    };

    private static final String[] EXTENSIONS = {
            // 文档类
            "txt", "doc", "docx", "pdf", "rtf", "odt", "md",
            // 表格类
            "xls", "xlsx", "csv", "ods",
            // 演示文稿
            "ppt", "pptx", "odp",
            // 图片类
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "ico", "webp",
            // 视频类
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
            // 音频类
            "mp3", "wav", "flac", "aac", "ogg", "m4a",
            // 压缩文件
            "zip", "rar", "7z", "tar", "gz",
            // 代码文件
            "java", "py", "js", "html", "css", "cpp", "c", "h", "cs",
            // 配置文件
            "json", "xml", "yml", "yaml", "ini", "cfg", "conf",
            // 其他
            "exe", "msi", "apk", "dmg", "iso", "log", "bak"
    };
    // 身份证校验码
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};


    /**
     * 产生一个随机的UUID
     *
     * @return UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 【英文大写】产生一个随机的UUID
     *
     * @return UUID
     */
    public static String UUID() {
        return uuid().toUpperCase();
    }

    /**
     * 产生一个随机的NanoId
     *
     * @return NanoId
     */
    public static String nanoid(Integer... length) {
        int len = ContainerUtils.isEmptyArray(length) ? 21 : length[0];
        return NanoIdUtils.randomNanoId(len);
    }

    /**
     * 生成随机中文姓名（2-3个字符）
     *
     * @return 随机中文姓名
     */
    @FunctionAlias("random_ch_name")
    public static String randomChName() {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];

        // 随机生成1-2个名字字符，确保总长度为2-3个字符
        int nameLength = random.nextInt(2) + 1; // 1或2
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < nameLength; i++) {
            nameBuilder.append(NAME_CHARS[random.nextInt(NAME_CHARS.length)]);
        }

        return surname + nameBuilder;
    }

    /**
     * 生成随机电话号码
     *
     * @return 随机手机号码
     */
    @FunctionAlias("random_tel")
    public static String randomTel() {
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
    @FunctionAlias("random_email")
    public static String randomEmail() {
        String domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
        return randomEnName().replace(" ", "-").toLowerCase() + "@" + domain;
    }

    /**
     * 生成随机身份证号码
     *
     * @return 随机身份证号码（18位）
     */
    @FunctionAlias("random_id_card")
    public static String randomIdCard() {
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
     * 随机生成银行卡号
     *
     * @return 银行卡号
     */
    @FunctionAlias("random_bank_card")
    public static String randomBankCard() {
        return BankCardGenerator.generateBankCardNumber();
    }


    /**
     * 生成随机英文名
     *
     * @return 随机英文名（格式：FirstName LastName）
     */
    @FunctionAlias("random_en_name")
    public static String randomEnName() {
        String firstName = ENGLISH_FIRST_NAMES[random.nextInt(ENGLISH_FIRST_NAMES.length)];
        String lastName = ENGLISH_LAST_NAMES[random.nextInt(ENGLISH_LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    /**
     * 生成随机日期
     *
     * @return 生成随机日期
     */
    @FunctionAlias("random_date")
    public static Date randomDate() {
        // 生成1980-今年之间的随机日期
        int year = randomInt(1980, Year.now().getValue());
        int month = randomInt(1, 12);
        int day = randomInt(1, 28);

        int hour = randomInt(0, 23);
        int minute = randomInt(0, 59);
        int second = randomInt(0, 59);

        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }


    /**
     * 随机生成指定格式的时间字符串，默认格式为：yyyy-MM-dd HH:mm:ss
     *
     * @param formats 格式字符串
     * @return 指定格式的时间字符串
     */
    @FunctionAlias("random_date_str")
    public static String randomDateStr(String... formats) {
        String format = formats.length > 0 ? formats[0] : "yyyy-MM-dd HH:mm:ss";
        return new SimpleDateFormat(format).format(randomDate());
    }

    /**
     * 生成一个指定范围内的随机整数
     * <pre>
     *     1.nums传入 0 个参数时
     *        -> random.nextInt()
     *     2.nums传入 1 个参数时
     *        -> 随机返回一个[0 - nums]的数
     *     3.nums传入 2 个参数时
     *        -> 随机返回[nums[0] -nums[1]]之间的数
     *     4.nums传入 n 个参数时
     *        -> 随机返回这 n 个数中其中的一个
     * </pre>
     *
     * @param nums 随机范围
     * @return 指定范围内的随机整数
     */
    @FunctionAlias("random_int")
    public static int randomInt(int... nums) {
        if (nums.length == 0) {
            return random.nextInt();
        }
        if (nums.length == 1) {
            return random.nextInt(nums[0] + 1);
        }
        if (nums.length == 2) {
            return random.nextInt(nums[1] - nums[0] + 1) + nums[0];
        }
        return nums[randomInt(nums.length - 1)];
    }

    /**
     * 随机生成一个 boolean 值
     *
     * @return 随机生成一个 boolean 值
     */
    @FunctionAlias("random_boolean")
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    /**
     * 随机生成一个 long 值
     *
     * @return 随机生成一个 long 值
     */
    @FunctionAlias("random_long")
    public static long randomLong() {
        return random.nextLong();
    }

    /**
     * 随机生成一个 double 值
     *
     * @return 随机生成一个 double 值
     */
    @FunctionAlias("random_double")
    public static double randomDouble() {
        return random.nextDouble();
    }

    /**
     * 随机生成一个 float 值
     *
     * @return 随机生成一个 float 值
     */
    @FunctionAlias("random_float")
    public static float randomFloat() {
        return random.nextFloat();
    }

    /**
     * 随机生成一个 IP 地址
     *
     * @return 随机IP地址
     */
    @FunctionAlias("random_ip")
    public static String randomIp() {
        return String.format("%d.%d.%d.%d",
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
        );
    }

    /**
     * 生成随机的端口号
     *
     * @return 随机端口号
     */
    @FunctionAlias("random_port")
    public static int randomPort() {
        return randomInt(1024, 65535);
    }

    /**
     * 生成随机性别
     *
     * @return 随机性别
     */
    @FunctionAlias("random_sex")
    public static String randomSex() {
        return randomBoolean() ? "男" : "女";
    }

    /**
     * 随机生成相对完善的地址信息
     *
     * @return 完整的地址字符串
     */
    @FunctionAlias("random_address")
    public static String randomAddress() {
        StringBuilder address = new StringBuilder();

        // 省份/直辖市
        address.append(PROVINCES[random.nextInt(PROVINCES.length)]);

        // 城市/区
        address.append(CITIES[random.nextInt(CITIES.length)]);

        // 街道
        address.append(STREETS[random.nextInt(STREETS.length)]);

        // 门牌号
        address.append(BUILDING_NUMBERS[random.nextInt(BUILDING_NUMBERS.length)]);

        // 社区/小区（70%概率添加）
        if (random.nextDouble() < 0.7) {
            String communityPrefix = getRandomChineseString(2, 4);
            address.append(communityPrefix)
                    .append(COMMUNITY_SUFFIXES[random.nextInt(COMMUNITY_SUFFIXES.length)]);
        }

        // 房间号（50%概率添加）
        if (random.nextDouble() < 0.5) {
            address.append(ROOM_NUMBERS[random.nextInt(ROOM_NUMBERS.length)]);
        }

        return address.toString();
    }

    /**
     * 随机生成相对完整的公司信息
     *
     * @return 完整的公司信息字符串
     */
    @FunctionAlias("random_company")
    public static String randomCompany() {
        StringBuilder companyInfo = new StringBuilder();

        // 公司类型：前缀+行业+类型+后缀的组合方式有多种

        int pattern = random.nextInt(4);
        switch (pattern) {
            case 0:
                // 模式1: 前缀 + 类型
                companyInfo.append(COMPANY_PREFIXES[random.nextInt(COMPANY_PREFIXES.length)])
                        .append(COMPANY_TYPES[random.nextInt(COMPANY_TYPES.length)]);
                break;

            case 1:
                // 模式2: 城市 + 行业 + 类型
                String city = CITIES[random.nextInt(CITIES.length)].replace("区", "");
                companyInfo.append(city)
                        .append(INDUSTRIES[random.nextInt(INDUSTRIES.length)])
                        .append(COMPANY_TYPES[random.nextInt(COMPANY_TYPES.length)]);
                break;

            case 2:
                // 模式3: 前缀 + 行业 + 类型
                companyInfo.append(COMPANY_PREFIXES[random.nextInt(COMPANY_PREFIXES.length)])
                        .append(INDUSTRIES[random.nextInt(INDUSTRIES.length)])
                        .append(COMPANY_TYPES[random.nextInt(COMPANY_TYPES.length)]);
                break;

            case 3:
                // 模式4: 随机中文名 + 类型 + 后缀
                String randomName = getRandomChineseString(3, 6);
                companyInfo.append(randomName)
                        .append(COMPANY_TYPES[random.nextInt(COMPANY_TYPES.length)])
                        .append(COMPANY_SUFFIXES[random.nextInt(COMPANY_SUFFIXES.length)]);
                break;
        }

        return companyInfo.toString();
    }

    /**
     * 随机生成URL地址
     *
     * @return 完整的URL字符串
     */
    @FunctionAlias("random_url")
    public static String randomUrl() {
        StringBuilder url = new StringBuilder();

        // 协议
        url.append(PROTOCOLS[random.nextInt(PROTOCOLS.length)]);

        // 子域名（50%概率添加）
        if (random.nextDouble() < 0.5) {
            url.append(SUBDOMAINS[random.nextInt(SUBDOMAINS.length)])
                    .append(".");
        }

        // 主域名
        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        url.append(domain)
                .append(".");

        // 域名后缀（可能有多级，如.co.uk）
        int suffixPattern = random.nextInt(3);
        switch (suffixPattern) {
            case 0:
                // 单级后缀，如 .com
                url.append(DOMAIN_SUFFIXES[random.nextInt(DOMAIN_SUFFIXES.length)]);
                break;
            case 1:
                // 两级后缀，如 .com.cn
                url.append(DOMAIN_SUFFIXES[random.nextInt(DOMAIN_SUFFIXES.length)])
                        .append(".")
                        .append(DOMAIN_SUFFIXES[random.nextInt(DOMAIN_SUFFIXES.length)]);
                break;
            case 2:
                // 国家域名，如 .co.uk
                String[] countryDomains = {"co.uk", "com.au", "com.br", "co.jp", "co.kr", "com.sg", "com.hk"};
                url.append(countryDomains[random.nextInt(countryDomains.length)]);
                break;
        }

        // 路径（至少一级）
        int pathLevels = 1 + random.nextInt(3); // 1-3级路径
        for (int i = 0; i < pathLevels; i++) {
            url.append("/")
                    .append(URL_PATHS[random.nextInt(URL_PATHS.length)]);

            // 40%概率在路径中添加数字ID
            if (random.nextDouble() < 0.4) {
                url.append(random.nextInt(10000));
            }
        }

        // 30%概率添加文件扩展名
        if (random.nextDouble() < 0.3) {
            url.append(".")
                    .append(FILE_EXTENSIONS[random.nextInt(FILE_EXTENSIONS.length)]);
        }

        // 查询参数（40%概率添加）
        if (random.nextDouble() < 0.4) {
            int paramCount = 1 + random.nextInt(3); // 1-3个参数
            url.append("?");

            for (int i = 0; i < paramCount; i++) {
                if (i > 0) {
                    url.append("&");
                }
                url.append(URL_PARAMS[random.nextInt(URL_PARAMS.length)])
                        .append("=");

                // 参数值
                int valueType = random.nextInt(3);
                switch (valueType) {
                    case 0:
                        url.append(random.nextInt(1000));
                        break;
                    case 1:
                        url.append(getRandomString(3, 10, false));
                        break;
                    case 2:
                        url.append(URL_PATHS[random.nextInt(URL_PATHS.length)]);
                        break;
                }
            }
        }

        // 10%概率添加锚点
        if (random.nextDouble() < 0.1) {
            url.append("#")
                    .append(getRandomString(5, 15, false).toLowerCase());
        }

        return url.toString();
    }


    /**
     * 随机生成本地文件地址
     *
     * @return 完整的本地文件路径字符串
     */
    @FunctionAlias("random_file_path")
    public static String randomFilePath() {
        StringBuilder path = new StringBuilder();

        // 操作系统判断（模拟不同系统的路径格式）
        int osType = random.nextInt(3);

        switch (osType) {
            case 0: // Windows
                path.append(DRIVES[random.nextInt(DRIVES.length)])
                        .append("\\");
                break;

            case 1: // Linux/Mac
                path.append("/");
                // 可能的根目录
                String[] linuxRoots = {"home", "usr", "var", "etc", "tmp", "opt", "mnt"};
                if (random.nextDouble() < 0.7) {
                    path.append(linuxRoots[random.nextInt(linuxRoots.length)])
                            .append("/");
                }
                break;

            case 2: // Unix风格路径（带用户目录）
                String[] users = {"user", "admin", "guest", "test", "developer", "john", "jane"};
                path.append("/Users/")
                        .append(users[random.nextInt(users.length)])
                        .append("/");
                break;
        }

        // 文件夹层级（2-4级）
        int folderLevels = 2 + random.nextInt(3);
        for (int i = 0; i < folderLevels; i++) {
            // 有时会添加带版本或日期的文件夹
            if (random.nextDouble() < 0.3) {
                path.append(FOLDER_NAMES[random.nextInt(FOLDER_NAMES.length)])
                        .append("_")
                        .append(getVersionOrDate())
                        .append(getPathSeparator(osType));
            } else {
                path.append(FOLDER_NAMES[random.nextInt(FOLDER_NAMES.length)])
                        .append(getPathSeparator(osType));
            }
        }

        // 文件名（可能带版本或日期）
        String fileName = FILE_NAMES[random.nextInt(FILE_NAMES.length)];
        if (random.nextDouble() < 0.4) {
            fileName += "_" + getVersionOrDate();
        }

        // 可能的编号
        if (random.nextDouble() < 0.3) {
            fileName += "_" + (random.nextInt(20) + 1);
        }

        path.append(fileName);

        // 扩展名
        String extension = EXTENSIONS[random.nextInt(EXTENSIONS.length)];
        path.append(".")
                .append(extension);

        return path.toString();
    }

    /**
     * 获取路径分隔符
     */
    @FunctionFilter
    private static String getPathSeparator(int osType) {
        return osType == 0 ? "\\" : "/";
    }

    /**
     * 生成版本号或日期字符串
     */
    @FunctionFilter
    private static String getVersionOrDate() {
        int type = random.nextInt(3);
        switch (type) {
            case 0: // 版本号
                return "v" + random.nextInt(5) + "." + random.nextInt(10);
            case 1: // 日期
                int year = 2020 + random.nextInt(5);
                int month = 1 + random.nextInt(12);
                int day = 1 + random.nextInt(28);
                return String.format("%04d%02d%02d", year, month, day);
            case 2: // 时间戳
                return "2024" + (1000 + random.nextInt(9000));
            default:
                return "backup";
        }
    }

    /**
     * 生成随机字符串
     */
    @FunctionFilter
    private static String getRandomString(int minLength, int maxLength, boolean includeNumbers) {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        if (includeNumbers) {
            chars += "0123456789";
        }

        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    /**
     * 生成随机中文字符串
     *
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 随机中文字符串
     */
    @FunctionFilter
    private static String getRandomChineseString(int minLength, int maxLength) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder sb = new StringBuilder();

        // 常用汉字Unicode范围：0x4e00-0x9fa5
        for (int i = 0; i < length; i++) {
            char ch = (char) (0x4e00 + random.nextInt(0x9fa5 - 0x4e00 + 1));
            sb.append(ch);
        }

        return sb.toString();
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
