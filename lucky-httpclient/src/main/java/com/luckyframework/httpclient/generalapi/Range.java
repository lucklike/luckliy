package com.luckyframework.httpclient.generalapi;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.spel.FunctionFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 分片类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 16:48
 */
public class Range {

    private static final Range NOT_SUPPORT = new Range(false, null, -1L);

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

    private Range(boolean support, String filename, long length) {
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
    public static Range create(String filename, long length) {
        return new Range(true, filename, length);
    }

    /**
     * 创建一个支持分片的分片信息实例
     *
     * @param response 响应对象
     * @return 支持分片的分片信息实例
     */
    public static Range create(Response response) {
        String contentLength = String.valueOf(response.getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_LENGTH).getValue());
        String filename = response.getResponseMetaData().getDownloadFilename();
        return create(filename, Long.parseLong(contentLength.trim()));
    }

    /**
     * 创建一个不支持分片的分片信息实例
     *
     * @return 不支持分片的分片信息实例
     */
    public static Range notSupport() {
        return NOT_SUPPORT;
    }


    /**
     * 分片文件索引
     */
    public static class Index {
        private final long begin;
        private final long end;

        public Index(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        public long getBegin() {
            return begin;
        }

        public long getEnd() {
            return end;
        }
    }


    /**
     * 失败原因
     */
    public static class FailCause {
        private final Index index;
        private final List<String> exCauseChain;

        private FailCause(Index index, List<String> exCauseChain) {
            this.index = index;
            this.exCauseChain = exCauseChain;
        }

        public static FailCause forException(Index index, Throwable throwable, int maxChainLength) {
            List<String> exChain = new ArrayList<>();
            int i = 0;
            while (i < maxChainLength && throwable != null) {
                String exInfo = "[" + throwable + "] " + throwable.getMessage();
                exChain.add(exInfo);
                throwable = throwable.getCause();
                i++;
            }
            return new FailCause(index, exChain);
        }

        public static FailCause forException(Index index, Throwable throwable) {
            return forException(index, throwable, 10);
        }


        public Index getIndex() {
            return index;
        }

        public List<String> getExCauseChain() {
            return exCauseChain;
        }
    }
}
