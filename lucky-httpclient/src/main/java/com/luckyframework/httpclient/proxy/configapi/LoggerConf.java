package com.luckyframework.httpclient.proxy.configapi;

import java.util.Set;

public class LoggerConf {


    /**
     * 是否开启日志功能，默认关闭
     */
    private Boolean enable;

    /**
     * 是否开启请求日志，默认开启（只有在{@link #enable}为{@code true}时才生效）
     */
    private Boolean enableReqLog;

    /**
     * 是否开启响应日志，默认开启（只有在{@link #enable}为{@code true}时才生效）
     */
    private Boolean enableRespLog;

    /**
     * MimeType为这些类型时，将打印响应体日志（覆盖默认值）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     */
    private Set<String> setAllowMimeTypes;

    /**
     * MimeType为这些类型时，将打印响应体日志（在默认值的基础上新增）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     */
    private Set<String> addAllowMimeTypes;


    /**
     * 请求体超过该值时，将不会打印请求体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    private Long reqBodyMaxLength = -1L;

    /**
     * 响应体超过该值时，将不会打印响应体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    private Long respBodyMaxLength = -1L;


    /**
     * 打印请求日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     */
    private String reqLogCondition;

    /**
     * 打印响应日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     */
    private String respLogCondition;

    /**
     * 设置是否打印日志
     *
     * @param enable 是否打印日志
     */
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    /**
     * 设置是否开启请求日志的打印，默认开启
     *
     * @param enableReqLog 是否开启请求日志的打印
     */
    public void setEnableReqLog(Boolean enableReqLog) {
        this.enableReqLog = enableReqLog;
    }

    /**
     * 设置是否开启响应日志的打印，默认开启
     *
     * @param enableRespLog 否开启响应日志的打印
     */
    public void setEnableRespLog(Boolean enableRespLog) {
        this.enableRespLog = enableRespLog;
    }

    /**
     * MimeType为这些类型时，将打印响应体日志（覆盖默认值）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     *
     * @param setAllowMimeTypes 打印响应体内容的MimeType集合
     */
    public void setSetAllowMimeTypes(Set<String> setAllowMimeTypes) {
        this.setAllowMimeTypes = setAllowMimeTypes;
    }

    /**
     * MimeType为这些类型时，将打印响应体日志（在默认值的基础上新增）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     *
     * @param addAllowMimeTypes 追加的打印响应体内容的MimeType集合
     */
    public void setAddAllowMimeTypes(Set<String> addAllowMimeTypes) {
        this.addAllowMimeTypes = addAllowMimeTypes;
    }


    /**
     * 打印请求日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     *
     * @param reqLogCondition 打印请求日志的条件
     */
    public void setReqLogCondition(String reqLogCondition) {
        this.reqLogCondition = reqLogCondition;
    }

    /**
     * 打印响应日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     *
     * @param respLogCondition 打印请求日志的条件
     */
    public void setRespLogCondition(String respLogCondition) {
        this.respLogCondition = respLogCondition;
    }

    /**
     * 是否开启日志打印功能
     *
     * @return 是否开启日志打印功能
     */
    public Boolean isEnable() {
        return enable;
    }

    /**
     * 是否开启了请求日志打印功能
     *
     * @return 是否开启了请求日志打印功能
     */
    public Boolean isEnableReqLog() {
        return enableReqLog;
    }

    /**
     * 是否开启了响应日志打印功能
     *
     * @return 是否开启了响应日志打印功能
     */
    public Boolean isEnableRespLog() {
        return enableRespLog;
    }


    /**
     * MimeType为这些类型时，将打印响应体日志（覆盖默认值）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     */
    public Set<String> getSetAllowMimeTypes() {
        return setAllowMimeTypes;
    }

    /**
     * MimeType为这些类型时，将打印响应体日志（在默认值的基础上新增）<br/>
     * (注： *&frasl;* : 表示所有类型)<br/>
     * 默认值：
     * <ui>
     * <li>application/json</li>
     * <li>application/*+json</li>
     *
     * <li>application/xml</li>
     * <li>application/*+xml</li>
     * <li>text/xml</li>
     *
     * <li>application/x-protobuf</li>
     *
     * <li>application/x-java-serialized-object</li>
     *
     * <li>text/plain</li>
     * <li>text/html</li>
     * </ui>
     */
    public Set<String> getAddAllowMimeTypes() {
        return addAllowMimeTypes;
    }

    /**
     * 打印请求日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     *
     * @return 打印请求日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     */
    public String getReqLogCondition() {
        return reqLogCondition;
    }

    /**
     * 打印响应日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     *
     * @return 打印响应日志的条件，这里可以写一个返回值为boolean类型的SpEL表达式，true时才会打印日志
     */
    public String getRespLogCondition() {
        return respLogCondition;
    }


    /**
     * 获取打印请求日志的阈值，请求体超过该值时，将不会打印请求体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    public long getReqBodyMaxLength() {
        return reqBodyMaxLength;
    }

    /**
     * 获取打印响应日志的阈值，响应体超过该值时，将不会打印响应体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    public long getRespBodyMaxLength() {
        return respBodyMaxLength;
    }

    /**
     * 设置打印请求日志的阈值，请求体超过该值时，将不会打印请求体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    public void setReqBodyMaxLength(long reqBodyMaxLength) {
        this.reqBodyMaxLength = reqBodyMaxLength;
    }

    /**
     * 设置打印响应日志的阈值，响应体超过该值时，将不会打印响应体日志，值小于等于0时表示没有限制<br/>
     * 单位：字节<br/>
     * 默认值：-1
     */
    public void setRespBodyMaxLength(long respBodyMaxLength) {
        this.respBodyMaxLength = respBodyMaxLength;
    }


}
