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

    private Object json;
    private Object xml;
    private Object form;
    private Object java;
    private Object protobuf;

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

    public Object getJson() {
        return json;
    }

    public void setJson(Object json) {
        this.json = json;
    }

    public Object getXml() {
        return xml;
    }

    public void setXml(Object xml) {
        this.xml = xml;
    }

    public Object getForm() {
        return form;
    }

    public void setForm(Object form) {
        this.form = form;
    }

    public Object getJava() {
        return java;
    }

    public void setJava(Object java) {
        this.java = java;
    }

    public Object getProtobuf() {
        return protobuf;
    }

    public void setProtobuf(Object protobuf) {
        this.protobuf = protobuf;
    }
}
