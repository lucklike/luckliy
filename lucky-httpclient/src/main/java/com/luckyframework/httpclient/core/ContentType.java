package com.luckyframework.httpclient.core;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.TempPair;
import org.apache.http.Consts;
import org.apache.http.util.Args;
import org.apache.http.util.TextUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Locale;

/**
 * http context-type
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 10:05 上午
 */
public final class ContentType implements Serializable {

    // constants
    public static final ContentType APPLICATION_ATOM_XML = create(
            "application/atom+xml", Consts.ISO_8859_1);
    public static final ContentType APPLICATION_FORM_URLENCODED = create(
            "application/x-www-form-urlencoded", Consts.ISO_8859_1);
    public static final ContentType APPLICATION_JSON = create(
            "application/json", Consts.UTF_8);
    public static final ContentType APPLICATION_OCTET_STREAM = create(
            "application/octet-stream", (Charset) null);
    public static final ContentType APPLICATION_SVG_XML = create(
            "application/svg+xml", Consts.ISO_8859_1);
    public static final ContentType APPLICATION_XHTML_XML = create(
            "application/xhtml+xml", Consts.ISO_8859_1);
    public static final ContentType APPLICATION_XML = create(
            "application/xml", Consts.ISO_8859_1);
    public static final ContentType MULTIPART_FORM_DATA = create(
            "multipart/form-data", Consts.ISO_8859_1);
    public static final ContentType TEXT_HTML = create(
            "text/html", Consts.ISO_8859_1);
    public static final ContentType TEXT_PLAIN = create(
            "text/plain", Consts.ISO_8859_1);
    public static final ContentType TEXT_XML = create(
            "text/xml", Consts.ISO_8859_1);
    public static final ContentType WILDCARD = create(
            "*/*", (Charset) null);

    // defaults
    public static final ContentType DEFAULT_TEXT = TEXT_PLAIN;
    public static final ContentType DEFAULT_BINARY = APPLICATION_OCTET_STREAM;
    

    private final String mimeType;
    private final Charset charset;
    private final TempPair<String,String>[] params;

    ContentType(String mimeType, Charset charset) {
        this.mimeType = mimeType;
        this.charset = charset;
        this.params = null;
    }

    ContentType(String mimeType, TempPair<String,String>[] params) {
        this.mimeType = mimeType;
        this.params = params;
        final String s = getParameter("charset");
        this.charset = StringUtils.hasText(s) ? Charset.forName(s) : StandardCharsets.ISO_8859_1;
    }

    public String getParameter(final String name) {
        Assert.notNull(name, "Parameter name is null");
        if (this.params == null) {
            return null;
        }
        for (final TempPair<String,String> param: this.params) {
            if (param.getOne().equalsIgnoreCase(name)) {
                return param.getTwo();
            }
        }
        return null;
    }

    public static ContentType create(final String mimeType, final Charset charset) {
        Assert.notNull(mimeType,"MIME type is null");
        final String type = mimeType.toLowerCase(Locale.ENGLISH);
        Args.check(valid(type), "MIME type may not contain reserved characters");
        return new ContentType(type, charset);
    }

    public static ContentType create(
            final String mimeType, final String charset) throws UnsupportedCharsetException {
        return create(mimeType, StringUtils.hasText(charset) ? Charset.forName(charset) : null);
    }

    public static ContentType create(
            final String mimeType, final TempPair<String,String>[] params) throws UnsupportedCharsetException {
        return new ContentType(mimeType, params);
    }


    private static boolean valid(final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if (ch == '"' || ch == ',' || ch == ';') {
                return false;
            }
        }
        return true;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Charset getCharset() {
        return charset;
    }

    public TempPair<String, String>[] getParams() {
        return params;
    }

    @Override
    public String toString() {
        String charsetTemp = charset == null?"":"charset="+charset;
        StringBuilder paramTemp = new StringBuilder();
        if(!ContainerUtils.isEmptyArray(params)){
            for (TempPair<String, String> param : params) {
                paramTemp.append(param.getOne()).append("=").append(param.getTwo()).append(";");
            }
        }
        String paramStr = paramTemp.toString();
        paramStr = paramStr.endsWith(";")?paramStr.substring(0,paramStr.length()-1):paramStr;
        String result = mimeType;
        if(StringUtils.hasText(charsetTemp)){
            result = result+";"+charsetTemp;
        }
        if(StringUtils.hasText(paramStr)){
            result = result+";"+paramStr;
        }
        return result;
    }
}
