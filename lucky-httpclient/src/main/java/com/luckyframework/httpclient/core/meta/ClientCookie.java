package com.luckyframework.httpclient.core.meta;

import com.luckyframework.conversion.ConversionUtils;

import java.net.URL;
import java.time.Instant;
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
            key = key.toLowerCase();
            switch (key) {
                case "name":
                    this.name = value;
                    break;
                case "value":
                    this.value = value;
                    break;
                case "expires":
                    this.expireTime = parseDate(value);
                    break;
                case "version":
                    this.version = ConversionUtils.conversion(value, int.class);
                    break;
                case "comment":
                    this.comment = value;
                    break;
                case "domain":
                    this.domain = value;
                    break;
                case "max-age":
                    this.maxAge = ConversionUtils.conversion(value, Integer.class);
                    break;
                case "path":
                    this.path = value;
                    break;
                case "secure":
                    this.secure = true;
                    break;
                case "httponly":
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
        try {
            DateTimeFormatter pattern = date.contains("-") ? FORMATTER_ : FORMATTER;
            LocalDateTime localDateTime = LocalDateTime.parse(date, pattern);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(name).append("=").append(value);

        if (expireTime != null) {
            Instant instant = expireTime.toInstant();
            ZoneId zoneId = ZoneId.systemDefault();
            sb.append("; Expires=").append(FORMATTER.format(instant.atZone(zoneId).toLocalDateTime()));
        }

        if (version != 0) {
            sb.append("; Version=").append(version);
        }

        if (comment != null) {
            sb.append("; Comment=").append(comment);
        }

        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }

        if (maxAge != null) {
            sb.append("; Max-Age=").append(maxAge);
        }

        if (path != null) {
            sb.append("; Path=").append(path);
        }
        if (secure) {
            sb.append("; Secure");
        }

        if (httpOnly) {
            sb.append("; HttpOnly");
        }

        return sb.toString();
    }
}
