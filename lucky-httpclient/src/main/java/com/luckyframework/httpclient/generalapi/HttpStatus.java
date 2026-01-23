package com.luckyframework.httpclient.generalapi;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Http状态
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/8 23:34
 */
public enum HttpStatus {

    // 1xx
    _100(100, "Continue", "继续"),
    _101(101, "Switching Protocols", "切换协议"),
    _102(102, "Processing", "处理中"),
    _103(103, "Early Hints", "早期提示"),


    // 2xx
    _200(200, "OK", "请求成功"),
    _201(201, "Created", "已创建"),
    _202(202, "Accepted", "已接受"),
    _203(203, "Non-Authoritative Information", "非授权信息"),
    _204(204, "No Content", "无内容"),
    _205(205, "Reset Content", "重置内容"),
    _206(206, "Partial Content", "部分内容"),
    _207(207, "Multi-Status", "多状态"),
    _208(208, "Already Reported", "已报告"),
    _226(226, "IM Used", "IM已使用"),


    // 3xx
    _300(300, "Multiple Choices", "多种选择"),
    _301(301, "Moved Permanently", "永久移动"),
    _302(302, "Found", "临时移动"),
    _303(303, "See Other", "查看其他位置"),
    _304(304, "Not Modified", "未修改"),
    _305(305, "SOAPBinding.Use Proxy", "使用代理"),
    _306(306, "Unused", "已废弃"),
    _307(307, "Temporary Redirect", "临时重定向"),
    _308(308, "Permanent Redirect", "永久重定向"),


    // 4xx
    _400(400, "Bad Request", "错误请求"),
    _401(401, "Unauthorized", "未授权"),
    _402(402, "Payment Required", "需要付款"),
    _403(403, "Forbidden", "禁止访问"),
    _404(404, "Not Found", "未找到"),
    _405(405, "Method Not Allowed ", "方法不允许"),
    _406(406, "Not Acceptable", "不可接受"),
    _407(407, "Proxy Authentication Required", "需要代理认证"),
    _408(408, "Request Timeout", "请求超时"),
    _409(409, "Conflict", "冲突"),
    _410(410, "Gone", "已删除"),
    _411(411, "Length Required", "需要内容长度"),
    _412(412, "Precondition Failed", "前置条件失败"),
    _413(413, "Payload Too Large", "请求体过大"),
    _414(414, "URI Too Long", "URI过长"),
    _415(415, "Unsupported Media Type", "不支持的媒体类型"),
    _416(416, "Range Not Satisfiable", "请求范围不符合要求"),
    _417(417, "Expectation Failed ", "期望失败"),
    _418(418, "I'm a teapot", "我是茶壶"),
    _421(421, "Misdirected Request", "错误定向请求"),
    _422(422, "Unprocessable Entity", "不可处理的实体"),
    _423(423, "Locked", "已锁定"),
    _424(424, "Failed Dependency", "依赖失败"),
    _425(425, "Too Early", "请求过早"),
    _426(426, "Upgrade Required", "需要升级"),
    _428(428, "Precondition Required", "需要前置条件"),
    _429(429, "Too Many Requests", "请求过多"),
    _431(431, "Request Header Fields Too Large", "求头字段过大"),
    _451(451, "Unavailable For Legal Reasons", "法律原因不可用"),


    // 5xx
    _500(500, "Internal Server Error",           "服务器内部错误"),
    _501(501, "Not Implemented",                  "未实现"),
    _502(502, "Bad Gateway",                      "错误网关"),
    _503(503, "Service Unavailable",             "服务不可用"),
    _504(504, "Gateway Timeout",                 "网关超时"),
    _505(505, "HTTP Version Not Supported",       "HTTP版本不支持"),
    _506(506, "Variant Also Negotiates",          "变体也可协商"),
    _507(507, "Insufficient Storage",             "存储空间不足"),
    _508(508, "Loop Detected",                    "检测到循环"),
    _510(510, "Not Extended",                     "未扩展"),
    _511(511, "Network Authentication Required",  "需要网络认证"),
    ;

    private final static Map<Integer, HttpStatus> MAP = Arrays.stream(HttpStatus.values()).collect(Collectors.toMap(e -> e.code, e -> e));


    /**
     * 状态码
     */
    private final int code;

    /**
     * 描述信息
     */
    private final String desc;

    /**
     * 详细的中文描述
     */
    private final String chineseDesc;

    /**
     * 内置的构造函数
     *
     * @param code        状态码
     * @param desc        描述信息
     * @param chineseDesc 详细的中文描述
     */
    HttpStatus(int code, String desc, String chineseDesc) {
        this.code = code;
        this.desc = desc;
        this.chineseDesc = chineseDesc;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取描述信息
     *
     * @return 描述信息
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取详细的中文描述
     *
     * @return 详细的中文描述
     */
    public String getChineseDesc() {
        return chineseDesc;
    }

    /**
     * 是否是异常的错误码
     * <pre>
     *     异常码：>= 400
     *     正常码：< 400
     * </pre>
     *
     * @return 是否是异常的错误码
     */
    public boolean isErr() {
        return err(getCode());
    }

    /**
     * 将某个状态码转化为对应的枚举对象
     *
     * @param code 状态码
     * @return 状态码转化为对应的枚举对象
     */
    @Nullable
    public static HttpStatus getStatus(int code) {
        return MAP.get(code);
    }

    /**
     * 判断某个状态码是否异常，大于等于400的状态码会被视为异常
     *
     * @param code 状态码转
     * @return 当前状态码是否异常
     */
    public static boolean err(int code) {
        return code >= 400;
    }

}
