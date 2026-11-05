package com.luckyframework.httpclient.generalapi.upload.support;

import com.luckyframework.httpclient.generalapi.upload.ChunkUpload;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadApi;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadMeta;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadResult;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.Query;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;

import java.io.File;

/**
 * 文件标识由服务端生成时的端到端测试API。
 * <pre>
 *     与{@link ChunkUploadTestApi}只有一处差别：覆写了{@link ChunkUploadApi#initUpload}，
 *     在切片之前先请求服务端创建一个上传任务，用返回的任务ID作为后续分片、秒传探测与合并的fileId。
 *     因此{@link #uploadWithServerFileId}方法上<b>没有</b>配置fileId属性，
 *     内核会走「服务端生成 → 文件内容MD5」这条解析链；
 *     而{@link #uploadWithConfiguredFileId}配置了fileId，服务端生成能力就不会被用到。
 *     分片与合并协议直接复用父接口的实现，验证不带{@code File}参数的
 *     {@link ChunkUploadMeta}同样能驱动完整链路。
 * </pre>
 */
@ServerAddress(urlFunc = "chunk_upload_test_server_url")
public interface ChunkUploadServerFileIdTestApi extends ChunkUploadTestApi {

    /**
     * 申请文件标识：把文件级元信息报给服务端，由服务端生成任务ID
     *
     * @param meta 文件级元信息，此时fileId为null
     * @return 服务端生成的任务ID
     */
    @Override
    default String initUpload(ChunkUploadMeta meta) {
        return createTask(meta.getFileName(), meta.getTotalSize(), meta.getTotalChunks());
    }

    /**
     * 创建上传任务，服务端返回{@code {"code":200,"data":"srv-task-1"}}形式的任务ID
     *
     * @param fileName    服务端保存时使用的文件名
     * @param totalSize   文件总大小
     * @param totalChunks 分片总数
     * @return 服务端生成的任务ID
     */
    @Post("task/create")
    @RespConvert("#{$body$.data}")
    String createTask(@Query("fileName") String fileName,
                      @Query("totalSize") long totalSize,
                      @Query("totalChunks") int totalChunks);

    /**
     * 不配置fileId的分片上传：标识由服务端生成，用于验证整条解析链
     *
     * @param file 待上传的文件
     * @return 上传结果
     */
    @ChunkUpload(chunkSize = "" + TEST_CHUNK_SIZE, maxConcurrency = "2", maxRetryCount = "2", retryBackoffMillis = "0")
    ChunkUploadResult uploadWithServerFileId(File file);

    /**
     * 同时配置了fileId属性：标识由注解直接给出，不会再去请求服务端创建任务
     *
     * @param file 待上传的文件
     * @return 上传结果
     */
    @ChunkUpload(fileId = "client-file-id", chunkSize = "" + TEST_CHUNK_SIZE, maxConcurrency = "2",
            maxRetryCount = "2", retryBackoffMillis = "0")
    ChunkUploadResult uploadWithConfiguredFileId(File file);
}
