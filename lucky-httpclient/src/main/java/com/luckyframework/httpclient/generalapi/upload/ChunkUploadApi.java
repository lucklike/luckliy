package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

import java.io.File;

/**
 * 分片上传的服务端协议接口。
 * <pre>
 *     HTTP分片上传没有像下载（{@code Range}请求头）那样的标准协议，不同服务端的
 *     「判断分片是否已上传、上传单个分片、通知合并、秒传探测、删除分片」各接口的URL、参数名、响应格式都不一样，
 *     因此本接口只约定<b>动作</b>，具体协议由使用者用自己的API接口去实现。
 *     「上传单个分片」「通知合并」「删除分片」「秒传探测」四个动作都没有可兜底的协议假设，
 *     默认实现直接抛{@link ChunkUploadNotImplementedException}；其中「上传单个分片」「通知合并」<b>必须</b>覆写，
 *     「删除分片」只在开启「合并失败时删除分片」（{@link ChunkUploadOptions#isDeleteOnMergeFailure()}）时才被调用，
 *     「秒传探测」只在开启{@link ChunkUploadOptions#isInstantUpload()}时才被调用；
 *     两个可选动作不开启就无需覆写，开启而未覆写时，该异常会被内核记录进{@link ChunkUploadResult}与日志，不会向外抛；
 *     「判断分片是否已上传」默认返回{@code false}，即服务端没有查询能力时不做跳过、全量上传，
 *     需要断点续传时覆写该方法，按真实查询协议对账即可。
 *
 *     实现时只需把下面几个方法接到真实接口上，例如：
 *     {@code
 *     @ServerAddress("http://localhost:8864/shard/upload/")
 *     public interface FileChunkUploadApi extends ChunkUploadApi {
 *
 *         // 1.判断分片是否已上传：先查服务端已有的片号集合，再与当前分片对账；
 *         //   本方法会被每个分片调用一次，片数多时在覆写实现里缓存查询结果或转成Set再判断
 *         @Override
 *         default boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
 *             return uploadedChunks(fileId).contains(fileChunk.getIndex());
 *         }
 *
 *         @Get("uploadChunks")
 *         @RespConvert("#{$body$.data}")
 *         List<Long> uploadedChunks(@Query("fileId") String fileId);
 *
 *         // 2.上传单个分片：本接口给的是FileChunk对象，表单协议要的是拆开的字段，
 *         //   在覆写实现里拆解即可；接口代理会直接执行default方法体，
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
 *         // 3.通知服务端合并：收到的是原始文件，文件名、大小等按需从File取用再上报；
 *         //   合并成功后服务端若返回内部文件ID/下载URL，放进ChunkUploadResponse的data原样带回
 *         @Override
 *         default ChunkUploadResponse mergeChunks(String fileId, File file, int totalChunks) {
 *             return mergeRequest(fileId, file.getName(), totalChunks);
 *         }
 *
 *         @Post("merge")
 *         @RespConvert("#{$body$.data}")
 *         ChunkUploadResponse mergeRequest(@Query("fileId") String fileId,
 *                                          @Query("fileName") String fileName,
 *                                          @Query("totalChunks") int totalChunks);
 *
 *         // 4.秒传探测：切片之前先问服务端「这个文件是否已存在」，data可携带服务端文件ID；
 *         //   只在开启instantUpload时被调用，不开启无需覆写
 *         @Override
 *         default ChunkUploadResponse instantUpload(String fileId, File file) {
 *             return existsRequest(fileId, DigestFunctions.md5Hex(file));
 *         }
 *
 *         @Get("instant")
 *         @RespConvert("#{$body$.data}")
 *         ChunkUploadResponse existsRequest(@Query("fileId") String fileId, @Query("md5") String md5);
 *
 *         // 5.删除服务端已收到的分片：可选动作，只在开启「合并失败时删除分片」且合并
 *         //   实际发起仍失败后被调用；不启用该开关就不用覆写，启用则必须覆写，
 *         //   否则默认实现抛ChunkUploadNotImplementedException
 *         @Override
 *         @Delete("chunks")
 *         @RespConvert("#{$body$.data}")
 *         boolean deleteChunks(@Query("fileId") String fileId);
 *     }
 *     }
 *
 *     需要上报哪些参数由服务端协议决定：除了{@code fileChunk.getIndex()}与{@code fileChunk.getContent()}，
 *     {@link FileChunk}还提供{@code getStart()/getEnd()/getSize()}与
 *     {@code getTotalSize()/getChunkSize()/getTotalChunks()}，需要把区间或切分元数据一并上报时直接取用；
 *     分片内容也可以改用{@code fileChunk.writeContentTo(out)}流式写出，避免整片驻留内存。
 *
 *     实现时的五条约定：
 *     1.各方法内部可以正常使用本框架的注解（{@code @Post}/{@code @MultiFile}/{@code @Retryable}等），
 *       它们会各自发出真实的HTTP请求；
 *     2.返回值用{@code boolean}表达业务成功与否即可，抛异常会被框架记录为分片执行失败并纳入重试，
 *       返回{@code false}同样会被视为需要重试的分片；
 *     3.声明为{@code boolean}的方法要求服务端返回能被自动转成布尔值的响应体，
 *       {@code text/plain}会被框架原样当字符串返回而无法转成{@code boolean}，
 *       这类接口请返回{@code application/json}（如响应体直接是{@code true}），
 *       或者用{@code @RespConvert}从JSON包装结果里取出布尔值；
 *     4.框架不假定服务端用片号标识分片：{@code isChunkUploaded}的覆写实现决定怎么对账，
 *       片号之外的标识（分片ID、内容MD5等）覆写该方法、从{@code fileChunk}
 *       （索引/区间/大小/内容）取值比对即可；
 *     5.{@code deleteChunks}是唯一的「失败清理」动作，只在合并实际发起且仍失败后被调用；
 *       分片没传完而未发起合并时不会调用它——那些分片留着下次续传还有用；
 *       与{@code uploadChunk}/{@code mergeChunks}一样没有可兜底的协议假设，默认实现抛
 *       {@link ChunkUploadNotImplementedException}：不启用删除开关就无需覆写，启用则必须覆写。
 *     6.「合并」「秒传」两个动作返回{@link ChunkUploadResponse}而非{@code boolean}：
 *       框架只判读{@code isSuccess()}，{@code getData()}是服务端附加信息的透传通道
 *       （内部文件ID、下载URL等），原样落进{@link ChunkUploadResult#getServerData()}，
 *       没有附加信息时返回{@code ChunkUploadResponse.success()}即可。
 * </pre>
 *
 * @author fukang
 * @version 4.0.0
 * @date 2026-10-21 10:45
 * @see ChunkUploadExecutor
 * @see ChunkUpload
 */
