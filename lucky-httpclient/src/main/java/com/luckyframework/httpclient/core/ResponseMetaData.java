package com.luckyframework.httpclient.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * 响应原数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/5 13:09
 */
public class ResponseMetaData {

    private final Request request;
    private final int status;
    private final HttpHeaderManager responseHeader;
    private final InputStreamFactory inputStreamFactory;

    private String downloadFilename;

    public ResponseMetaData(Request request, int status, HttpHeaderManager responseHeader, InputStreamFactory inputStreamFactory) {
        this.request = request;
        this.status = status;
        this.responseHeader = responseHeader;
        this.inputStreamFactory = inputStreamFactory;
    }

    /**
     * 获取请求实例
     * @return 请求实例
     */
    public Request getRequest() {
        return request;
    }

    /**
     * 获取响应的状态码
     * @return 响应的状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取响应头信息
     * @return 响应头信息
     */
    public HttpHeaderManager getResponseHeader() {
        return responseHeader;
    }

    /**
     * 获取响应体内容对应的InputStream
     * @return 响应体内容对应的InputStream
     * @throws IOException 获取失败时会抛出该异常
     */
    public InputStream getInputStream() throws IOException {
        return inputStreamFactory.getInputStream();
    }

    /**
     * 当前请求是否成功响应，状态为200时表示成功，其他表示失败
     * @return 当前请求是否成功响应
     */
    public boolean isSuccess() {
        return status == 200;
    }

    /**
     * 获取请求URL
     * @return 取请求URL
     */
    public String getRequestUrl() {
        return this.request.getUrl();
    }

    /**
     * 文件下载场景时使用，用于获取当前正在下载的文件名称
     * @return 当前正在下载的文件名称
     */
    public String getDownloadFilename() {
        if (downloadFilename == null) {
            this.downloadFilename = ResourceNameParser.getResourceName(this);
        }
        return downloadFilename;
    }
}
