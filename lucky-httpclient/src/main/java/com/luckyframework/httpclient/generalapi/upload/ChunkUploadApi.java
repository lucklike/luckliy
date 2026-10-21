package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;

import java.util.Collection;
import java.util.Collections;

/**
 * 分片上传的服务端协议接口。
 * <pre>
 *     HTTP分片上传没有像下载（{@code Range}请求头）那样的标准协议，不同服务端的
 *     「查询已上传分片、上传单个分片、通知合并」三个接口的URL、参数名、响应格式都不一样，
 *     因此本接口只约定<b>动作</b>，具体协议由使用者用自己的API接口去实现。
 *     类型参数{@code T}即「查询已上传分片」接口返回的<b>报文类型</b>，由服务端协议决定：
 *     片号集合用{@code List<Long>}，分片ID列表用{@code List<String>}，
 *     片号与摘要的映射可用{@code Map<Long, String>}，复合结构就声明自己的DTO——
 *     框架全程以{@code T}传递报文，不做任何「一定是Long片号」的假设。
 *
 *     实现时只需把下面三个方法接到真实接口上，例如：
 *     {@code
 *     @ServerAddress("http://localhost:8864/shard/upload/")
 *     public interface FileChunkUploadApi extends ChunkUploadApi<List<Long>> {
 *
 *         // 1.查询已上传分片：返回类型就是类型参数T；
 *         //   服务端返回{"code":200,"data":[1,3]}，取data字段即为片号集合
 *         @Override
 *         @Get("uploadChunks")
 *         @RespConvert("#{$body$.data}")
 *         List<Long> uploadedChunks(@Query("fileId") String fileId);
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
 *     实现时的四条约定：
 *     1.三个方法内部可以正常使用本框架的注解（{@code @Post}/{@code @MultiFile}/{@code @Retryable}等），
 *       它们会各自发出真实的HTTP请求；
 *     2.返回值用{@code boolean}表达业务成功与否即可，抛异常会被框架记录为分片执行失败并纳入重试，
 *       返回{@code false}同样会被视为需要重试的分片；
 *     3.声明为{@code boolean}的方法要求服务端返回能被自动转成布尔值的响应体，
 *       {@code text/plain}会被框架原样当字符串返回而无法转成{@code boolean}，
 *       这类接口请返回{@code application/json}（如响应体直接是{@code true}），
 *       或者用{@code @RespConvert}从JSON包装结果里取出布尔值；
 *     4.框架不假定服务端用片号标识分片：报文（类型{@code T}）怎么与本地分片对账，
 *       由{@code isChunkUploaded}决定，片号之外的标识（分片ID、内容MD5等）覆写该方法即可，
 *       覆写时参数已经是强类型，无需任何向下转型。
 * </pre>
 *
 * @param <T> 「查询已上传分片」接口返回的报文类型，由服务端协议决定
 * @author fukang
 * @version 2.0.0
 * @date 2026-10-21 10:45
 * @see ChunkUploadExecutor
 * @see ChunkUpload
 */
public interface ChunkUploadApi<T> {

    /**
     * 查询服务端已经收到的分片报文
     * <pre>
     *     用于断点续传：报文如何与本地分片对账由{@link #isChunkUploaded}决定，
     *     命中的分片会在上传前被跳过，不再重复传输。
     *     默认返回{@code null}，即服务端没有提供该查询接口时不做跳过、全量上传；
     *     服务端确实「没有任何已有分片」时返回{@code null}或空集合都可以。
     * </pre>
     *
     * @param fileId 文件唯一标识
     * @return 服务端查询结果的原始报文，类型与结构由协议自定（{@code T}），
     *         {@code null}视为「服务端没有任何已有分片」
     */
    default T uploadedChunks(String fileId) {
        return null;
    }

    /**
     * 判断某个分片服务端是否已存在，即「分片用什么标识」的扩展点
     * <pre>
     *     服务端不一定用片号标识分片，因此本方法只接收{@link #uploadedChunks(String)}返回的
     *     报文（类型{@code T}），由实现方决定怎么比对。
     *     默认实现面向最常见的「片号集合」协议：{@code Number}元素按{@code longValue}与分片索引比对，
     *     其余元素按去空格后的字符串与片号字符串比对，因此{@code [1,3]}、{@code ["1","3"]}均可识别。
     *     服务端用分片ID、内容MD5等片号之外的标识时，覆写本方法并按协议从{@code fileChunk}
     *     （索引/区间/大小/内容）取值比对即可，此时参数已经是强类型，无需向下转型；
     *     默认实现每片都会遍历一次报文，片数多且报文量大时建议在覆写实现里先转成Set再判断。
     * </pre>
     *
     * @param uploadedChunks {@link #uploadedChunks(String)}返回的报文
     * @param fileChunk      待判断的分片
     * @return true表示服务端已有该分片，本次可以跳过
     */
    default boolean isChunkUploaded(T uploadedChunks, FileChunk fileChunk) {
        T payload = uploadedChunks;
        if (payload == null) {
            return false;
        }
        Collection<?> payloads = payload instanceof Collection
                ? (Collection<?>) payload
                : Collections.singletonList(payload);
        long index = fileChunk.getIndex();
        String indexText = String.valueOf(index);
        for (Object item : payloads) {
            if (item instanceof Number) {
                if (((Number) item).longValue() == index) {
                    return true;
                }
            } else if (item != null && indexText.equals(item.toString().trim())) {
                return true;
            }
        }
        return false;
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
