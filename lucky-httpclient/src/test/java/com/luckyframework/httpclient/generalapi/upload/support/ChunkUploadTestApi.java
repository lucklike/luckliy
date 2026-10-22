package com.luckyframework.httpclient.generalapi.upload.support;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.upload.ChunkUpload;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadApi;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadResult;
import com.luckyframework.httpclient.proxy.annotations.BinaryBody;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

import java.io.File;
import java.util.List;

/**
 * 分片上传注解端到端测试使用的API接口。
 * <pre>
 *     它同时演示了两种协议适配方式：
 *     1.协议方法签名与{@link ChunkUploadApi}一致时，直接用注解声明（{@code mergeChunks}）；
 *     2.签名不一致时，用default方法做参数拆解（{@code isChunkUploaded}/{@code uploadChunk}），
 *       接口代理会直接执行default方法体，方法体内的调用仍然会发出真实HTTP请求。
 *
 *     假服务端的端口在测试运行时才确定，所以域名交给{@code urlFunc}指定的函数动态获取。
 * </pre>
 */
@ServerAddress(urlFunc = "chunk_upload_test_server_url")
public interface ChunkUploadTestApi extends ChunkUploadApi {

    /**
     * 单个分片的字节数，与测试用例里的文件长度配合，切出6个分片
     */
    long TEST_CHUNK_SIZE = 5L;

    /**
     * 断点续传判据：查服务端已收到的片号集合，再与当前分片对账，
     * 假服务端返回{@code {"code":200,"data":[1,3]}}，所以用{@code @RespConvert}取data字段
     *
     * @param fileId    文件唯一标识
     * @param fileChunk 待判断的分片
     * @return 服务端是否已有该分片
     */
    @Override
    default boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
        return uploadedChunks(fileId).contains(fileChunk.getIndex());
    }

    /**
     * 查询服务端已收到的分片索引，供{@link #isChunkUploaded}对账使用
     *
     * @param fileId 文件唯一标识
     * @return 服务端已有的分片索引
     */
    @Get("uploadChunks?fileId=#{fileId}")
    @RespConvert("#{$body$.data}")
    List<Long> uploadedChunks(String fileId);

    @Override
    default boolean uploadChunk(String fileId, FileChunk fileChunk) {
        return uploadChunkBytes(fileId, fileChunk.getIndex(), fileChunk.getContent());
    }

    /**
     * 以二进制请求体上传单个分片的原始字节
     * <pre>
     *     服务端返回的是{@code {"code":200,"data":true}}这样的包装结果，
     *     因此需要{@code @RespConvert}把data取出来再转成boolean。
     * </pre>
     *
     * @param fileId      文件唯一标识
     * @param chunkNumber 分片索引
     * @param content     分片内容
     * @return 服务端是否接收成功
     */
    @Post("chunk?fileId=#{fileId}&chunkNumber=#{chunkNumber}")
    @RespConvert("#{$body$.data}")
    boolean uploadChunkBytes(String fileId, long chunkNumber, @BinaryBody byte[] content);

    /**
     * 通知服务端合并：签名与协议方法对齐后用注解声明，文件名从原始File取用后拼到URL上
     *
     * @param fileId      文件唯一标识
     * @param file        待上传的原始文件
     * @param totalChunks 分片总数
     * @return 是否合并成功
     */
    @Override
    default boolean mergeChunks(String fileId, File file, int totalChunks) {
        return mergeRequest(fileId, file.getName(), totalChunks);
    }

    /**
     * 合并请求的真实接口
     *
     * @param fileId      文件唯一标识
     * @param fileName    服务端保存时使用的文件名
     * @param totalChunks 分片总数
     * @return 是否合并成功
     */
    @Post("merge?fileId=#{fileId}&fileName=#{fileName}&totalChunks=#{totalChunks}")
    @RespConvert("#{$body$.data}")
    boolean mergeRequest(String fileId, String fileName, int totalChunks);

    /**
     * 分片上传并返回完整结果
     *
     * @param file 待上传的文件
     * @return 上传结果
     */
    @ChunkUpload(chunkSize = TEST_CHUNK_SIZE, maxConcurrency = 2, maxRetryCount = 2, retryBackoffMillis = 0)
    ChunkUploadResult upload(File file);

    /**
     * 分片上传并只返回是否成功
     *
     * @param file 待上传的文件
     * @return 整个流程是否成功
     */
    @ChunkUpload(chunkSize = TEST_CHUNK_SIZE, maxConcurrency = 2, maxRetryCount = 1, retryBackoffMillis = 0)
    boolean uploadQuietly(File file);

    /**
     * 由调用方指定fileId的分片上传，用于断点续传等需要预置服务端分片的用例
     *
     * @param file   待上传的文件
     * @param fileId 文件唯一标识
     * @return 上传结果
     */
    @ChunkUpload(fileId = "#{fileId}", chunkSize = TEST_CHUNK_SIZE, maxConcurrency = 2, maxRetryCount = 2, retryBackoffMillis = 0)
    ChunkUploadResult uploadWithFileId(File file, String fileId);

    /**
     * 静默上传：返回值声明为void时只执行流程，不取上传结果
     *
     * @param file 待上传的文件
     */
    @ChunkUpload(fileId = "silent-upload", chunkSize = TEST_CHUNK_SIZE, maxConcurrency = 2, maxRetryCount = 1, retryBackoffMillis = 0)
    void uploadSilently(File file);


    /**
     * 当前正在运行的假服务端地址
     *
     * @return 地址
     */
    @FunctionAlias("chunk_upload_test_server_url")
    static String serverUrl() {
        return ChunkUploadTestServer.currentBaseUrl();
    }

}
