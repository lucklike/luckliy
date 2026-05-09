package com.luckyframework.httpclient.proxy.configapi;

import org.springframework.core.io.Resource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义multipart/form-data类型参数
 */
public class MultipartFormData {

    /**
     * 定义multipart/form-data类型的文本参数
     */
    private Map<String, Object> txt = new LinkedHashMap<>();

    /**
     * 定义multipart/form-data类型的文件参数, Value必须是{@link Resource}对象
     */
    private Map<String, Object> file = new LinkedHashMap<>();

    /**
     * 获取multipart/form-data类型的文本参数
     *
     * @return multipart/form-data类型的文本参数
     */
    public Map<String, Object> getTxt() {
        return txt;
    }

    /**
     * 设置multipart/form-data类型的文本参数
     *
     * @param txt multipart/form-data类型的文本参数
     */
    public void setTxt(Map<String, Object> txt) {
        this.txt = txt;
    }

    /**
     * 获取multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     *
     * @return multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     */
    public Map<String, Object> getFile() {
        return file;
    }

    /**
     * 设置multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     *
     * @param file multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     */
    public void setFile(Map<String, Object> file) {
        this.file = file;
    }

    /**
     * 添加multipart/form-data类型的文本参数
     *
     * @param txt multipart/form-data类型的文本参数
     */
    public void putAllTxt(Map<String, Object> txt) {
        this.txt.putAll(txt);
    }

    /**
     * 添加multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     *
     * @param file multipart/form-data类型的文件参数，Value必须是{@link Resource}对象
     */
    public void putAllFile(Map<String, Object> file) {
        this.file.putAll(file);
    }
}
