package com.luckyframework.httpclient.core.meta;

import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * http context-type
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 10:05 上午
 */
public final class ContentType implements Serializable {


    // constants
    public static final ContentType APPLICATION_JSON = create(MimeTypeUtils.APPLICATION_JSON);

    public static final ContentType APPLICATION_OCTET_STREAM = create(MimeTypeUtils.APPLICATION_OCTET_STREAM);
    public static final ContentType APPLICATION_JAVA_SERIALIZED_OBJECT = create("application", "x-java-serialized-object");
    public static final ContentType APPLICATION_GRAPHQL = create("application", "graphql+json");
    public static final ContentType APPLICATION_PROTOBUF = create("application", "x-protobuf");


    public static final ContentType APPLICATION_XML = create(MimeTypeUtils.APPLICATION_XML);
    public static final ContentType APPLICATION_ATOM_XML = create("application", "atom+xml", StandardCharsets.ISO_8859_1);
    public static final ContentType APPLICATION_SVG_XML = create("application", "svg+xml", StandardCharsets.ISO_8859_1);
    public static final ContentType APPLICATION_XHTML_XML = create("application", "xhtml+xml", StandardCharsets.ISO_8859_1);

    public static final ContentType APPLICATION_FORM_URLENCODED = create("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1);
    public static final ContentType MULTIPART_FORM_DATA = create("multipart", "form-data", StandardCharsets.ISO_8859_1);


    public static final ContentType TEXT_HTML = create(MimeTypeUtils.TEXT_HTML);
    public static final ContentType TEXT_PLAIN = create(MimeTypeUtils.TEXT_PLAIN);
    public static final ContentType TEXT_XML = create(MimeTypeUtils.TEXT_XML);
    public static final ContentType ALL = create(MimeTypeUtils.ALL);

    public static final ContentType NON = create("`non`", "`non`");

    private final MimeType mimeType;

    ContentType(MimeType mimeType) {
        this.mimeType = mimeType;
    }


    public MimeType getMimeType() {
        return mimeType;
    }

    public String getParameter(final String name) {
        return this.mimeType.getParameter(name);
    }

    public Charset getCharset() {
        Charset charset = this.mimeType.getCharset();
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static ContentType create(MimeType mimeType) {
        return new ContentType(mimeType);
    }

    public static ContentType create(String type) {
        return create(new MimeType(type));
    }

    public static ContentType create(String type, String subType) {
        return create(new MimeType(type, subType));
    }

    public static ContentType create(String type, String subType, Charset charset) {
        return create(new MimeType(type, subType, charset));
    }

    public static ContentType create(String type, String subtype, @Nullable Map<String, String> parameters) {
        return create(new MimeType(type, subtype, parameters));
    }

    public static ContentType valueOf(String parseMimeType) {
        return create(MimeType.valueOf(parseMimeType));
    }

    public static ContentType valueOf(String mimeTypeStr, Charset charset) {
        if (charset == null) {
            return valueOf(mimeTypeStr);
        }

        MimeType soureMimeType = MimeType.valueOf(mimeTypeStr);
        return create(soureMimeType.getType(), soureMimeType.getSubtype(), charset);
    }

    public boolean includes(ContentType contentType) {
        return this.mimeType.includes(contentType.getMimeType());
    }

    public boolean isCompatibleWith(ContentType contentType) {
        return this.mimeType.isCompatibleWith(contentType.getMimeType());
    }

    @Override
    public String toString() {
        return this.mimeType.toString();
    }
}
