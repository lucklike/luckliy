package com.luckyframework.httpclient.proxy.mock.config;

/**
 * Body 相关的 Mock 配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/4/30 00:59
 */
public class MockBody {

    /**
     * 模拟文本类型的响应体
     */
    private String txt;

    /**
     * 模拟文件类型的响应体
     */
    private String file;

    /**
     * 获取模拟文本类型的响应体
     *
     * @return 文本类型的响应体
     */
    public String getTxt() {
        return txt;
    }

    /**
     * 模拟文本类型的响应体
     *
     * @param txt 文本类型的响应体
     */
    public void setTxt(String txt) {
        this.txt = txt;
    }

    /**
     * 获取模拟文件类型的响应体
     *
     * @return 文件类型的响应体
     */
    public String getFile() {
        return file;
    }

    /**
     * 模拟文件类型的响应体
     *
     * @param file 文件类型的响应体
     */
    public void setFile(String file) {
        this.file = file;
    }
}
