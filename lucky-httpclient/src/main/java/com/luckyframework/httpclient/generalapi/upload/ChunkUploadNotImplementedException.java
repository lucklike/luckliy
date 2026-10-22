package com.luckyframework.httpclient.generalapi.upload;

/**
 * 分片上传协议方法未实现异常。
 * <pre>
 *     {@link ChunkUploadApi#uploadChunk}与{@link ChunkUploadApi#mergeChunks}没有任何可兜底的
 *     服务端协议假设，接口默认实现只抛本异常，用来在子类/子接口忘记覆写时于调用第一时间
 *     给出明确提示（缺哪个动作、怎么补、不补的后果），而不是带着空实现反复重试、
 *     把配置错误拖成传输失败。
 *
 *     与之相对，分片传输失败、重试用尽、合并失败都不会抛出本异常，
 *     这些情况一律通过{@link ChunkUploadResult#isSuccess()}与{@link ChunkUploadResult#getFailedChunks()}表达，
 *     见{@link ChunkUploadExecutor}的失败处理约定。
 * </pre>
 *
 * @author fukang
 * @version 1.1.0
 * @date 2026-10-22 09:30
 * @see ChunkUploadApi
 * @see ChunkUploadException
 */
public class ChunkUploadNotImplementedException extends ChunkUploadException {

    /**
     * 构造未实现异常
     *
     * @param action          未完成的服务端动作描述，如{@code upload a single chunk}
     * @param methodSignature 未覆写的协议方法签名，如{@code uploadChunk(String, FileChunk)}
     * @param consequence     不覆写时的业务后果描述
     */
    public ChunkUploadNotImplementedException(String action, String methodSignature, String consequence) {
        super("ChunkUpload: the protocol action '{}' is not implemented -- ChunkUploadApi.{} is still the default "
                + "implementation, and it throws this exception because no server-side protocol can be assumed for it. "
                + "Please override this method on the interface that extends ChunkUploadApi: declare the real request "
                + "with framework annotations (@Post/@Query/@MultiFile etc.), or override the default method to "
                + "delegate to such an annotated method. Without the override, {}.", action, methodSignature, consequence);
    }
}
