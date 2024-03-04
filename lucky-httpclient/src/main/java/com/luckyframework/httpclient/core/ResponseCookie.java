package com.luckyframework.httpclient.core;

import com.luckyframework.conversion.ConversionUtils;

import java.util.Date;
import java.util.Map;

/**
 * 响应Cookie
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/5 00:34
 */
public class ResponseCookie {

    /**
     * Cookie name.
     */
    private  String name;

    /**
     * Cookie value.
     */
    private String value;

    /**
     * Cookie version value. {@code ;Version=1 ...} means RFC 2109 style.
     */
    private int version = 0;

    //
    // Attributes encoded in the header's cookie fields.
    //
    /**
     * {@code ;Comment=VALUE ...} describes cookie's use.
     */
    private String comment;
    /**
     * {@code ;Domain=VALUE ...} domain that sees cookie
     */
    private String domain;
    /**
     * {@code ;Max-Age=VALUE ...} cookies auto-expire
     */
    private int maxAge = -1;

    /**
     * 创建时间
     */
    private final Date createTime;
    /**
     * {@code ;Path=VALUE ...} URLs that see the cookie
     */
    private String path;
    /**
     * {@code ;Secure ...} e.g. use SSL
     */
    private boolean secure;

    private boolean httpOnly;

    private final String string;

    public ResponseCookie(Header cookieHeader) {
        this.string = cookieHeader.toString();
        this.createTime = new Date();
        Map<String, String> nameValuePairMap = cookieHeader.getNameValuePairMap();
        int i = 0;
        for (Map.Entry<String, String> entry : nameValuePairMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (i == 0) {
                this.name = key;
                this.value = value;
            } else {
                switch (key) {
                    case "Version": this.version = ConversionUtils.conversion(value, int.class); break;
                    case "Comment": this.comment = value; break;
                    case "Domain": this.domain = value; break;
                    case "Max-Age": this.maxAge = ConversionUtils.conversion(value, int.class); break;
                    case "Path": this.path = value; break;
                    case "Secure": this.secure = ConversionUtils.conversion(value, boolean.class); break;
                }
            }
            i++;
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public String getComment() {
        return comment;
    }

    public String getDomain() {
        return domain;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public String getPath() {
        return path;
    }

    public boolean isSecure() {
        return secure;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public boolean isExpired() {
        if (this.maxAge == -1) {
            return false;
        }
        return new Date().getTime() - createTime.getTime() <= maxAge;
    }

    @Override
    public String toString() {
        return string;
    }
}
