package com.luckyframework.httpclient.generalapi.chunk;

/**
 * 分片文件处理结果
 *
 * @param <R> 结果类型
 */
public class HandleResult<R> {

    /**
     * 分片文件信息
     */
    private final FileChunk fileChunk;

    /**
     * 返回结果
     */
    private final R result;


    private HandleResult(FileChunk fileChunk, R result) {
        this.fileChunk = fileChunk;
        this.result = result;
    }

    public static <R> HandleResult<R> of(FileChunk fileChunk, R result) {
        return new HandleResult<>(fileChunk, result);
    }

    public FileChunk getFileChunk() {
        return fileChunk;
    }

    public R getResult() {
        return result;
    }
}
