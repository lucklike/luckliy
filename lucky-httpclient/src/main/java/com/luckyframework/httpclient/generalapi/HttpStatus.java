package com.luckyframework.httpclient.generalapi;

import com.luckyframework.common.Console;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Callback;

/**
 * Http状态
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/8 23:34
 */
public enum HttpStatus {

    _100(100, "Continue", "继续。客户端应继续其请求"),
    _101(101, "Switching Protocols", "切换协议。服务器根据客户端的请求切换协议。"),

    _200(200, "OK", "请求成功"),
    _201(201, "Created", "已创建。成功请求并创建了新的资源"),
    _202(202, "Accepted", "已接受。已经接受请求，但未处理完成"),
    _203(203, "Non-Authoritative Information", "非授权信息。请求成功。但返回的meta信息不在原始的服务器，而是一个副本"),
    _204(204, "No Content", "无内容。服务器成功处理，但未返回内容。在未更新网页的情况下，可确保浏览器继续显示当前文档"),
    _205(205, "Reset Content", "重置内容。服务器处理成功，用户终端（例如：浏览器）应重置文档视图。可通过此返回码清除浏览器的表单域"),
    _206(206, "Partial Content", "部分内容。服务器成功处理了部分GET请求"),

    _300(300, "Multiple Choices", "多种选择。请求的资源可包括多个位置，相应可返回一个资源特征与地址的列表用于用户终端（例如：浏览器）选择"),
    _301(301, "Moved Permanently", "永久移动。请求的资源已被永久的移动到新URI，返回信息会包括新的URI，浏览器会自动定向到新URI。今后任何新的请求都应使用新的URI代替"),
    _302(302, "Found", "临时移动。与301类似。但资源只是临时被移动。客户端应继续使用原有URI"),
    _303(303, "See Other", "查看其它地址。与301类似。使用GET和POST请求查看"),
    _304(304, "Not Modified", "未修改。所请求的资源未修改，服务器返回此状态码时，不会返回任何资源。客户端通常会缓存访问过的资源，通过提供一个头信息指出客户端希望只返回在指定日期之后修改的资源"),
    _305(305, "Use Proxy", "使用代理。所请求的资源必须通过代理访问"),
    _306(306, "Unused", "已经被废弃的HTTP状态码"),
    _307(307, "Temporary Redirect", "临时重定向。与302类似。使用GET请求重定向"),

    _400(400, "Bad Request", "客户端请求的语法错误，服务器无法理解"),
    _401(401, "Unauthorized", "请求要求用户的身份认证"),
    _402(402, "Payment Required", "保留，将来使用"),
    _403(403, "Forbidden", "服务器理解请求客户端的请求，但是拒绝执行此请求"),
    _404(404, "Not Found", "服务器无法根据客户端的请求找到资源（网页）。通过此代码，网站设计人员可设置\"您所请求的资源无法找到\"的个性页面"),
    _405(405, "Method Not Allowed", "客户端请求中的方法被禁止"),
    _406(406, "Not Acceptable", "服务器无法根据客户端请求的内容特性完成请求"),
    _407(407, "Proxy Authentication Required", "请求要求代理的身份认证，与401类似，但请求者应当使用代理进行授权"),
    _408(408, "Request Time-out", "服务器等待客户端发送的请求时间过长，超时"),
    _409(409, "Conflict", "服务器完成客户端的 PUT 请求时可能返回此代码，服务器处理请求时发生了冲突"),
    _410(410, "Gone", "客户端请求的资源已经不存在。410不同于404，如果资源以前有现在被永久删除了可使用410代码，网站设计人员可通过301代码指定资源的新位置"),
    _411(411, "Length Required", "服务器无法处理客户端发送的不带Content-Length的请求信息"),
    _412(412, "Precondition Failed", "客户端请求信息的先决条件错误"),
    _413(413, "Request Entity Too Large", "由于请求的实体过大，服务器无法处理，因此拒绝请求。为防止客户端的连续请求，服务器可能会关闭连接。如果只是服务器暂时无法处理，则会包含一个Retry-After的响应信息"),
    _414(414, "Request-URI Too Large", "请求的URI过长（URI通常为网址），服务器无法处理"),
    _415(415, "Unsupported Media Type", "服务器无法处理请求附带的媒体格式"),
    _416(416, "Requested range not satisfiable", "客户端请求的范围无效"),
    _417(417, "Expectation Failed", "服务器无法满足请求头中 Expect 字段指定的预期行为。"),
    _418(418, "I'm a teapot", "状态码 418 实际上是一个愚人节玩笑。它在 RFC 2324 中定义，该 RFC 是一个关于超文本咖啡壶控制协议（HTCPCP）的笑话文件。在这个笑话中，418 状态码是作为一个玩笑加入到 HTTP 协议中的。"),

    _500(500, "Internal Server Error", "服务器内部错误，无法完成请求"),
    _501(501, "Not Implemented", "服务器不支持请求的功能，无法完成请求"),
    _502(502, "Bad Gateway", "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"),
    _503(503, "Service Unavailable", "由于超载或系统维护，服务器暂时的无法处理客户端的请求。延时的长度可包含在服务器的Retry-After头信息中"),
    _504(504, "Gateway Time-out", "充当网关或代理的服务器，未及时从远端服务器获取请求"),
    _505(505, "HTTP Version not supported", "服务器不支持请求的HTTP协议的版本，无法完成处理"),
    ;


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
     * 将某个状态码转化为对应的枚举对象
     *
     * @param code 状态码
     * @return 状态码转化为对应的枚举对象
     */
    public static HttpStatus getStatus(int code) {
        return HttpStatus.valueOf(String.format("_%s", code));
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

    /**
     * 包含一个校验HTTP状态码回调函数的工具类
     */
    public static class Check {

        /**
         * 校验HTTP状态码的回调函数
         *
         * @param response 响应对象
         */
        @Callback(lifecycle = Lifecycle.RESPONSE)
        public static void check(Response response) {
            int status = response.getStatus();
            if (HttpStatus.err(status)) {
                throw new HttpStatusException(
                        "HTTP status Abnormal ['{}'] {}, URL: [{}] {}",
                        Console.getRedString(status),
                        HttpStatus.getStatus(status).getDesc(),
                        Console.getYellowString(response.getRequest().getRequestMethod()),
                        response.getRequest().getUrl()
                );
            }
        }
    }
}
