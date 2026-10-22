package com.luckyframework.httpclient.generalapi.upload.support;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.upload.ChunkUpload;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadApi;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadResult;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.MultiData;
import com.luckyframework.httpclient.proxy.annotations.MultiFile;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.Query;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

import java.io.File;
import java.util.List;

/**
 * 文档示例同款协议的分片上传API：分片走multipart/form-data表单，查询与合并的参数走URL。
 * <pre>
 *     声明这个接口只为验证文档中的写法确实可用，属性名与注解组合与
 *     {@code docs/simple/annotation-api/35.分片文件上传.md} 保持一致：
 *     1.服务端统一返回{@code {"code":200,"data":..}}包装结果，接口用{@code @RespConvert}取出data；
 *     2.上传分片用{@code @MultiData}上报元信息、用{@code @MultiFile}上报分片字节；
 *     3.合并接口用{@code @Query}把参数拼到URL上。
 * </pre>
 */
@ServerAddress(urlFunc = "chunk_upload_test_server_url")
@SpELImport(ChunkUploadMultipartTestApi.UrlFunctions.class)
public interface ChunkUploadMultipartTestApi extends ChunkUploadApi {

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
    @Get("uploadChunks")
    @RespConvert("#{$body$.data}")
    List<Long> uploadedChunks(@Query("fileId") String fileId);

    @Override
    default boolean uploadChunk(String fileId, FileChunk fileChunk) {
        return uploadChunkForm(fileId, fileChunk.getIndex(), fileChunk.getContent());
    }

    /**
     * 以multipart表单方式上传单个分片
     *
     * @param fileId      文件唯一标识
     * @param chunkNumber 分片索引
     * @param content     分片内容
     * @return 服务端是否接收成功
     */
    @Post("chunkMultipart")
    @RespConvert("#{$body$.data}")
    boolean uploadChunkForm(@MultiData("fileId") String fileId,
                            @MultiData("chunkNumber") long chunkNumber,
                            @MultiFile(value = "file", fileName = "chunk") byte[] content);

    @Override
    default boolean mergeChunks(String fileId, File file, int totalChunks) {
        return mergeRequest(fileId, file.getName(), totalChunks);
    }

    /**
     * 合并请求的真实接口，参数用{@code @Query}拼到URL上
     *
     * @param fileId      文件唯一标识
     * @param fileName    服务端保存时使用的文件名
     * @param totalChunks 分片总数
     * @return 是否合并成功
     */
    @Post("merge")
    @RespConvert("#{$body$.data}")
    boolean mergeRequest(@Query("fileId") String fileId,
                         @Query("fileName") String fileName,
                         @Query("totalChunks") int totalChunks);

    /**
     * 分片上传并返回完整结果
     *
     * @param file 待上传的文件
     * @return 上传结果
     */
    @ChunkUpload(chunkSize = "" + ChunkUploadTestApi.TEST_CHUNK_SIZE, maxConcurrency = "2", maxRetryCount = "2", retryBackoffMillis = "0")
    ChunkUploadResult upload(File file);

    /**
     * 分片上传的URL提供者，端口在测试运行时才确定
     */
    class UrlFunctions {

        /**
         * 当前正在运行的假服务端地址
         *
         * @return 地址
         */
        @FunctionAlias("chunk_upload_test_server_url")
        public static String serverUrl() {
            return ChunkUploadTestServer.currentBaseUrl();
        }
    }
}
