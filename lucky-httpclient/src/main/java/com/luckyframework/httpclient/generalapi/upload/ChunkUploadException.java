package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 分片文件上传异常。
 * <pre>
 *     只用于「上传根本没跑起来」的场景，例如注解属性解析出的文件类型不支持、
 *     找不到待上传的文件、未配置服务端协议接口、方法返回值类型不被支持、计算fileId时读取文件失败。
 *
 *     与之相对，分片传输失败、重试用尽、合并失败都不会抛出本异常，
 *     这些情况一律通过{@link ChunkUploadResult#isSuccess()}与{@link ChunkUploadResult#getFailedChunks()}表达，
 *     见{@link ChunkUploadExecutor}的失败处理约定。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-21 11:40
 * @see ChunkUpload
 * @see ChunkUploadExecutor
 */
public class ChunkUploadException extends LuckyRuntimeException {

    public ChunkUploadException(String message) {
        super(message);
    }

    public ChunkUploadException(Throwable ex) {
        super(ex);
    }

    public ChunkUploadException(String message, Throwable ex) {
        super(message, ex);
    }

    public ChunkUploadException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ChunkUploadException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
