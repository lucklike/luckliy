package com.luckyframework.httpclient.proxy.logging.support;

import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Header;
import com.luckyframework.httpclient.proxy.annotations.MultipartFormData;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.Query;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.annotations.StaticJsonBody;
import com.luckyframework.httpclient.proxy.annotations.StaticXmlBody;
import com.luckyframework.httpclient.proxy.logging.BeautifulLoggerPrintHandler;
import com.luckyframework.httpclient.proxy.logging.Logger;
import com.luckyframework.httpclient.proxy.logging.MaskType;
import com.luckyframework.httpclient.proxy.logging.Masker;
import com.luckyframework.httpclient.proxy.mock.Mock;

/**
 * 数据脱敏场景测试接口
 * <pre>
 * 所有方法均使用 {@link Mock @Mock} 模拟响应，不会发出真实网络请求，
 * 接口使用 {@link Logger @Logger}(handlerClass = BeautifulLoggerPrintHandler.class)
 * 触发真实的日志打印与脱敏链路。
 *
 * 覆盖的脱敏场景：
 *   1.login          - URL查询参数脱敏 + JSON响应字段脱敏 + JWT裸值兜底
 *   2.createUser     - 静态JSON请求体脱敏 + 独立@Masker注解隐式启用脱敏
 *   3.sendSms        - text/plain自由文本裸值脱敏 + maskResponse表达式显式启用
 *   4.users          - 中文键名脱敏 + JSON数组元素脱敏 + 容器值跳过
 *   5.rawJson        - 转义JSON（JSON字符串被再次序列化）内嵌字段脱敏
 *   6.headerPassing  - 请求头传参脱敏（动态@Header参数 + 静态@StaticHeader）
 *   7.xmlBody        - XML请求体/响应体按标签名脱敏
 *   8.uploadForm     - MultipartForm文本字段按键名脱敏 + 裸值兜底
 *   9.responseHeader - Mock响应头脱敏（X-Auth-Token / Set-Cookie）
 *  10.xmlNestedJson  - XML内嵌JSON：容器值整体跳过，由JSON工序进入内部按键名脱敏
 *  11.jsonEmbeddedXml - JSON内嵌XML字符串：按XML标签名脱敏 + 裸值兜底
 *  12.jsonInJsonString - JSON内嵌JSON字符串：转义JSON键值对模式脱敏 + 裸值兜底
 *  13.uploadNestedForm - multipart字段值为JSON/XML/k=v字符串：裸值兜底脱敏 + 字段名脱敏器
 *  14.headerNested   - 请求头中的JSON/XML/k=v数据脱敏
 *  15.responseHeaderNested - 响应头中的JSON/XML/k=v数据脱敏
 * </pre>
 */
@ServerAddress("http://localhost:8080")
@Logger(handlerClass = BeautifulLoggerPrintHandler.class)
public interface DataMaskingTestApi {

