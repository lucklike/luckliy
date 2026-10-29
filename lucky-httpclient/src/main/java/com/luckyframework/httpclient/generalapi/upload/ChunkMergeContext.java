package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 分片上传的分片清单，即{@link ChunkUploadApi#mergeChunks}的第二个入参。
 * <pre>
 *     本类回答「这一次上传切出了哪些分片、其中哪些已经确认到达服务端」，由
 *     {@link ChunkUploadExecutor}在通知合并之前创建，是不可变值对象：
 *     1.{@link #getAllChunks()}：本次切出的全部分片，按片号升序；
 *     2.{@link #getUploadedChunks()}：本次实际上传成功的分片，同样按片号升序；
 *       断点续传中被跳过的分片（服务端此前已存在）本次并没有发出上传请求，因此不计入这一份，
 *       但它们仍出现在{@link #getAllChunks()}里——两份合起来才是服务端当前持有的全部分片。
 *
 *     为什么合并动作需要分片清单：不少服务端的合并接口要求客户端把「这次传了哪些片」一并报上去
 *     （片号列表、逐片内容MD5、每片大小等），服务端对账通过后才允许合并，而这些信息只有内核知道，
 *     于是与{@link ChunkUploadMeta}一起作为合并动作的入参。例如上报片号列表：
 *     {@code
 *     @Override
 *     default ChunkUploadResponse mergeChunks(ChunkUploadMeta meta, ChunkMergeContext context) {
 *         List<Long> chunkIndexes = new ArrayList<>();
 *         for (FileChunk chunk : context.getAllChunks()) {
 *             chunkIndexes.add(chunk.getIndex());
 *         }
 *         Map<String, Object> mergeForm = new LinkedHashMap<>();
 *         mergeForm.put("fileId", meta.getFileId());
 *         mergeForm.put("chunkIndexes", chunkIndexes);
 *         return mergeRequest(mergeForm);
 *     }
 *
 *     @Post("merge")
 *     @RespConvert("#{chunk_success($body$.data)}")
 *     ChunkUploadResponse mergeRequest(@JsonBody Map<String, Object> mergeForm);
 *     }
 *
 *     要逐片MD5时把循环体换成{@code chunk.md5()}即可，注意它声明了{@code throws Exception}，
 *     在本方法里可以直接向外抛（协议方法本身允许抛检查异常）。
 *
 *     取数约定：需要片内容用{@code getContent()}/{@code writeContentTo(out)}，需要内容摘要用
 *     {@code md5()}/{@code sha256()}等方法，需要区间用{@code getStart()/getEnd()/getSize()}。
 *     这些都按片读取文件且不做缓存，同一片既要MD5又要SHA256时请自行复用结果，不必重复读两次。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-29 15:10
 * @see ChunkUploadApi#mergeChunks(ChunkUploadMeta, ChunkMergeContext)
 * @see ChunkUploadMeta
 * @see FileChunk
 */
public class ChunkMergeContext {

    /**
     * 本次切出的全部分片，按片号升序
     */
    private final List<FileChunk> allChunks;

    /**
     * 本次实际上传成功的分片，按片号升序；断点续传中被跳过的分片不在此列
     */
    private final List<FileChunk> uploadedChunks;

    /**
     * 构造分片清单，实例由{@link #of(List, List)}与{@link #ofAllChunks(List)}创建
     *
     * @param allChunks      全部分片
     * @param uploadedChunks 本次上传成功的分片
     */
    private ChunkMergeContext(List<FileChunk> allChunks, List<FileChunk> uploadedChunks) {
        this.allChunks = sortedImmutableCopy(allChunks);
        this.uploadedChunks = sortedImmutableCopy(uploadedChunks);
    }

    /**
     * 创建分片清单
     * <pre>
     *     两个列表都会被拷贝成不可变列表并按片号升序排列，因此传入的列表后续被修改
     *     不会影响已创建的实例；传{@code null}视为空列表。
     * </pre>
     *
     * @param allChunks      本次切出的全部分片
     * @param uploadedChunks 本次实际上传成功的分片
     * @return 分片清单
     */
    public static ChunkMergeContext of(List<FileChunk> allChunks, List<FileChunk> uploadedChunks) {
        return new ChunkMergeContext(allChunks, uploadedChunks);
    }

    /**
     * 创建「全部分片都已上传成功」的分片清单
     * <pre>
     *     用于拿不到内核清单的场合，例如关闭了{@code mergeAfterSuccess}、
     *     事后由业务自己调用{@link ChunkUploadApi#mergeChunks}通知合并时：
     *     此时两份清单内容相同，没必要把同一个列表传两遍。
     * </pre>
     *
     * @param allChunks 本次切出的全部分片
     * @return 分片清单，{@link #getUploadedChunks()}与{@link #getAllChunks()}内容相同
     */
    public static ChunkMergeContext ofAllChunks(List<FileChunk> allChunks) {
        return new ChunkMergeContext(allChunks, allChunks);
    }

    /**
     * 将入参列表拷贝、按片号升序排序后包装为不可变列表
     *
     * @param chunks 待处理的分片列表，可为null
     * @return 按片号升序的不可变列表
     */
    private static List<FileChunk> sortedImmutableCopy(List<FileChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileChunk> copy = new ArrayList<>(chunks);
        copy.sort(Comparator.comparingLong(FileChunk::getIndex));
        return Collections.unmodifiableList(copy);
    }

    /**
     * 获取本次切出的全部分片
     * <pre>
     *     与服务端当前持有的分片范围一致（含断点续传中被跳过的分片），
     *     需要「全部片的片号/摘要」时从本方法取。
     * </pre>
     *
     * @return 全部分片，按片号升序，不可修改
     */
    public List<FileChunk> getAllChunks() {
        return allChunks;
    }

    /**
     * 获取本次实际上传成功的分片
     * <pre>
     *     不包含断点续传中被跳过的分片：它们本次没有发出上传请求，但服务端本来就持有，
     *     需要「本次真正传了哪些片」时从本方法取，需要「服务端一共有哪些片」时用
     *     {@link #getAllChunks()}。
     * </pre>
     *
     * @return 本次上传成功的分片，按片号升序，不可修改
     */
    public List<FileChunk> getUploadedChunks() {
        return uploadedChunks;
    }

    /**
     * 获取本次切出的分片总数
     *
     * @return 分片总数，空文件为0，等价于{@code getAllChunks().size()}
     */
    public int getTotalChunks() {
        return allChunks.size();
    }

    /**
     * 以{@code ChunkMergeContext{totalChunks=, uploadedChunks=}}形式输出分片清单概况，便于日志与问题定位
     *
     * @return 分片清单概况
     */
    @Override
    public String toString() {
        return "ChunkMergeContext{" +
                "totalChunks=" + allChunks.size() +
                ", uploadedChunks=" + uploadedChunks.size() +
                '}';
    }
}