public interface ChunkUploadApi {

    /**
     * 当前分片服务端是否已经收到，即「分片用什么标识」的扩展点
     * <pre>
     *     用于断点续传：命中的分片会在上传前被跳过，不再重复传输。
     *     本方法对每个分片各调用一次，服务端协议是「一次查询全部片号」时，
     *     在覆写实现里自己完成查询与比对（{@code fileChunk.getIndex()}即片号），
     *     片数多且报文量大时建议先转成Set再判断，避免逐片远程查询。
     *     默认返回{@code false}，即不覆写就等价于「服务端没有查询接口」，每次全量上传。
     * </pre>
     *
     * @param fileId    文件唯一标识，与{@link #uploadChunk}收到的是同一个值
     * @param fileChunk 待判断的分片，可从中取得索引、区间、大小乃至内容用于比对
     * @return true表示服务端已有该分片，本次可以跳过
     */
    default boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
        return false;
    }

    /**
     * 上传单个分片
     * <pre>
     *     没有可兜底的默认协议，必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，该异常会走分片失败重试通道被原样上报。
     * </pre>
     *
     * @param fileId    文件唯一标识
     * @param fileChunk 待上传的分片，可通过它取得索引、区间、本片内容以及
     *                  {@code totalSize/chunkSize/totalChunks}等协议参数
     * @return 该分片是否上传成功；返回{@code false}或抛出异常都会让框架在下一轮重试该分片
     */
    default boolean uploadChunk(String fileId, FileChunk fileChunk) {
        throw new ChunkUploadNotImplementedException("upload a single chunk", "uploadChunk(String, FileChunk)",
                "no chunk can reach the server and the file will never be uploaded completely");
    }

    /**
     * 通知服务端合并全部分片
     * <pre>
     *     没有可兜底的默认协议，必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，由框架记录为合并失败。
     *     只有在所有分片都上传成功之后才会被调用（由{@link ChunkUploadOptions#isMergeAfterSuccess()}控制），
     *     因此本方法无需再自行校验分片完整性。
     *     返回值用{@link ChunkUploadResponse}表达成败；服务端合并后若返回内部文件ID、下载URL等附加信息，
     *     放进{@code data}原样带回即可，框架只判读{@code isSuccess()}，
     *     {@code data}会透传到{@link ChunkUploadResult#getServerData()}供调用方取用。
     * </pre>
     *
     * @param fileId      文件唯一标识
     * @param file        待上传的原始文件，可从中取得文件名（{@code getName()}）、
     *                    大小（{@code length()}）等信息按协议上报
     * @param totalChunks 分片总数，空文件为0
     * @return 合并响应；{@code isSuccess()}为{@code false}或抛出异常都会让框架记录合并失败
     */
    default ChunkUploadResponse mergeChunks(String fileId, File file, int totalChunks) {
        throw new ChunkUploadNotImplementedException("notify the server to merge all chunks", "mergeChunks(String, File, int)",
                "the server will never produce the merged file");
    }

    /**
     * 秒传探测：上传前询问服务端该文件是否已存在
     * <pre>
     *     只在开启{@link ChunkUploadOptions#isInstantUpload()}时被调用，且发生在
     *     {@code ChunkUtils.shard}切片<b>之前</b>——命中即返回，全部分片的对账、上传与合并都不再执行。
     *     探测哪些信息（内容MD5、大小、文件名）由协议实现自己从{@code file}取用，
     *     框架不假定服务端用什么标识整个文件；
     *     命中时（{@code isSuccess()}为{@code true}）若服务端返回内部文件ID/下载URL，
     *     放进{@code data}透传到{@link ChunkUploadResult#getServerData()}。
     *     未命中或探测失败（返回{@code false}、{@code null}或抛异常）都不会中断本次上传，
     *     内核只记录warn日志，继续走正常分片流程；
     *     与{@code uploadChunk}/{@code mergeChunks}一样没有可兜底的默认协议，
     *     启用秒传开关时必须由子类或子接口覆写，未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}并被降级为未命中。
     * </pre>
     *
     * @param fileId 文件唯一标识，与{@link #uploadChunk}收到的是同一个值
     * @param file   待上传的原始文件，可从中取得内容MD5、大小、文件名等探测参数
     * @return 秒传响应；{@code isSuccess()}为{@code true}表示服务端已有该文件，本次无需上传
     */
    default ChunkUploadResponse instantUpload(String fileId, File file) {
        throw new ChunkUploadNotImplementedException("check whether the server already has this whole file (instant upload)",
                "instantUpload(String, File)",
                "the instant-upload check can never hit and every byte will be uploaded again");
    }

    /**
     * 删除服务端已收到的该文件的全部分片
     * <pre>
     *     可选的失败清理动作：只有开启{@link ChunkUploadOptions#isDeleteOnMergeFailure()}、
     *     且合并请求<b>实际发起</b>仍失败时才会被调用，用于回收合并失败后留在服务端的孤儿分片；
     *     分片没传完而未发起合并时不会调用——那些分片下次续传还要用。
     *     没有可兜底的默认协议，启用删除开关时必须由子类或子接口覆写；未覆写时默认实现抛出
     *     {@link ChunkUploadNotImplementedException}，该异常由内核记录进{@link ChunkUploadResult}
     *     与日志（{@code getDeleteError()}），不会向外抛。
     * </pre>
     *
     * @param fileId 文件唯一标识，与{@link #uploadChunk}收到的是同一个值
     * @return 是否删除成功
     */
    default boolean deleteChunks(String fileId) {
        throw new ChunkUploadNotImplementedException("delete the uploaded chunks on the server", "deleteChunks(String)",
                "the orphan chunks left after a failed merge can never be reclaimed");
    }


    @FunctionAlias("cur_success")
    static ChunkUploadResponse success(Object... serverData) {
        if (ContainerUtils.isEmptyArray(serverData)) {
            return ChunkUploadResponse.success();
        }
        return ChunkUploadResponse.success(serverData[0]);
    }

    @FunctionAlias("cur_failed")
    static ChunkUploadResponse failed() {
        return ChunkUploadResponse.failed();
    }

    @FunctionAlias("byte_file")
    static HttpFile byteFile(String filename, byte[] fileContent){
        return new HttpFile(fileContent, filename);
    }
}