    /**
     * 场景1：登录
     * <pre>
     * URL查询参数(phone/password)在请求日志中被脱敏；
     * JSON响应中的phone/email字段被脱敏；
     * 响应中的JWT token未配置键脱敏器，由全局裸值脱敏器兜底
     * </pre>
     *
     * @param phone    手机号
     * @param password 密码
     * @return Mock响应JSON字符串
     */
    @Get("/api/login")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"code\":200,\"message\":\"登录成功\",\"phone\":\"13812345678\",\"email\":\"zhangsan@example.com\",\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U\"}"
    )
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "password", type = MaskType.FULL)
    @Masker(keys = "email", type = MaskType.EMAIL)
    String login(@Query("phone") String phone, @Query("password") String password);

    /**
     * 场景2：创建用户
     * <pre>
     * 静态JSON请求体中的字段被脱敏；
     * 未标注@PrintLog，仅通过独立@Masker注解隐式启用脱敏（配置了脱敏器即默认启用）
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Post("/api/user")
    @StaticJsonBody("{\"userName\":\"张三\",\"phone\":\"13812345678\",\"password\":\"Abc123456\",\"idCardNum\":\"510123199001011234\"}")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"success\":true}"
    )
    @Masker(keys = "userName", type = MaskType.NAME)
    @Masker(keys = "password", type = MaskType.FULL)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    String createUser();

    /**
     * 场景3：发送短信
     * <pre>
     * text/plain自由文本响应无键名结构，其中的手机号/邮箱由全局裸值脱敏器脱敏；
     * 通过 maskResponse = "#{true}" 表达式显式启用响应脱敏
     * </pre>
     *
     * @return Mock响应文本
     */
    @Post("/api/sms")
    @Mock(
            status = "200",
            header = {"Content-Type: text/plain"},
            body = "您的验证码为862693，客服电话13812345678，邮箱kefu@example.com，请勿泄露。"
    )
    @PrintLog(maskResponse = "#{true}")
    String sendSms();

    /**
     * 场景4：用户列表
     * <pre>
     * 中文键名(手机号)脱敏；
     * JSON数组元素内的phone字段逐一脱敏；
     * data是容器值（数组），配置了脱敏器也应跳过整体脱敏，避免破坏JSON结构
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/users")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"total\":2,\"手机号\":\"13812345678\",\"data\":[{\"phone\":\"13998887777\"},{\"phone\":\"13611112222\"}]}"
    )
    @Masker(keys = "手机号", type = MaskType.FULL)
    @Masker(keys = "phone", type = MaskType.FULL)
    @Masker(keys = "data", type = MaskType.FULL)
    String users();

    /**
     * 场景5：转义JSON
     * <pre>
     * 响应中rawData字段的值是"被再次序列化的JSON字符串"，
     * 其中的phone字段应通过转义JSON模式(\"key\":\"value\")被脱敏
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/raw")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"rawData\":\"{\\\"phone\\\":\\\"13812345678\\\"}\"}"
    )
    @Masker(keys = "phone", type = MaskType.FULL)
    String rawJson();

    /**
     * 场景6：请求头传参
     * <pre>
     * 动态{@code @Header}参数（X-Api-Token）与静态{@code @StaticHeader}（X-App-Id）
     * 均以"头名称: 头值"的形式打印在请求日志的请求头区域；
     * 请求头名称在日志中带有ANSI着色，由ANSI彩色键值对工序按键名匹配脱敏器：
     *   X-Api-Token -> 完全脱敏
     *   X-App-Id    -> 保留前4后4
     * </pre>
     *
     * @param apiToken 调用方传入的接口令牌
     * @return Mock响应JSON字符串
     */
    @Post("/api/header")
    @StaticHeader("X-App-Id: app-9f8e7d6c5b4a")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"code\":200,\"message\":\"ok\"}"
    )
    @Masker(keys = "X-Api-Token", type = MaskType.FULL)
    @Masker(keys = "X-App-Id", type = MaskType.FIRST4_LAST4)
    String headerPassing(@Header("X-Api-Token") String apiToken);

    /**
     * 场景7：XML请求体与XML响应体
     * <pre>
     * 请求体（{@code @StaticXmlBody}）与响应体（Content-Type: application/xml）
     * 在日志中均会被格式化输出，脱敏由XML工序完成——按XML标签名匹配脱敏器：
     *   请求体：userName（姓名）、phone（手机号）、idCardNum（身份证）
     *   响应体：phone（手机号）、email（邮箱）
     * 非敏感标签（如code）原样保留
     * </pre>
     *
     * @return Mock响应XML字符串
     */
    @Post("/api/xml")
    @StaticXmlBody("<user><userName>张三</userName><phone>13812345678</phone><idCardNum>510123199001011234</idCardNum></user>")
    @Mock(
            status = "200",
            header = {"Content-Type: application/xml"},
            body = "<result><code>0</code><phone>13812345678</phone><email>zhangsan@example.com</email></result>"
    )
    @Masker(keys = "userName", type = MaskType.NAME)
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    @Masker(keys = "email", type = MaskType.EMAIL)
    String xmlBody();

    /**
     * 场景8：MultipartForm表单
     * <pre>
     * multipart文本字段按"字段名+字段值"逐字段打印；
     * 字段值先按键名脱敏（userName/phone/secretKey），
     * 未配置键脱敏器的字段（orderNo）由全局裸值脱敏器兜底；
     * 脱敏只作用于字段值，字段名（Content-Disposition中的name）原样保留
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Post("/api/upload")
    @MultipartFormData(txt = {
            "userName=张三",
            "phone=13812345678",
            "secretKey=Abc123456",
            "orderNo=13899998888"
    })
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"uploaded\":true}"
    )
    @Masker(keys = "userName", type = MaskType.NAME)
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "secretKey", type = MaskType.FULL)
    String uploadForm();

    /**
     * 场景9：响应头脱敏
     * <pre>
     * Mock响应头（X-Auth-Token、Set-Cookie）随响应日志一并打印；
     * 响应头为"头名称: 头值"的普通文本（无ANSI着色），
     * 由通用键值对工序按键名匹配脱敏器：
     *   X-Auth-Token -> 完全脱敏
     *   Set-Cookie   -> 保留前4后4
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/resp-header")
    @Mock(
            status = "200",
            header = {
                    "Content-Type: application/json",
                    "X-Auth-Token: tk_live_9f8e7d6c5b4a3210",
                    "Set-Cookie: sid=abcdef123456"
            },
            body = "{\"code\":200}"
    )
    @Masker(keys = "X-Auth-Token", type = MaskType.FULL)
    @Masker(keys = "Set-Cookie", type = MaskType.FIRST4_LAST4)
    String responseHeader();

    /**
     * 场景10：XML内嵌JSON
     * <pre>
     * 请求体（XML）中contact标签的值是一段JSON文本：
     *   XML工序识别到值以'{'开头（容器结构）后跳过整体脱敏，避免破坏结构；
     *   随后由JSON工序进入JSON文本内部，按键名（userName/phone/email）完成脱敏。
     * 响应体（XML）中data标签的值同样是JSON文本，由JSON工序按键名脱敏；
     * 非敏感XML标签（orderNo/code）原样保留
     * </pre>
     *
     * @return Mock响应XML字符串
     */
    @Post("/api/xml-nested")
    @StaticXmlBody("<order><orderNo>SO20260924</orderNo><contact>{\"userName\":\"张三\",\"phone\":\"13812345678\",\"email\":\"zhangsan@example.com\"}</contact></order>")
    @Mock(
            status = "200",
            header = {"Content-Type: application/xml"},
            body = "<result><code>0</code><data>{\"phone\":\"13998887777\"}</data></result>"
    )
    @Masker(keys = "userName", type = MaskType.NAME)
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "email", type = MaskType.EMAIL)
    String xmlNestedJson();

    /**
     * 场景11：JSON内嵌XML字符串
     * <pre>
     * 响应JSON中多个字段的值是XML字符串：
     *   xmlData   - 内嵌的XML按标签名脱敏（userName/phone/idCardNum）
     *   xmlBlock  - 内嵌的XML按标签名脱敏（token）
     *   xmlFree   - 内嵌消息文本中的手机号无标签可依，由裸值脱敏器兜底
     * 外层JSON字段名（xmlData/xmlBlock/xmlFree）未配置脱敏器，原样保留
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/json-nested")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"code\":0,\"xmlData\":\"<user><userName>张三</userName><phone>13812345678</phone><idCardNum>510123199001011234</idCardNum></user>\",\"xmlBlock\":\"<auth><token>tk_live_9f8e7d6c5b4a3210</token></auth>\",\"xmlFree\":\"<msg>验证码13812345678已发送</msg>\"}"
    )
    @Masker(keys = "userName", type = MaskType.NAME)
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    @Masker(keys = "token", type = MaskType.FULL)
    String jsonEmbeddedXml();

    /**
     * 场景12：JSON内嵌JSON字符串（转义JSON）
     * <pre>
     * 响应JSON中多个字段的值是"被再次序列化的JSON字符串"（值内的引号带\转义）：
     *   payload   - 转义JSON中的phone按键名脱敏
     *   idPayload - 转义JSON中的deviceId按键名脱敏（dev-9f8e7d6c5b4a → dev-********5b4a）
     *   phoneInfo - 转义JSON中的idCardNum脱敏
     *   note      - 普通JSON字符串值中的手机号（中文紧邻），由裸值脱敏器兜底
     * 转义JSON中的非敏感字段（userId/orderNo/carrier）结构与值均保留
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/json-in-json")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"code\":0,\"payload\":\"{\\\"userId\\\":1001,\\\"phone\\\":\\\"13812345678\\\"}\",\"idPayload\":\"{\\\"orderNo\\\":1001,\\\"deviceId\\\":\\\"dev-9f8e7d6c5b4a\\\"}\",\"phoneInfo\":\"{\\\"carrier\\\":\\\"mobile\\\",\\\"idCardNum\\\":\\\"510123199001011234\\\"}\",\"note\":\"验证码13812345678已发送给用户\"}"
    )
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "deviceId", type = MaskType.FIRST4_LAST4)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    String jsonInJsonString();

    /**
     * 场景13：MultipartForm字段值为JSON/XML/k=v字符串
     * <pre>
     * multipart文本字段值本身是嵌套结构时的脱敏（字段级脱敏管线）：
     *   jsonField - 值为JSON字符串，内部手机号/邮箱由裸值脱敏器兜底
     *   xmlField  - 值为XML字符串，内部手机号/身份证由裸值脱敏器兜底
     *   kvField   - 值为k=v数据，内部手机号/JWT由裸值脱敏器兜底
     *   envelope  - 命中字段名脱敏器（FULL），整个JSON字符串被完全脱敏
     *   secretKey - 命中字段名脱敏器（FULL），完全脱敏
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Post("/api/upload-nested")
    @MultipartFormData(txt = {
            "jsonField={\"phone\":\"13812345678\",\"email\":\"zhangsan@example.com\"}",
            "xmlField=<user><phone>13812345678</phone><idCardNum>510123199001011234</idCardNum></user>",
            "kvField=uid=U1001;phone=13812345678;token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
            "envelope={\"phone\":\"13812345678\"}",
            "secretKey=Abc123456"
    })
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"uploaded\":true}"
    )
    @Masker(keys = "secretKey", type = MaskType.FULL)
    @Masker(keys = "envelope", type = MaskType.FULL)
    String uploadNestedForm();

    /**
     * 场景14：请求头中的JSON/XML/k=v数据
     * <pre>
     * 动态{@code @Header}参数传入嵌套结构数据，请求头按"头名称: 头值"打印：
     *   X-Json-Data - 值为JSON字符串，由JSON工序按键名（phone/email）脱敏
     *   X-Xml-Data  - 值为XML字符串，由XML工序按标签名（phone）脱敏
     *   X-Kv-Data   - 值为k=v数据，由通用键值对工序按键名（phone/idCardNum/token）脱敏
     * 请求头名称在日志中带ANSI着色，未配置键脱敏器的头名称不影响内部数据脱敏
     * </pre>
     *
     * @param jsonData 请求头中的JSON字符串
     * @param xmlData  请求头中的XML字符串
     * @param kvData   请求头中的k=v字符串
     * @return Mock响应JSON字符串
     */
    @Post("/api/header-nested")
    @Mock(
            status = "200",
            header = {"Content-Type: application/json"},
            body = "{\"code\":200}"
    )
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "email", type = MaskType.EMAIL)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    @Masker(keys = "token", type = MaskType.FULL)
    String headerNested(@Header("X-Json-Data") String jsonData,
                        @Header("X-Xml-Data") String xmlData,
                        @Header("X-Kv-Data") String kvData);

    /**
     * 场景15：响应头中的JSON/XML/k=v数据
     * <pre>
     * Mock响应头（普通文本打印，无ANSI着色）中的嵌套结构数据：
     *   X-Json-Data - 值为JSON字符串，由JSON工序按键名脱敏
     *   X-Xml-Data  - 值为XML字符串，由XML工序按标签名脱敏
     *   X-Kv-Data   - 值为k=v数据，由通用键值对工序按键名脱敏
     * </pre>
     *
     * @return Mock响应JSON字符串
     */
    @Get("/api/resp-header-nested")
    @Mock(
            status = "200",
            header = {
                    "Content-Type: application/json",
                    "X-Json-Data: {\"phone\":[\"13812345678\", \"13842345678\"],\"email\":\"zhangsan@example.com\"}",
                    "X-Xml-Data: <user><phone>13812345678</phone></user>",
                    "X-Kv-Data: uid=U1001;phone=13812345678;idCardNum=510123199001011234;token=Abc123456"
            },
            body = "{\"code\":200}"
    )
    @Masker(keys = "phone", type = MaskType.PHONE)
    @Masker(keys = "email", type = MaskType.EMAIL)
    @Masker(keys = "idCardNum", type = MaskType.ID_CARD)
    @Masker(keys = "token", type = MaskType.FULL)
    String responseHeaderNested();
}
