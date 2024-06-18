package com.luckyframework.httpclient.generalapi;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;

/**
 * 分片信息类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 16:48
 */
public class RangeInfo {

    private static final RangeInfo NOT_SUPPORT = new RangeInfo(false, null, -1L);

    /**
     * 是否支持分片模式
     */
    private final boolean support;

    /**
     * 文件的总大小
     */
    private final long length;

    /**
     * 文件名
     */
    private final String filename;

    private RangeInfo(boolean support, String filename, long length) {
        this.support = support;
        this.length = length;
        this.filename = filename;
    }

    /**
     * 是否支持分片下载
     *
     * @return 是否支持分片下载
     */
    public boolean isSupport() {
        return support;
    }

    /**
     * 获取文件的总大小
     *
     * @return 文件的总大小
     */
    public long getLength() {
        return length;
    }

    /**
     * 获取要下载的文件名
     *
     * @return 要下载的文件名
     */
    public String getFilename() {
        return filename;
    }

    @FunctionFilter
    public static RangeInfo create(String filename, long length) {
        return new RangeInfo(true, filename, length);
    }

    /**
     * 创建一个支持分片的分片信息实例
     *
     * @param response 响应对象
     * @return 支持分片的分片信息实例
     */
    public static RangeInfo create(Response response) {
        String contentRang = String.valueOf(response.getSimpleHeaders().get("Content-Range"));
        String filename = response.getResponseMetaData().getDownloadFilename();
        return create(filename, Long.parseLong(contentRang.split("/")[1]));
    }

    /**
     * 创建一个不支持分片的分片信息实例
     *
     * @return 不支持分片的分片信息实例
     */
    public static RangeInfo notSupport() {
        return NOT_SUPPORT;
    }

}
