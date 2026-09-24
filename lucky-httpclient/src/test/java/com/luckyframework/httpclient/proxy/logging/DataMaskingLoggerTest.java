package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.logging.support.DataMaskingTestApi;
import com.luckyframework.httpclient.proxy.logging.support.LogMemoryAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 日志脱敏场景集成测试
 * <pre>
 * 测试思路：
 *   1.通过 {@link DataMaskingTestApi}（{@code @Logger(handlerClass = BeautifulLoggerPrintHandler.class)}）
 *      触发真实的日志打印与脱敏链路，接口响应由 {@code @Mock} 模拟，不会发出真实网络请求
 *   2.将 {@link LogMemoryAppender} 挂载到 BeautifulLoggerPrintHandler 对应的Logger上，
 *      收集真实打印的日志文本（含ANSI着色、JSON美化等完整的日志后处理）
 *   3.断言：日志中的敏感数据被正确脱敏、非敏感内容原样保留、接口返回值不受脱敏影响
 *
 * 场景覆盖：
 *   1.URL查询参数脱敏（GET参数）
 *   2.JSON响应字段脱敏 + JWT裸值兜底
 *   3.静态JSON请求体脱敏 + 独立{@code @Masker}注解隐式启用脱敏
 *   4.text/plain自由文本裸值脱敏 + maskResponse表达式显式启用
 *   5.中文键名脱敏 + JSON数组元素脱敏 + 容器值跳过
 *   6.转义JSON（JSON字符串被再次序列化）内嵌字段脱敏
 *   7.请求头传参脱敏（动态@Header参数 + 静态@StaticHeader）
 *   8.XML请求体/响应体按标签名脱敏
 *   9.MultipartForm文本字段按字段名脱敏 + 未配置字段的裸值兜底
 *   10.Mock响应头脱敏（X-Auth-Token / Set-Cookie）
 *   11.XML内嵌JSON脱敏（容器值跳过 + JSON工序进入内部按键名脱敏）
 *   12.JSON内嵌XML字符串脱敏（XML工序按标签名脱敏 + 裸值兜底）
 *   13.JSON内嵌JSON字符串脱敏（转义JSON键值对模式 + 裸值兜底）
 *   14.multipart字段值为JSON/XML/k=v字符串时的兜底脱敏
 *   15.请求头中的JSON/XML/k=v数据脱敏
 *   16.响应头中的JSON/XML/k=v数据脱敏
 * </pre>
 */
public class DataMaskingLoggerTest {

    private static LogMemoryAppender appender;
    private static org.apache.logging.log4j.core.Logger coreLogger;
    private static LoggerConfig targetLoggerConfig;

    private DataMaskingTestApi api;

    @BeforeClass
    public static void setUpClass() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        coreLogger = context.getLogger(BeautifulLoggerPrintHandler.class.getName());
        coreLogger.setLevel(Level.INFO);

        // 【注意】此处不能使用 coreLogger.addAppender(appender)：
        // log4j2 会为配置文件中不存在的logger名动态创建独立的LoggerConfig，
        // 且其 additivity 复制自最近祖先（root 的 additivity 字段值为 false），
        // 导致该logger的日志事件不再向 root 传播，root 上的 ConsoleAppender
        // 将收不到日志（控制台看不到任何输出）。
        // 正确做法：直接在该logger当前绑定的LoggerConfig（未显式配置时即root）上挂载Appender，
        // 使日志既能正常输出到控制台，又能被内存Appender收集用于断言。
        targetLoggerConfig = configuration.getLoggerConfig(BeautifulLoggerPrintHandler.class.getName());

