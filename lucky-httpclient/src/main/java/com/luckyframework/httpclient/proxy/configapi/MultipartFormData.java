package com.luckyframework.httpclient.proxy.configapi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义multipart/form-data类型的文本参数
 */
public class MultipartFormData {

    /**
     * 定义multipart/form-data类型的文本参数
     */
    private Map<String, Object> txt = new LinkedHashMap<>();

    /**
     * 定义multipart/form-data类型的文件参数
     */
    private Map<String, Object> file = new LinkedHashMap<>();


    public Map<String, Object> getTxt() {
        return txt;
    }

    public void setTxt(Map<String, Object> txt) {
        this.txt = txt;
    }

    public Map<String, Object> getFile() {
        return file;
    }

    public void setFile(Map<String, Object> file) {
        this.file = file;
    }

    public void putAllTxt(Map<String, Object> txt) {
        this.txt.putAll(txt);
    }

    public void putAllFile(Map<String, Object> file) {
        this.file.putAll(file);
    }
}
