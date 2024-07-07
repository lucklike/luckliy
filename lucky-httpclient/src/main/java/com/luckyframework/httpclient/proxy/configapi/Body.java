package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:15
 */
public class Body {

    @TargetField("mime-type")
    private String mimeType;
    private String charset = "UTF-8";
    private String data;
    private String file;

    private String json;
    private String xml;
    private String form;
    private String java;
    private String protobuf;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getJava() {
        return java;
    }

    public void setJava(String java) {
        this.java = java;
    }

    public String getProtobuf() {
        return protobuf;
    }

    public void setProtobuf(String protobuf) {
        this.protobuf = protobuf;
    }
}