        appender = new LogMemoryAppender("DataMaskingTestAppender");
        appender.start();
        targetLoggerConfig.addAppender(appender, null, null);
    }

    @AfterClass
    public static void tearDownClass() {
        targetLoggerConfig.removeAppender(appender.getName());
        appender.stop();
        // 恢复级别继承（不影响其他测试）
        coreLogger.setLevel(null);
    }

    @Before
    public void setUp() {
        appender.clear();

        // 全局脱敏配置隔离：清理其他测试可能残留的配置，并开启常用裸值脱敏
        // （手机号/身份证/邮箱/Basic认证/JWT，用于无键名结构的自由文本场景兜底）
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
        DataMasker.enableCommonValueMaskers();

        api = new HttpClientProxyObjectFactory().getProxyObject(DataMaskingTestApi.class);
    }

    @After
    public void tearDown() {
        DataMasker.clearMaskers();
        DataMasker.clearValueMaskers();
    }

    /**
     * 请求日志文本（按日志标题中的 REQUEST 过滤）
     */
    private String requestLogText() {
        return appender.textContaining("REQUEST");
    }

    /**
     * 响应日志文本（按日志标题中的 RESPONSE 过滤）
     */
    private String responseLogText() {
        return appender.textContaining("RESPONSE");
    }

    /**
     * 场景1：URL查询参数脱敏 + JSON响应字段脱敏 + JWT裸值兜底
     */
    @Test
    public void loginShouldMaskQueryParamsAndJsonResponseAndBareJwt() {
        String result = api.login("13812345678", "Abc123456");

        // 1.接口返回值不受脱敏影响（脱敏只作用于日志副本，不改变业务数据）
        assertTrue("登录接口返回值应保留原始手机号", result.contains("13812345678"));
        assertTrue("登录接口返回值应保留非敏感业务数据", result.contains("登录成功"));

        // 2.请求日志：URL查询参数被脱敏
        String requestLog = requestLogText();
        assertTrue("请求日志应打印URL参数", requestLog.contains("phone=138****5678"));
        assertTrue("URL中的密码参数应被完全脱敏", requestLog.contains("password=********"));
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("请求日志不应出现密码原始值", requestLog.contains("Abc123456"));

        // 3.响应日志：JSON字段被键匹配脱敏
        String responseLog = responseLogText();
        assertTrue("响应中的手机号应被脱敏", responseLog.contains("138****5678"));
        assertTrue("响应中的邮箱应被脱敏", responseLog.contains("zha***@example.com"));
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("响应日志不应出现邮箱原始值", responseLog.contains("zhangsan@example.com"));

        // 4.响应日志：JWT未配置键脱敏器，由全局裸值脱敏器兜底
        assertFalse("响应日志不应出现JWT原始值", responseLog.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertTrue("JWT应被完全脱敏为占位符", responseLog.contains("\"********\""));
    }

    /**
     * 场景2：静态JSON请求体脱敏 + 独立@Masker注解隐式启用脱敏
     */
    @Test
    public void createUserShouldMaskStaticJsonRequestBodyWithStandaloneMasker() {
        String result = api.createUser();
        assertTrue("接口返回值不受脱敏影响", result.contains("true"));

        String requestLog = requestLogText();

        // 1.姓名：仅配置了独立@Masker(userName -> NAME)，未标注@PrintLog也应隐式启用脱敏
        assertTrue("请求体中的姓名应被脱敏", requestLog.contains("张**"));
        assertFalse("请求日志不应出现姓名原始值", requestLog.contains("张三"));

        // 2.密码：完全脱敏（值被替换为带引号的8个星号，与身份证的部分脱敏结果可区分）
        assertTrue("请求体中的密码应被完全脱敏", requestLog.contains("\"********\""));
        assertFalse("请求日志不应出现密码原始值", requestLog.contains("Abc123456"));

        // 3.身份证：保留前6后4
        assertTrue("请求体中的身份证应被脱敏", requestLog.contains("510123********1234"));
        assertFalse("请求日志不应出现身份证原始值", requestLog.contains("510123199001011234"));

        // 4.手机号未配置键脱敏器，由全局裸值脱敏器兜底
        assertTrue("请求体中的手机号应由裸值脱敏器兜底脱敏", requestLog.contains("138****5678"));
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
    }

    /**
     * 场景3：text/plain自由文本裸值脱敏 + maskResponse表达式显式启用
     */
    @Test
    public void sendSmsShouldMaskBareValuesInFreeTextResponse() {
        String result = api.sendSms();
        assertTrue("非敏感内容应保留在返回值中", result.contains("您的验证码为862693"));

        String responseLog = responseLogText();

        // 自由文本（无键名结构）中的手机号/邮箱由裸值脱敏器兜底脱敏
        assertTrue("响应文本中的手机号应被脱敏", responseLog.contains("138****5678"));
        assertTrue("响应文本中的邮箱应被脱敏", responseLog.contains("kef***@example.com"));
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("响应日志不应出现邮箱原始值", responseLog.contains("kefu@example.com"));

        // 与前缀中文紧邻的敏感值同样被脱敏，非敏感内容原样保留
        assertTrue("非敏感内容应原样保留", responseLog.contains("您的验证码为862693"));
    }

    /**
     * 场景4：中文键名脱敏 + JSON数组元素脱敏 + 容器值跳过
     */
    @Test
    public void usersShouldMaskChineseKeyAndArrayElementsAndSkipContainer() {
        String result = api.users();
        assertTrue("接口返回值不受脱敏影响", result.contains("13812345678"));

        String responseLog = responseLogText();

        // 1.中文键名（手机号）对应的值被脱敏
        assertFalse("中文键名对应的值应被脱敏", responseLog.contains("13812345678"));
        // 2.数组元素内的phone字段逐一被脱敏
        assertFalse("数组元素中的手机号应被脱敏", responseLog.contains("13998887777"));
        assertFalse("数组元素中的手机号应被脱敏", responseLog.contains("13611112222"));
        // 3.data是容器值（数组），跳过整体脱敏：JSON结构与内部键名应当保留
        assertTrue("容器值不应被整体脱敏，数组内的键名应保留", responseLog.contains("phone"));
        assertTrue("容器值不应被整体脱敏，非敏感字段应保留", responseLog.contains("total"));
        // 4.完全脱敏占位符出现（手机号/phone字段的FULL脱敏结果）
        assertTrue("完全脱敏的占位符应出现", responseLog.contains("\"********\""));
    }

    /**
     * 场景5：转义JSON（JSON字符串被再次序列化）内嵌字段脱敏
     */
    @Test
    public void rawJsonShouldMaskEscapedJsonFields() {
        String result = api.rawJson();
        assertTrue("接口返回值不受脱敏影响", result.contains("13812345678"));

        String responseLog = responseLogText();

        assertFalse("转义JSON中的手机号应被脱敏", responseLog.contains("13812345678"));
        assertTrue("转义JSON结构应保留且值被完全脱敏", responseLog.contains("\\\"phone\\\":\\\"********\\\""));
    }

    /**
     * 场景6：请求头传参脱敏（动态@Header参数 + 静态@StaticHeader）
     */
    @Test
    public void headerPassingShouldMaskRequestHeaders() {
        String result = api.headerPassing("tk_live_9f8e7d6c5b4a3210");
        assertTrue("接口返回值不受脱敏影响", result.contains("\"code\":200"));

        String requestLog = requestLogText();

        // 1.请求头名称在日志中保留（名称带ANSI着色，值紧随其后）
        assertTrue("请求日志应打印动态请求头名称", requestLog.contains("X-Api-Token"));
        assertTrue("请求日志应打印静态请求头名称", requestLog.contains("X-App-Id"));

        // 2.动态@Header参数：完全脱敏
        assertFalse("动态请求头的令牌原始值不应出现", requestLog.contains("tk_live_9f8e7d6c5b4a3210"));
        assertTrue("动态请求头的令牌应被完全脱敏", requestLog.contains("********"));

        // 3.静态@StaticHeader：保留前4后4
        assertTrue("静态请求头应被保留前4后4脱敏", requestLog.contains("app-********5b4a"));
        assertFalse("静态请求头的原始值不应出现", requestLog.contains("app-9f8e7d6c5b4a"));
    }

    /**
     * 场景7：XML请求体/响应体按标签名脱敏
     */
    @Test
    public void xmlBodyShouldMaskXmlRequestAndResponse() {
        String result = api.xmlBody();
        assertTrue("接口返回值不受脱敏影响", result.contains("<code>0</code>"));

        // 1.请求日志：XML请求体按标签名脱敏，非敏感标签原样保留
        String requestLog = requestLogText();
        assertTrue("XML请求体中的姓名应被脱敏", requestLog.contains("<userName>张**</userName>"));
        assertTrue("XML请求体中的手机号应被脱敏", requestLog.contains("<phone>138****5678</phone>"));
        assertTrue("XML请求体中的身份证应被脱敏", requestLog.contains("<idCardNum>510123********1234</idCardNum>"));
        assertFalse("XML请求日志不应出现姓名原始值", requestLog.contains("张三"));
        assertFalse("XML请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("XML请求日志不应出现身份证原始值", requestLog.contains("510123199001011234"));

        // 2.响应日志：XML响应体按标签名脱敏
        String responseLog = responseLogText();
        assertTrue("XML响应体中的手机号应被脱敏", responseLog.contains("<phone>138****5678</phone>"));
        assertTrue("XML响应体中的邮箱应被脱敏", responseLog.contains("<email>zha***@example.com</email>"));
        assertTrue("非敏感标签原样保留", responseLog.contains("<code>0</code>"));
        assertFalse("XML响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("XML响应日志不应出现邮箱原始值", responseLog.contains("zhangsan@example.com"));
    }

    /**
     * 场景8：MultipartForm文本字段按字段名脱敏 + 未配置字段的裸值兜底
     */
    @Test
    public void multipartFormShouldMaskTextFields() {
        String result = api.uploadForm();
        assertTrue("接口返回值不受脱敏影响", result.contains("true"));

        String requestLog = requestLogText();

        // 1.multipart结构完整打印，字段名（name="xxx"）原样保留
        assertTrue("请求日志应打印multipart的Content-Type", requestLog.contains("multipart/form-data; boundary=LuckyBoundary"));
        assertTrue("字段名应原样保留", requestLog.contains("form-data; name=\"phone\""));
        assertTrue("字段名应原样保留", requestLog.contains("form-data; name=\"secretKey\""));

        // 2.配置了键脱敏器的字段：userName/phone/secretKey
        assertTrue("userName字段应被脱敏", requestLog.contains("张**"));
        assertTrue("phone字段应被脱敏", requestLog.contains("138****5678"));
        assertTrue("secretKey字段应被完全脱敏", requestLog.contains("********"));

        // 3.未配置键脱敏器的字段（orderNo）由全局裸值脱敏器兜底
        assertTrue("orderNo字段中的手机号应被裸值脱敏器兜底脱敏", requestLog.contains("138****8888"));

        // 4.原始敏感值不应出现在日志中
        assertFalse("请求日志不应出现姓名原始值", requestLog.contains("张三"));
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("请求日志不应出现密码原始值", requestLog.contains("Abc123456"));
        assertFalse("请求日志不应出现orderNo原始值", requestLog.contains("13899998888"));
    }

    /**
     * 场景9：Mock响应头脱敏（X-Auth-Token / Set-Cookie）
     */
    @Test
    public void responseHeaderShouldMaskMockResponseHeaders() {
        String result = api.responseHeader();
        assertTrue("接口返回值不受脱敏影响", result.contains("\"code\":200"));

        String responseLog = responseLogText();

        // 1.响应头为普通键值对文本（无ANSI着色），名称保留、值按键名脱敏
        assertTrue("X-Auth-Token响应头应被完全脱敏", responseLog.contains("X-Auth-Token: ********"));
        assertTrue("Set-Cookie响应头应被保留前4后4脱敏", responseLog.contains("Set-Cookie: sid=********3456"));

        // 2.原始敏感值不应出现在日志中
        assertFalse("响应日志不应出现令牌原始值", responseLog.contains("tk_live_9f8e7d6c5b4a3210"));
        assertFalse("响应日志不应出现Cookie原始值", responseLog.contains("abcdef123456"));
    }

    /**
     * 场景10：XML内嵌JSON（请求体与响应体）
     */
    @Test
    public void xmlNestedJsonShouldMaskJsonFieldsInsideXml() {
        String result = api.xmlNestedJson();
        assertTrue("接口返回值不受脱敏影响", result.contains("<code>0</code>"));

        // 1.请求日志：contact标签的值是JSON文本，容器值跳过整体脱敏后由JSON工序进入内部按键名脱敏
        String requestLog = requestLogText();
        assertTrue("XML内嵌JSON中的姓名应被脱敏", requestLog.contains("\"userName\":\"张**\""));
        assertTrue("XML内嵌JSON中的手机号应被脱敏", requestLog.contains("\"phone\":\"138****5678\""));
        assertTrue("XML内嵌JSON中的邮箱应被脱敏", requestLog.contains("\"email\":\"zha***@example.com\""));
        assertTrue("非敏感XML标签应原样保留", requestLog.contains("<orderNo>SO20260924</orderNo>"));
        assertFalse("请求日志不应出现姓名原始值", requestLog.contains("张三"));
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("请求日志不应出现邮箱原始值", requestLog.contains("zhangsan@example.com"));

        // 2.响应日志：XML响应体中data标签的值同样是JSON文本，由JSON工序按键名脱敏
        String responseLog = responseLogText();
        assertTrue("XML响应内嵌JSON中的手机号应被脱敏", responseLog.contains("\"phone\":\"139****7777\""));
        assertTrue("非敏感XML标签应原样保留", responseLog.contains("<code>0</code>"));
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13998887777"));
    }

    /**
     * 场景11：JSON内嵌XML字符串
     */
    @Test
    public void jsonEmbeddedXmlShouldMaskXmlTagsInsideJsonString() {
        String result = api.jsonEmbeddedXml();
        assertTrue("接口返回值不受脱敏影响", result.contains("13812345678"));

        String responseLog = responseLogText();

        // 1.内嵌XML按标签名脱敏
        assertTrue("内嵌XML中的姓名应被脱敏", responseLog.contains("<userName>张**</userName>"));
        assertTrue("内嵌XML中的手机号应被脱敏", responseLog.contains("<phone>138****5678</phone>"));
        assertTrue("内嵌XML中的身份证应被脱敏", responseLog.contains("<idCardNum>510123********1234</idCardNum>"));
        assertTrue("内嵌XML中的令牌应被完全脱敏", responseLog.contains("<token>********</token>"));

        // 2.无标签可依的自由文本（xmlFree）中的手机号由裸值脱敏器兜底
        assertTrue("无标签自由文本中的手机号应由裸值兜底脱敏", responseLog.contains("验证码138****5678已发送"));

        // 3.外层JSON结构应保留（字段未配置脱敏器，不整体替换）
        assertTrue("外层JSON字段名应保留", responseLog.contains("\"xmlData\""));
        assertTrue("外层JSON字段名应保留", responseLog.contains("\"xmlBlock\""));

        // 4.原始敏感值不应出现在日志中
        assertFalse("响应日志不应出现姓名原始值", responseLog.contains("张三"));
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("响应日志不应出现身份证原始值", responseLog.contains("510123199001011234"));
        assertFalse("响应日志不应出现令牌原始值", responseLog.contains("tk_live_9f8e7d6c5b4a3210"));
    }

    /**
     * 场景12：JSON内嵌JSON字符串（转义JSON）
     */
    @Test
    public void jsonInJsonStringShouldMaskEscapedNestedFields() {
        String result = api.jsonInJsonString();
        assertTrue("接口返回值不受脱敏影响", result.contains("13812345678"));

        String responseLog = responseLogText();

        // 1.转义JSON字符串内的字段按转义键值对模式（\"key\":\"value\"）脱敏
        assertTrue("转义JSON中的手机号应被脱敏", responseLog.contains("\\\"phone\\\":\\\"138****5678\\\""));
        assertTrue("转义JSON中的设备号应按前4后4脱敏", responseLog.contains("\\\"deviceId\\\":\\\"dev-********5b4a\\\""));
        assertTrue("转义JSON中的身份证应被脱敏", responseLog.contains("\\\"idCardNum\\\":\\\"510123********1234\\\""));

        // 2.未配置脱敏器的字段应保留结构与原始值
        assertTrue("转义JSON中的非敏感字段应保留", responseLog.contains("\\\"userId\\\":1001"));
        assertTrue("转义JSON中的非敏感字段应保留", responseLog.contains("\\\"orderNo\\\":1001"));
        assertTrue("转义JSON中的非敏感字段应保留", responseLog.contains("\\\"carrier\\\":\\\"mobile\\\""));

        // 3.普通JSON字符串值中的手机号（两侧中文紧邻）由裸值脱敏器兜底
        assertTrue("JSON字符串中的手机号应由裸值兜底脱敏", responseLog.contains("验证码138****5678已发送给用户"));

        // 4.原始敏感值不应出现在日志中
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("响应日志不应出现身份证原始值", responseLog.contains("510123199001011234"));
        assertFalse("响应日志不应出现设备号原始值", responseLog.contains("dev-9f8e7d6c5b4a"));
    }

    /**
     * 场景13：MultipartForm字段值为JSON/XML/k=v字符串
     */
    @Test
    public void multipartNestedFieldsShouldMaskJsonXmlKvStrings() {
        String result = api.uploadNestedForm();
        assertTrue("接口返回值不受脱敏影响", result.contains("true"));

        String requestLog = requestLogText();

        // 1.multipart结构完整，字段名（name="xxx"）原样保留
        assertTrue("请求日志应打印multipart的Content-Type", requestLog.contains("multipart/form-data; boundary=LuckyBoundary"));
        assertTrue("字段名应原样保留", requestLog.contains("name=\"jsonField\""));
        assertTrue("字段名应原样保留", requestLog.contains("name=\"xmlField\""));
        assertTrue("字段名应原样保留", requestLog.contains("name=\"kvField\""));
        assertTrue("字段名应原样保留", requestLog.contains("name=\"envelope\""));

        // 2.JSON字符串字段：内部手机号/邮箱由裸值脱敏器兜底脱敏
        assertTrue("JSON字符串字段中的手机号应被脱敏", requestLog.contains("{\"phone\":\"138****5678\",\"email\":\"zha***@example.com\"}"));
        // 3.XML字符串字段：内部手机号/身份证由裸值脱敏器兜底脱敏
        assertTrue("XML字符串字段中的手机号应被脱敏", requestLog.contains("<user><phone>138****5678</phone><idCardNum>510123********1234</idCardNum></user>"));
        // 4.k=v格式字段：手机号为裸值脱敏、JWT为裸值脱敏
        assertTrue("k=v字段中的手机号应被脱敏", requestLog.contains("uid=U1001;phone=138****5678;token=********"));
        // 5.命中字段名脱敏器的字段：整体替换为完全脱敏占位符
        assertTrue("secretKey字段应被完全脱敏", requestLog.contains("********"));

        // 6.原始敏感值不应出现在日志中
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("请求日志不应出现邮箱原始值", requestLog.contains("zhangsan@example.com"));
        assertFalse("请求日志不应出现身份证原始值", requestLog.contains("510123199001011234"));
        assertFalse("请求日志不应出现JWT原始值", requestLog.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertFalse("请求日志不应出现secretKey原始值", requestLog.contains("Abc123456"));
        // envelope字段命中了键脱敏器（FULL），其JSON原文不应出现
        assertFalse("envelope字段的JSON原文不应出现", requestLog.contains("{\"phone\":\"13812345678\"}"));
    }

    /**
     * 场景14：请求头中的JSON/XML/k=v数据
     */
    @Test
    public void requestHeadersShouldMaskNestedJsonXmlKvData() {
        String result = api.headerNested(
                "{\"phone\":\"13812345678\",\"email\":\"zhangsan@example.com\"}",
                "<user><phone>13812345678</phone></user>",
                "uid=U1001;phone=13812345678;idCardNum=510123199001011234;token=Abc123456"
        );
        assertTrue("接口返回值不受脱敏影响", result.contains("\"code\":200"));

        String requestLog = requestLogText();

        // 1.三个请求头名称均原样保留
        assertTrue("JSON数据头名称应保留", requestLog.contains("X-Json-Data"));
        assertTrue("XML数据头名称应保留", requestLog.contains("X-Xml-Data"));
        assertTrue("k=v数据头名称应保留", requestLog.contains("X-Kv-Data"));

        // 2.JSON字符串头：由JSON工序按键名脱敏
        assertTrue("请求头中的JSON手机号/邮箱应被脱敏", requestLog.contains("{\"phone\":\"138****5678\",\"email\":\"zha***@example.com\"}"));
        // 3.XML字符串头：由XML工序按标签名脱敏
        assertTrue("请求头中的XML手机号应被脱敏", requestLog.contains("<user><phone>138****5678</phone></user>"));
        // 4.k=v数据头：由通用键值对工序按键名脱敏，非敏感数据保留
        assertTrue("请求头中的k=v数据应被脱敏", requestLog.contains("phone=138****5678;idCardNum=510123********1234;token=********"));
        assertTrue("非敏感k=v数据应保留", requestLog.contains("uid=U1001"));

        // 5.原始敏感值不应出现在日志中
        assertFalse("请求日志不应出现手机号原始值", requestLog.contains("13812345678"));
        assertFalse("请求日志不应出现邮箱原始值", requestLog.contains("zhangsan@example.com"));
        assertFalse("请求日志不应出现身份证原始值", requestLog.contains("510123199001011234"));
        assertFalse("请求日志不应出现令牌原始值", requestLog.contains("Abc123456"));
    }

    /**
     * 场景15：响应头中的JSON/XML/k=v数据
     */
    @Test
    public void responseHeadersShouldMaskNestedJsonXmlKvData() {
        String result = api.responseHeaderNested();
        assertTrue("接口返回值不受脱敏影响", result.contains("\"code\":200"));

        String responseLog = responseLogText();

        // 1.响应头为普通文本：JSON字符串头由JSON工序按键名脱敏
        assertTrue("响应头中的JSON手机号/邮箱应被脱敏", responseLog.contains("X-Json-Data: {\"phone\":\"138****5678\",\"email\":\"zha***@example.com\"}"));
        // 2.XML字符串头由XML工序按标签名脱敏
        assertTrue("响应头中的XML手机号应被脱敏", responseLog.contains("X-Xml-Data: <user><phone>138****5678</phone></user>"));
        // 3.k=v数据头由通用键值对工序按键名脱敏
        assertTrue("响应头中的k=v数据应被脱敏", responseLog.contains("X-Kv-Data: uid=U1001;phone=138****5678;idCardNum=510123********1234;token=********"));

        // 4.原始敏感值不应出现在日志中
        assertFalse("响应日志不应出现手机号原始值", responseLog.contains("13812345678"));
        assertFalse("响应日志不应出现邮箱原始值", responseLog.contains("zhangsan@example.com"));
        assertFalse("响应日志不应出现身份证原始值", responseLog.contains("510123199001011234"));
        assertFalse("响应日志不应出现令牌原始值", responseLog.contains("Abc123456"));
    }
}
