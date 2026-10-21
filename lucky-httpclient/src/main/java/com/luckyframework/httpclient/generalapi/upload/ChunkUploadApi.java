package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;

import java.util.Collections;
import java.util.List;

/**
 * 分片上传的服务端协议接口。
 * <pre>
 *     HTTP分片上传没有像下载（{@code Range}请求头）那样的标准协议，不同服务端的
 *     「查询已上传分片、上传单个分片、通知合并」三个接口的URL、参数名、响应格式都不一样，
 *     因此本接口只约定<b>动作</b>，具体协议由使用者用自己的API接口去实现。
 *     实现时只需把下面三个方法接到真实接口上，例如：
 *     {@code
 *     @ServerAddress("http://localhost:8864/shard/upload/")
 *     public interface FileChunkUploadApi extends ChunkUploadApi {
 *
 *         // 1.查询已上传分片：签名与本接口一致，直接用注解声明；
 *         //   服务端返回{"code":200,"data":[1,3]}，取data字段就是分片索引集合
 *         @Override
 *         @Get("uploadChunks")
 *         @RespConvert("#{$body$.data}")
 *         List<Long> uploadedChunkIndexes(@Query("fileId") String fileId);
 *
 *         // 2.上传单个分片：本接口给的是FileChunk对象，表单协议要的是拆开的字段，
 *         //   用default方法拆解即可；接口代理会直接执行default方法体，
 *         //   方法体内的调用仍然会发出真实的HTTP请求
 *         @Override
 *         default boolean uploadChunk(String fileId, FileChunk fileChunk) {
 *             return uploadChunkForm(fileId, fileChunk.getIndex(), fileChunk.getContent());
 *         }
 *
 *         @Post("chunk")
 *         boolean uploadChunkForm(@MultiData("fileId") String fileId,
 *                                 @MultiData("chunkNumber") long chunkNumber,
 *                                 @MultiFile(value = "file", fileName = "chunk") byte[] content);
 *
 *         // 3.通知服务端合并
 *         @Override
 *         @Post("merge")
 *         boolean mergeChunks(@Query("fileId") String fileId,
 *                             @Query("fileName") String fileName,
 *                             @Query("totalChunks") int totalChunks);
 *     }
 *     }
 *
 *     需要上报哪些参数由服务端协议决定：除了{@code fileChunk.getIndex()}与{@code fileChunk.getContent()}，
 *     {@link FileChunk}还提供{@code getStart()/getEnd()/getSize()}与
 *     {@code getTotalSize()/getChunkSize()/getTotalChunks()}，需要把区间或切分元数据一并上报时直接取用；
 *     分片内容也可以改用{@code fileChunk.writeContentTo(out)}流式写出，避免整片驻留内存。
 *
 *     实现时的三条约定：
 *     1.三个方法内部可以正常使用本框架的注解（{@code @Post}/{@code @MultiFile}/{@code @Retryable}等），
 *       它们会各自发出真实的HTTP请求；
 *     2.返回值用{@code boolean}表达业务成功与否即可，抛异常会被框架记录为分片执行失败并纳入重试，
 *       返回{@code false}同样会被视为需要重试的分片；
 *     3.声明为{@code boolean}的方法要求服务端返回能被自动转成布尔值的响应体，
 *       {@code text/plain}会被框架原样当字符串返回而无法转成{@code boolean}，
 *       这类接口请返回{@code application/json}（如响应体直接是{@code true}），
 *       或者用{@code @RespConvert}从JSON包装结果里取出布尔值。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/10/21 10:45
 * @see ChunkUploadExecutor
 * @see ChunkUpload
 */
public interface ChunkUploadApi {

    /**
     * 查询服务端已经收到的分片索引
     * <pre>
     *     用于断点续传：返回的分片会在上传前被跳过，不再重复传输。
     *     默认返回空集合，即服务端没有提供该查询接口时不做跳过、全量上传。
     * </pre>
     *
     * @param fileId 文件唯一标识
     * @return 服务端已有的分片索引集合（分片索引从1开始），不允许返回null
     */
    default List<Long> uploadedChunkIndexes(String fileId) {
        return Collections.emptyList();
    }

    /**
     * 上传单个分片
     *
     * @param fileId    文件唯一标识
     * @param fileChunk 待上传的分片，可通过它取得索引、区间、本片内容以及
     *                  {@code totalSize/chunkSize/totalChunks}等协议参数
     * @return 该分片是否上传成功；返回{@code false}或抛出异常都会让框架在下一轮重试该分片
     */
    boolean uploadChunk(String fileId, FileChunk fileChunk);

    /**
     * 通知服务端合并全部分片
     * <pre>
     *     只有在所有分片都上传成功之后才会被调用（由{@link ChunkUploadOptions#isMergeAfterSuccess()}控制），
     *     因此本方法无需再自行校验分片完整性。
     * </pre>
     *
     * @param fileId      文件唯一标识
     * @param fileName    服务端保存时使用的文件名
     * @param totalChunks 分片总数，空文件为0
     * @return 是否合并成功
     */
    boolean mergeChunks(String fileId, String fileName, int totalChunks);
}
