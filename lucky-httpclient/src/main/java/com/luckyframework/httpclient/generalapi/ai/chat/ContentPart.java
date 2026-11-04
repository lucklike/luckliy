package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息内容分片，本次覆盖文本分片与图片分片两种。
 * 图片分片统一以URL形态承载：既接受公网URL，也接受 {@code data:<mime>;base64,<数据>} 形式的内联数据，
 * 方言层负责把内联数据翻译成各家的结构。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentPart {

    /**
     * 文本分片类型标识
     */
    public static final String TYPE_TEXT = "text";

    /**
     * 图片分片类型标识
     */
    public static final String TYPE_IMAGE_URL = "image_url";

    private static final String DATA_PREFIX = "data:";
    private static final String BASE64_MARKER = ";base64,";

    private String type;
    private String text;
    private ImageUrl imageUrl;

    public ContentPart() {
    }

    private ContentPart(String type, String text, ImageUrl imageUrl) {
        this.type = type;
        this.text = text;
        this.imageUrl = imageUrl;
    }

    /**
     * 构造文本分片
     */
    public static ContentPart text(String text) {
        return new ContentPart(TYPE_TEXT, text, null);
    }

    /**
     * 构造图片分片（公网URL）
     */
    public static ContentPart image(String url) {
        return new ContentPart(TYPE_IMAGE_URL, null, new ImageUrl(url));
    }

    /**
     * 构造图片分片（base64内联数据），内部以 data URI 形式承载
     */
    public static ContentPart imageBase64(String mimeType, String base64Data) {
        return image(DATA_PREFIX + mimeType + BASE64_MARKER + base64Data);
    }

    /**
     * 判断图片分片是否为base64内联数据
     */
    @JsonIgnore
    public boolean isInlineImage() {
        return imageUrl != null && imageUrl.getUrl() != null && imageUrl.getUrl().startsWith(DATA_PREFIX);
    }

    /**
     * 取内联数据的媒体类型，非内联数据时返回null
     */
    @JsonIgnore
    public String getInlineMimeType() {
        if (!isInlineImage()) {
            return null;
        }
        String url = imageUrl.getUrl();
        return url.substring(DATA_PREFIX.length(), url.indexOf(BASE64_MARKER));
    }

    /**
     * 取内联数据的base64正文，非内联数据时返回null
     */
    @JsonIgnore
    public String getInlineBase64Data() {
        if (!isInlineImage()) {
            return null;
        }
        String url = imageUrl.getUrl();
        return url.substring(url.indexOf(BASE64_MARKER) + BASE64_MARKER.length());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("image_url")
    public ImageUrl getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("image_url")
    public void setImageUrl(ImageUrl imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 图片分片的地址载体
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageUrl {

        private String url;
        private String detail;

        public ImageUrl() {
        }

        public ImageUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
