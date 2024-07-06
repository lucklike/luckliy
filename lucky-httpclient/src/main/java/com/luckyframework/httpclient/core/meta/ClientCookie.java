package com.luckyframework.httpclient.core.meta;

import com.luckyframework.conversion.ConversionUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * 客户端Cookie
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/5 00:34
 */
public class ClientCookie {

    private static final DateTimeFormatter FORMATTER_ = DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.ENGLISH);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);


    /**
     * Cookie name.
     */
    private String name;

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
    private Integer maxAge;

    /**
     * 创建时间
     */
    private final Date createTime;

    /***
     * 过期时间
     */
    private Date expireTime;
    /**
     * {@code ;Path=VALUE ...} URLs that see the cookie
     */
    private String path;
    /**
     * {@code ;Secure ...} e.g. use SSL
     */
    private boolean secure;

    private boolean httpOnly;

    public ClientCookie(Header cookieHeader, Request request) {
        this.createTime = new Date();
        Map<String, String> nameValuePairMap = cookieHeader.getNameValuePairMap();
        boolean first = true;
        for (Map.Entry<String, String> entry : nameValuePairMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (first) {
                this.name = key;
                this.value = value;
                first = false;
            }
            switch (key) {
                case "Name":
                    this.name = value;
                    break;
                case "Value":
                    this.value = value;
                    break;
                case "Expires":
                    this.expireTime = parseDate(value);
                    break;
                case "Version":
                    this.version = ConversionUtils.conversion(value, int.class);
                    break;
                case "Comment":
                    this.comment = value;
                    break;
                case "Domain":
                    this.domain = value;
                    break;
                case "Max-Age":
                    this.maxAge = ConversionUtils.conversion(value, Integer.class);
                    break;
                case "Path":
                    this.path = value;
                    break;
                case "Secure":
                    this.secure = true;
                    break;
                case "HttpOnly":
                    this.httpOnly = true;
                    break;
            }
        }

        URL url = request.getURL();
        if (domain == null) {
            domain = url.getHost();
        }
        if (path == null) {
            path = "/";
        }
    }

    public ClientCookie(String name, String value) {
        this.name = name;
        this.value = value;
        this.domain = ".";
        this.path = "/";
        this.createTime = new Date();
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

    public Date getExpireTime() {
        return expireTime;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public boolean isExpired() {
        long expireTimeLong = getExpireTimeLong();
        if (expireTimeLong == -1) {
            return false;
        }
        return new Date().getTime() - createTime.getTime() > expireTimeLong;
    }

    public long getExpireTimeLong() {
        if (maxAge != null) {
            long time = createTime.getTime();
            return maxAge > 0 ? time + (maxAge * 1000L) : time;
        }
        if (expireTime != null) {
            return expireTime.getTime();
        }
        return -1;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    private Date parseDate(String date) {
        DateTimeFormatter pattern = date.contains("-") ? FORMATTER_ : FORMATTER;
        LocalDateTime localDateTime = LocalDateTime.parse(date, pattern);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
