package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.async.JavaThreadAsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 分片上传内核的行为测试。
 * <pre>
 * 使用内存版的假协议接口{@link FakeUploadApi}替代真实HTTP，分片任务统一提交给
 * {@link AsyncTaskExecutor}（内部包装一个固定大小线程池），可稳定覆盖全部分支：
 * 1.正常路径：切片数、每片内容、合并参数、结果列表的索引升序；
 * 2.断点续传：服务端已有的分片不再上传；判据查询抛异常时降级为全量上传且不中断；
 *   服务端用片号之外的标识（不透明分片ID）时，覆写isChunkUploaded即可扩展对账判据；
 * 3.失败重试：抛异常与返回false两类失败、每轮只重传未成功的分片、重传轮数统计、
 *   达到maxRetryCount后停止、maxRetryCount为负数时无限重传直到成功；
 * 4.失败兜底：重试用尽、合并返回false、合并抛异常都不抛异常，只体现在结果对象里；
 *   合并失败后的分片删除：开关控制、未发起合并不删除、删除失败只记录不抛出、
 *   开启开关但未覆写deleteChunks时默认实现的未实现异常同样只记入结果；
 * 5.秒传：命中时不切片不上传不合并、服务端返回的内部文件ID透传到结果；
 *   未命中与探测失败都降级为正常分片流程；
 * 6.参数校验：非法配置与非法入参在任何请求发出之前就被拦截；
 * 7.执行器由外部注入，内核不会创建也不关闭任何线程池；
 * 8.文件标识：显式给出（方法参数或注解属性）时不发起申请；未给出时先请服务端生成
 *   （覆写initUpload），服务端不生成则用文件内容MD5；申请抛异常则整体失败且零请求；
 * 9.释放资源：无论正常完成、申请标识失败还是秒传命中，收尾动作都恰好执行一次；
 *   收尾自身抛异常只记日志，不改变上传结果。
 * </pre>
 */
public class ChunkUploadExecutorTest {

    /**
     * 26字节按5字节切分，共6片
     */
    private static final long CHUNK_SIZE = 5L;

    private static final String FILE_ID = "file-id-001";

    /**
     * 所有用例共用的分片任务线程池：并发由{@code maxConcurrency}的提交窗口限定，
     * 内核拿到的是{@link AsyncTaskExecutor}包装，因此本池不会被内核关闭
     */
    private static final ExecutorService UPLOAD_POOL = Executors.newFixedThreadPool(4);

    @AfterClass
    public static void shutdownUploadPool() {
        UPLOAD_POOL.shutdown();
    }

    @Test
    public void uploadsEveryChunkAndMergesWhenAllSucceed() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertTrue(result.isAllChunksUploaded());
        assertTrue(result.isMerged());
        assertEquals(6, result.getTotalChunks());
        assertEquals(26L, result.getTotalSize());
        assertEquals(CHUNK_SIZE, result.getChunkSize());
        // 文件级元信息整体复用ChunkUploadMeta：结果里能直接拿到原始文件与切分依据
        assertEquals(file, result.getMeta().getFile());
        assertEquals(FILE_ID, result.getMeta().getFileId());
        assertEquals(0, result.getRetryRounds());
        assertEquals(6, result.getSuccessChunks().size());
        assertTrue(result.getFailedChunks().isEmpty());
        assertTrue(result.getAlreadyUploadedChunks().isEmpty());
        // 合并收到的是完整协议参数
        assertEquals(1, api.mergeCalls);
        assertEquals(FILE_ID, api.mergedFileId);
        assertEquals(file.getName(), api.mergedFileName);
        assertEquals(26L, api.mergedTotalSize);
        assertEquals(CHUNK_SIZE, api.mergedChunkSize);
        assertEquals(6, api.mergedTotalChunks);
        // 分片清单里全部分片与本次上传成功的分片都是6片，且按片号升序
        assertEquals(6, api.mergeContext.getTotalChunks());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), chunkIndicesOf(api.mergeContext.getAllChunks()));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), chunkIndicesOf(api.mergeContext.getUploadedChunks()));
        // 显式给出fileId时不应发起申请请求
        assertEquals(0, api.initCalls);
        assertFalse(result.isInitRequired());
        assertNull(result.getInitError());
        // 合并成功时服务端返回的内部文件ID透传到结果
        assertEquals("srv-" + FILE_ID, result.getServerData());
        // 分片内容按索引拼回后与原文件一致
        assertArrayEqual(ChunkTestSupport.sequence(26), api.reassembled());
    }

    @Test
    public void emptyFileGoesStraightToMergeWithZeroChunks() {
        File file = ChunkTestSupport.emptyFile();
        FakeUploadApi api = new FakeUploadApi();

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(0, result.getTotalChunks());
        assertEquals(0, api.uploadCalls.get());
        assertEquals(1, api.mergeCalls);
        assertEquals(0, api.mergedTotalChunks);
    }

    @Test
    public void chunksAlreadyOnServerAreNotUploadedAgain() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.existingChunks.addAll(Arrays.asList(1L, 3L));

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(Arrays.asList(1L, 3L), result.getAlreadyUploadedChunks());
        assertEquals(4, result.getSuccessChunks().size());
        assertEquals(4, api.uploadCalls.get());
        assertEquals(Arrays.asList(2L, 4L, 5L, 6L), new ArrayList<>(api.receivedChunks.keySet()));
        // 合并清单里全部6片都在，但本次实际上传成功的只有4片：跳过的1、3片不计入上传成功集合
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), chunkIndicesOf(api.mergeContext.getAllChunks()));
        assertEquals(Arrays.asList(2L, 4L, 5L, 6L), chunkIndicesOf(api.mergeContext.getUploadedChunks()));
    }

    @Test
    public void queryFailureDegradesToFullUploadWithoutInterrupting() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.queryThrows = true;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 断点续传是优化手段，判据查询失败不能让整次上传失败
        assertTrue(result.toString(), result.isSuccess());
        assertTrue(result.getAlreadyUploadedChunks().isEmpty());
        assertEquals(6, api.uploadCalls.get());
    }

    @Test
    public void opaqueChunkIdsAreMatchedByOverriddenDecider() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        OpaqueIdUploadApi api = new OpaqueIdUploadApi();
        api.existingChunks.addAll(Arrays.asList(2L, 5L));

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 服务端用不透明分片ID标识分片时，覆写isChunkUploaded即可，内核不关心标识长什么样
        assertTrue(result.toString(), result.isSuccess());
        assertEquals(Arrays.asList(2L, 5L), result.getAlreadyUploadedChunks());
        assertEquals(4, api.uploadCalls.get());
        assertEquals(Arrays.asList(1L, 3L, 4L, 6L), new ArrayList<>(api.receivedChunks.keySet()));
    }

    @Test
    public void skipUploadedCanBeDisabledByOptions() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.existingChunks.addAll(Arrays.asList(1L, 3L));

        ChunkUploadResult result = executor(api, options().setSkipUploaded(false)).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertTrue(result.getAlreadyUploadedChunks().isEmpty());
        assertEquals(6, api.uploadCalls.get());
    }

    @Test
    public void exceptionFailureIsRetriedAndOnlyFailedChunksResent() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        // 第2片首次抛异常，其余全部成功
        api.failTimes.put(2L, 1);

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, result.getRetryRounds());
        assertEquals(6, result.getSuccessChunks().size());
        // 成功列表按索引升序，即使分片来自多轮
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), indicesOf(result.getSuccessChunks()));
        // 首轮止损跳过的分片与失败分片一起进入第二轮，因此总请求数大于分片数
        assertTrue("实际请求数" + api.uploadCalls.get(), api.uploadCalls.get() > 6);
        // 第2片首轮失败、重传轮成功
        assertEquals(2, api.attemptsOf(2L));
        assertArrayEqual(ChunkTestSupport.sequence(26), api.reassembled());
    }

    @Test
    public void businessFalseIsRetriedAndCarriesNoError() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        // 服务端返回false但不抛异常
        api.failWithException = false;
        api.failTimes.put(4L, 1);

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, result.getRetryRounds());
        assertNull(errorOf(result.getSuccessChunks(), 4L));
        assertArrayEqual(ChunkTestSupport.sequence(26), api.reassembled());
    }

    /**
     * 协议实现抛检查异常（如读写本地文件的IOException）时，与运行时异常走同一条失败通道：
     * 被包装成{@link ChunkUploadException}后落进单片结果，不会把整轮上传拖成向外抛的异常
     */
    @Test
    public void checkedExceptionFromProtocolIsRecordedAsChunkFailure() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.failWithCheckedException = true;
        api.alwaysFailChunks.add(2L);

        ChunkUploadResult result = executor(api, options().setMaxRetryCount(0)).upload(file, FILE_ID);

        assertFalse(result.isSuccess());
        assertTrue(indicesOf(result.getFailedChunks()).contains(2L));
        // 检查异常被收敛为ChunkUploadException，原始异常挂在cause上
        Throwable error = errorOf(result.getFailedChunks(), 2L);
        assertNotNull(error);
        assertTrue("实际异常: " + error, error instanceof ChunkUploadException);
        assertTrue("原始异常应挂在cause上: " + error.getCause(), error.getCause() instanceof IOException);
    }

    @Test
    public void retryStopsAtMaxRetryCountAndReportsFailedChunks() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        // 第3、5片始终返回false（不抛异常，避免止损机制掩盖失败集合的构成）
        api.failWithException = false;
        api.alwaysFailChunks.addAll(Arrays.asList(3L, 5L));

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertFalse(result.isSuccess());
        assertFalse(result.isAllChunksUploaded());
        assertFalse(result.isMerged());
        assertNull(result.getMergeError());
        assertEquals(Arrays.asList(3L, 5L), indicesOf(result.getFailedChunks()));
        // 失败集合里保留分片定位信息，可直接用于自行重传
        assertEquals(26L, result.getFailedChunks().get(0).getFileChunk().getTotalSize());
        assertEquals(2L, result.getRetryRounds());
        assertEquals(4, result.getSuccessChunks().size());
        // 首轮 + 两轮重传
        assertEquals(3, api.attemptsOf(3L));
        // 分片未全部成功时绝不发起合并，避免服务端得到残缺文件
        assertEquals(0, api.mergeCalls);
    }

    @Test
    public void unlimitedRetryContinuesUntilEveryChunkSucceeds() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.failTimes.put(6L, 3);

        ChunkUploadResult result = executor(api, options().setMaxRetryCount(-1)).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(3, result.getRetryRounds());
        // 前3次尝试失败，第4次才成功
        assertEquals(4, api.attemptsOf(6L));
        assertEquals(1, api.mergeCalls);
    }

    @Test
    public void mergeIsSkippedWhenMergeAfterSuccessDisabled() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();

        ChunkUploadResult result = executor(api, options().setMergeAfterSuccess(false)).upload(file, FILE_ID);

        // 不需要合并时，分片全部上传成功即为整体成功
        assertTrue(result.toString(), result.isSuccess());
        assertFalse(result.isMerged());
        assertEquals(0, api.mergeCalls);
    }

    @Test
    public void mergeFailureIsReportedInsteadOfThrown() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeThrows = true;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.isAllChunksUploaded());
        assertFalse(result.isMerged());
        assertFalse(result.isSuccess());
        assertNotNull(result.getMergeError());
        assertEquals("merge-failed", result.getMergeError().getMessage());
        // 合并失败也是「上传结束」，收尾动作照常执行
        assertEquals(1, api.releaseCalls);
    }

    @Test
    public void mergeReturningFalseIsNotASuccess() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeResult = false;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertTrue(result.isAllChunksUploaded());
        assertFalse(result.isMerged());
        assertFalse(result.isSuccess());
        assertNull(result.getMergeError());
    }

    @Test
    public void mergeFailureDeletesServerChunksWhenOptionEnabled() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeResult = false;

        ChunkUploadResult result = executor(api, options().setDeleteOnMergeFailure(true)).upload(file, FILE_ID);

        assertFalse(result.isSuccess());
        // 合并实际发起且失败，删除被触发
        assertTrue(result.isDeleteRequired());
        assertTrue(result.isChunksDeleted());
        assertNull(result.getDeleteError());
        assertEquals(1, api.deleteCalls);
        assertEquals(FILE_ID, api.deletedFileId);
    }

    @Test
    public void mergeFailureKeepsChunksWhenOptionDisabled() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeThrows = true;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 默认不开启删除：合并失败也只记录结果，服务端分片原样保留
        assertFalse(result.isSuccess());
        assertFalse(result.isDeleteRequired());
        assertFalse(result.isChunksDeleted());
        assertEquals(0, api.deleteCalls);
    }

    @Test
    public void unmergedChunksAreNotDeletedWhenMergeNeverStarted() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.failWithException = false;
        api.alwaysFailChunks.add(3L);

        ChunkUploadResult result = executor(api, options().setDeleteOnMergeFailure(true)).upload(file, FILE_ID);

        // 分片没传完而未发起合并，不属于「合并失败」，分片要留着下次续传
        assertFalse(result.isSuccess());
        assertFalse(result.isDeleteRequired());
        assertEquals(0, api.deleteCalls);
        // 除始终失败的第3片外，其余5片都保留在服务端
        assertEquals(5, api.receivedChunks.size());
    }

    @Test
    public void deleteFailureIsRecordedInsteadOfThrown() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeResult = false;
        api.deleteThrows = true;

        ChunkUploadResult result = executor(api, options().setDeleteOnMergeFailure(true)).upload(file, FILE_ID);

        // 删除异常不向外抛，只落在结果里；整体成败在合并失败时已经是false
        assertFalse(result.isSuccess());
        assertTrue(result.isDeleteRequired());
        assertFalse(result.isChunksDeleted());
        assertNotNull(result.getDeleteError());
        assertEquals("delete-failed", result.getDeleteError().getMessage());
    }

    @Test
    public void deleteWithoutOverrideIsRecordedAsNotImplementedError() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.mergeResult = false;
        // 保持协议接口的默认deleteChunks实现（与mergeChunks一样抛未实现异常）
        api.deleteNotImplemented = true;

        ChunkUploadResult result = executor(api, options().setDeleteOnMergeFailure(true)).upload(file, FILE_ID);

        // 开启开关但未覆写：默认实现的未实现异常被记录进结果，不会向外抛
        assertFalse(result.isSuccess());
        assertTrue(result.isDeleteRequired());
        assertFalse(result.isChunksDeleted());
        assertNotNull(result.getDeleteError());
        assertTrue(result.getDeleteError() instanceof ChunkUploadNotImplementedException);
    }

    @Test
    public void releaseResourcesRunsAfterEveryOutcome() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));

        // 正常完成：释放拿到与合并一致的元信息，以及本次切出的全部分片
        FakeUploadApi succeeded = new FakeUploadApi();
        assertTrue(executor(succeeded, options()).upload(file, FILE_ID).isSuccess());
        assertEquals(1, succeeded.releaseCalls);
        assertEquals(FILE_ID, succeeded.releaseMeta.getFileId());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), chunkIndicesOf(succeeded.releaseContext.getAllChunks()));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), chunkIndicesOf(succeeded.releaseContext.getUploadedChunks()));

        // 申请标识失败：一片都没切，仍然要释放；此时fileId尚未产生，分片清单为空
        FakeUploadApi initFailed = new FakeUploadApi();
        initFailed.initThrows = true;
        assertFalse(executor(initFailed, options()).upload(file).isSuccess());
        assertEquals(1, initFailed.releaseCalls);
        assertNull(initFailed.releaseMeta.getFileId());
        assertTrue(initFailed.releaseContext.getAllChunks().isEmpty());

        // 秒传命中：不切片，分片清单为空，元信息里的分片总数为0
        FakeUploadApi instant = new FakeUploadApi();
        instant.instantResult = true;
        assertTrue(executor(instant, options().setInstantUpload(true)).upload(file, FILE_ID).isSuccess());
        assertEquals(1, instant.releaseCalls);
        assertEquals(0, instant.releaseMeta.getTotalChunks());
        assertTrue(instant.releaseContext.getAllChunks().isEmpty());
    }

    @Test
    public void releaseFailureDoesNotOverrideTheResult() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.releaseThrows = true;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 收尾动作自身失败只记日志：上传该成功还是成功，也不会向外抛
        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, api.releaseCalls);
    }

    @Test
    public void instantUploadHitSkipsEveryChunkAndMerge() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.instantResult = true;
        api.instantData = "srv-instant-001";

        ChunkUploadResult result = executor(api, options().setInstantUpload(true)).upload(file, FILE_ID);

        // 命中秒传：不切片、不上传、不合并，整体直接成功
        assertTrue(result.toString(), result.isSuccess());
        assertTrue(result.isInstantUploadRequired());
        assertTrue(result.isInstantUploaded());
        assertEquals(0, result.getTotalChunks());
        assertEquals(0, api.uploadCalls.get());
        assertEquals(0, api.mergeCalls);
        assertEquals(1, api.instantCalls);
        assertEquals(FILE_ID, api.instantFileId);
        // 探测拿到的是完整的文件级信息，而不是只有一个fileId
        assertEquals(6, api.instantTotalChunks);
        assertEquals("srv-instant-001", result.getServerData());
    }

    @Test
    public void instantUploadMissFallsBackToNormalChunkUpload() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        // 探测返回未命中，照常走分片流程，结果里不残留秒传痕迹

        ChunkUploadResult result = executor(api, options().setInstantUpload(true)).upload(file, FILE_ID);

        assertTrue(result.toString(), result.isSuccess());
        assertTrue(result.isInstantUploadRequired());
        assertFalse(result.isInstantUploaded());
        assertEquals(1, api.instantCalls);
        assertEquals(6, api.uploadCalls.get());
        assertEquals(1, api.mergeCalls);
        // serverData来自合并而非秒传
        assertEquals("srv-" + FILE_ID, result.getServerData());
    }

    @Test
    public void instantUploadProbeFailureDegradesWithoutInterrupting() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.instantThrows = true;

        ChunkUploadResult result = executor(api, options().setInstantUpload(true)).upload(file, FILE_ID);

        // 秒传是优化手段，探测抛异常不能让整次上传失败
        assertTrue(result.toString(), result.isSuccess());
        assertFalse(result.isInstantUploaded());
        assertEquals(6, api.uploadCalls.get());
    }

    @Test
    public void instantUploadWithoutOverrideDegradesToNormalUpload() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        // 保持协议接口的默认checkInstantUpload实现（抛ChunkUploadNotImplementedException）
        api.instantNotImplemented = true;

        ChunkUploadResult result = executor(api, options().setInstantUpload(true)).upload(file, FILE_ID);

        // 开启开关但未覆写：未实现异常被降级为未命中，不会向外抛
        assertTrue(result.toString(), result.isSuccess());
        assertFalse(result.isInstantUploaded());
        assertEquals(6, api.uploadCalls.get());
    }

    @Test
    public void instantUploadIsNotProbedWhenOptionDisabled() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.instantResult = true;

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 默认关闭秒传：即使服务端有该文件也不探测，照常全量上传
        assertTrue(result.toString(), result.isSuccess());
        assertFalse(result.isInstantUploadRequired());
        assertEquals(0, api.instantCalls);
        assertEquals(6, api.uploadCalls.get());
    }

    @Test
    public void illegalConfigurationAndArgumentsFailBeforeAnyRequest() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        ChunkUploadExecutor uploadExecutor = executor(api, options());

        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(api, options().setChunkSize(0), taskExecutor()));
        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(api, options().setMaxConcurrency(0), taskExecutor()));
        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(api, options().setMaxRetryCount(-2), taskExecutor()));
        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(api, options().setRetryBackoffMillis(-1), taskExecutor()));
        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(null, options(), taskExecutor()));
        // 执行器也必须外部注入，内核没有自建线程池可兼容null
        assertThrows(IllegalArgumentException.class, () -> new ChunkUploadExecutor(api, options(), null));
        assertThrows(IllegalArgumentException.class, () -> uploadExecutor.upload(new File(file.getParentFile(), "absent.bin"), FILE_ID));
        assertThrows(IllegalArgumentException.class, () -> uploadExecutor.upload(null, FILE_ID));
        assertThrows(IllegalArgumentException.class, () -> uploadExecutor.upload(file, FILE_ID, "  "));
        assertEquals(0, api.uploadCalls.get());
        assertEquals(0, api.mergeCalls);
    }

    @Test
    public void uploadWithoutFileNameUsesOriginalFileName() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11));
        FakeUploadApi api = new FakeUploadApi();

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        assertEquals(file.getName(), result.getFileName());
        assertEquals(file.getName(), api.mergedFileName);
        assertEquals(FILE_ID, result.getFileId());
        assertEquals(3, result.getTotalChunks());
    }

    @Test
    public void serverGeneratedFileIdFlowsThroughTheWholeFlow() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.initFileId = "srv-task-0001";

        ChunkUploadResult result = executor(api, options()).upload(file);

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, api.initCalls);
        // 申请发生在切片之前：此时fileId尚未产生，其余文件级参数已就绪
        assertNull(api.initMeta.getFileId());
        assertEquals(file.getName(), api.initMeta.getFileName());
        assertEquals(26L, api.initMeta.getTotalSize());
        assertEquals(CHUNK_SIZE, api.initMeta.getChunkSize());
        assertEquals(6, api.initMeta.getTotalChunks());
        assertTrue(result.isInitRequired());
        assertNull(result.getInitError());
        // 服务端生成的标识要贯穿分片、合并与结果
        assertEquals("srv-task-0001", result.getFileId());
        assertEquals("srv-task-0001", api.chunkFileId);
        assertEquals("srv-task-0001", api.mergedFileId);
        // 分片与合并收到的是同一份元信息，而不是裸的fileId
        assertSame(api.chunkMeta, api.mergeMeta);
        assertEquals("srv-task-0001", api.chunkMeta.getFileId());
        assertEquals(file, api.chunkMeta.getFile());
        assertEquals("srv-srv-task-0001", result.getServerData());
        assertArrayEqual(ChunkTestSupport.sequence(26), api.reassembled());
    }

    @Test
    public void initReturningNullFallsBackToContentMd5() throws Exception {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();

        ChunkUploadResult result = executor(api, options()).upload(file);

        // 默认实现不生成标识：内核退回内容MD5，不是空标识也不是跳过上传
        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, api.initCalls);
        assertEquals(DigestFunctions.md5Hex(file), result.getFileId());
        assertEquals(result.getFileId(), api.mergedFileId);
    }

    @Test
    public void blankFileIdIsTreatedAsNotGiven() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.initFileId = "srv-task-0002";

        ChunkUploadResult result = executor(api, options()).upload(file, "  ");

        assertTrue(result.toString(), result.isSuccess());
        assertEquals(1, api.initCalls);
        assertEquals("srv-task-0002", result.getFileId());
    }

    @Test
    public void explicitFileIdSkipsTheInitRequest() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.initFileId = "srv-task-0003";

        ChunkUploadResult result = executor(api, options()).upload(file, FILE_ID);

        // 标识已由调用方确定：不申请、也不会被服务端生成的值覆盖
        assertTrue(result.toString(), result.isSuccess());
        assertEquals(0, api.initCalls);
        assertFalse(result.isInitRequired());
        assertEquals(FILE_ID, result.getFileId());
        assertEquals(FILE_ID, api.mergedFileId);
    }


    @Test
    public void initFailureFailsTheWholeUploadWithoutAnyRequest() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        FakeUploadApi api = new FakeUploadApi();
        api.initThrows = true;

        ChunkUploadResult result = executor(api, options().setInstantUpload(true)).upload(file);

        // 申请属于必要准备：失败就不探测、不切片、不合并，也不向外抛异常
        assertEquals(1, api.initCalls);
        assertFalse(result.isSuccess());
        assertTrue(result.isInitRequired());
        assertNotNull(result.getInitError());
        assertEquals("init-failed", result.getInitError().getMessage());
        assertNull(result.getFileId());
        assertTrue(result.getFailedChunks().isEmpty());
        assertEquals(6, result.getTotalChunks());
        assertEquals(0, api.instantCalls);
        assertEquals(0, api.uploadCalls.get());
        assertEquals(0, api.mergeCalls);
    }

    /**
     * 构造一个便于测试的配置：并发2、失败不等待
     *
     * @return 上传配置
     */
    private static ChunkUploadOptions options() {
        return new ChunkUploadOptions()
                .setChunkSize(CHUNK_SIZE)
                .setMaxConcurrency(2)
                .setMaxRetryCount(2)
                .setRetryBackoffMillis(0L);
    }

    /**
     * 用共用的线程池包装出异步任务执行器，验证“执行器由外部注入、内核不接管生命周期”
     *
     * @return 异步任务执行器
     */
    private static AsyncTaskExecutor taskExecutor() {
        return JavaThreadAsyncTaskExecutor.createByExecutor(UPLOAD_POOL);
    }

    /**
     * 创建被测的上传内核
     *
     * @param api     假协议接口
     * @param options 上传配置
     * @return 上传内核
     */
    private static ChunkUploadExecutor executor(ChunkUploadApi api, ChunkUploadOptions options) {
        return new ChunkUploadExecutor(api, options, taskExecutor());
    }

    private static List<Long> indicesOf(List<ChunkResult<Boolean>> results) {
        List<Long> indices = new ArrayList<>();
        for (ChunkResult<Boolean> result : results) {
            indices.add(result.getFileChunk().getIndex());
        }
        return indices;
    }

    /**
     * 取出分片清单里每片的片号，用于比对顺序与内容
     *
     * @param chunks 分片集合
     * @return 片号列表，顺序与入参一致
     */
    private static List<Long> chunkIndicesOf(List<FileChunk> chunks) {
        List<Long> indices = new ArrayList<>(chunks.size());
        for (FileChunk chunk : chunks) {
            indices.add(chunk.getIndex());
        }
        return indices;
    }

    private static Throwable errorOf(List<ChunkResult<Boolean>> results, long index) {
        for (ChunkResult<Boolean> result : results) {
            if (result.getFileChunk().getIndex() == index) {
                return result.getError();
            }
        }
        return null;
    }

    private static void assertArrayEqual(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        assertTrue(Arrays.equals(expected, actual));
    }

    /**
     * 内存版分片上传协议实现，可指定每个分片的失败方式与失败次数
     */
    private static class FakeUploadApi implements ChunkUploadApi {

        /**
         * 服务端已收到的分片内容，key为分片索引
         */
        final Map<Long, byte[]> receivedChunks = new ConcurrentHashMap<>();

        /**
         * 上传前服务端已存在的分片索引，用于断点续传用例
         */
        final List<Long> existingChunks = new ArrayList<>();

        /**
         * 每个分片前N次尝试必须失败
         */
        final Map<Long, Integer> failTimes = new HashMap<>();

        /**
         * 每个分片始终失败
         */
        final Set<Long> alwaysFailChunks = new HashSet<>();

        /**
         * 每个分片已尝试的次数
         */
        final Map<Long, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();

        /**
         * 上传请求总数
         */
        final AtomicInteger uploadCalls = new AtomicInteger();

        /**
         * true表示失败时抛出异常，false表示返回false
         */
        boolean failWithException = true;

        /**
         * true表示失败时抛出IOException这类检查异常，用于验证协议方法声明throws Exception后的收敛路径
         */
        boolean failWithCheckedException;

        /**
         * 查询分片是否已上传时是否抛异常
         */
        boolean queryThrows;

        /**
         * 合并时是否抛异常
         */
        boolean mergeThrows;

        /**
         * 合并接口正常返回时的结果值
         */
        boolean mergeResult = true;

        /**
         * 删除接口是否抛异常
         */
        boolean deleteThrows;

        /**
         * true表示不覆写协议接口的默认deleteChunks实现，用于验证未实现异常的记录路径
         */
        boolean deleteNotImplemented;

        /**
         * 秒传探测的返回结果，默认未命中
         */
        boolean instantResult;

        /**
         * 秒传命中时服务端返回的模拟内部文件ID
         */
        Object instantData = "srv-instant";

        /**
         * 秒传探测是否抛异常
         */
        boolean instantThrows;

        /**
         * true表示不覆写协议接口的默认checkInstantUpload实现
         */
        boolean instantNotImplemented;

        int instantCalls;

        String instantFileId;

        int deleteCalls;

        String deletedFileId;

        int mergeCalls;

        String mergedFileId;

        String mergedFileName;

        int mergedTotalChunks = -1;

        /**
         * 合并时收到的文件总大小
         */
        long mergedTotalSize = -1L;

        /**
         * 合并时收到的单片大小
         */
        long mergedChunkSize = -1L;

        /**
         * 最近一次分片上传收到的文件标识
         */
        String chunkFileId;

        /**
         * 最近一次分片上传收到的文件级元信息，用于校验与合并/删除收到的是同一个对象
         */
        ChunkUploadMeta chunkMeta;

        /**
         * 最近一次合并收到的文件级元信息
         */
        ChunkUploadMeta mergeMeta;

        /**
         * 最近一次合并收到的分片清单，用于校验全部分片与本次上传成功的分片
         */
        ChunkMergeContext mergeContext;

        /**
         * 秒传探测时收到的分片总数
         */
        int instantTotalChunks = -1;

        /**
         * 申请文件标识时收到的元信息，用于校验上报参数
         */
        ChunkUploadMeta initMeta;

        int initCalls;

        /**
         * 服务端返回的文件标识，null表示服务端不生成
         */
        String initFileId;

        /**
         * 申请文件标识时是否抛异常
         */
        boolean initThrows;

        /**
         * 释放资源动作的调用次数
         */
        int releaseCalls;

        /**
         * 释放资源时收到的文件级元信息
         */
        ChunkUploadMeta releaseMeta;

        /**
         * 释放资源时收到的分片清单
         */
        ChunkMergeContext releaseContext;

        /**
         * 释放资源时是否抛异常
         */
        boolean releaseThrows;

        @Override
        public String initUpload(ChunkUploadMeta meta) {
            initCalls++;
            initMeta = meta;
            if (initThrows) {
                throw new IllegalStateException("init-failed");
            }
            return initFileId;
        }

        @Override
        public boolean isChunkUploaded(ChunkUploadMeta meta, FileChunk fileChunk) {
            if (queryThrows) {
                throw new IllegalStateException("query-failed");
            }
            return existingChunks.contains(fileChunk.getIndex());
        }

        @Override
        public boolean uploadChunk(ChunkUploadMeta meta, FileChunk fileChunk) throws Exception {
            long index = fileChunk.getIndex();
            chunkMeta = meta;
            chunkFileId = meta.getFileId();
            uploadCalls.incrementAndGet();
            int attempt = attemptCounters.computeIfAbsent(index, key -> new AtomicInteger()).incrementAndGet();
            Integer limitedFailTimes = failTimes.get(index);
            boolean shouldFail = alwaysFailChunks.contains(index) || (limitedFailTimes != null && attempt <= limitedFailTimes);
            if (shouldFail) {
                if (failWithCheckedException) {
                    throw new IOException("chunk-" + index + "-checked-failed-on-attempt-" + attempt);
                }
                if (failWithException) {
                    throw new IllegalStateException("chunk-" + index + "-failed-on-attempt-" + attempt);
                }
                return false;
            }
            receivedChunks.put(index, fileChunk.getContent());
            return true;
        }

        @Override
        public ChunkUploadResponse mergeChunks(ChunkUploadMeta meta, ChunkMergeContext context) {
            mergeCalls++;
            mergeMeta = meta;
            mergeContext = context;
            mergedFileId = meta.getFileId();
            mergedFileName = meta.getFileName();
            mergedTotalSize = meta.getTotalSize();
            mergedChunkSize = meta.getChunkSize();
            mergedTotalChunks = meta.getTotalChunks();
            if (mergeThrows) {
                throw new IllegalStateException("merge-failed");
            }
            // 模拟会在合并响应里携带内部文件ID的服务端
            return mergeResult ? ChunkUploadResponse.success("srv-" + meta.getFileId()) : ChunkUploadResponse.failed();
        }

        @Override
        public ChunkUploadResponse checkInstantUpload(ChunkUploadMeta meta) throws Exception {
            instantCalls++;
            instantFileId = meta.getFileId();
            instantTotalChunks = meta.getTotalChunks();
            if (instantNotImplemented) {
                // 回到协议接口的默认实现：抛ChunkUploadNotImplementedException
                return ChunkUploadApi.super.checkInstantUpload(meta);
            }
            if (instantThrows) {
                throw new IllegalStateException("instant-probe-failed");
            }
            return instantResult ? ChunkUploadResponse.success(instantData) : ChunkUploadResponse.failed();
        }

        @Override
        public boolean deleteChunks(ChunkUploadMeta meta) throws Exception {
            deleteCalls++;
            deletedFileId = meta.getFileId();
            if (deleteNotImplemented) {
                // 回到协议接口的默认实现：与mergeChunks一样抛ChunkUploadNotImplementedException
                return ChunkUploadApi.super.deleteChunks(meta);
            }
            if (deleteThrows) {
                throw new IllegalStateException("delete-failed");
            }
            // 删除成功后服务端不再持有该文件的分片
            receivedChunks.clear();
            return true;
        }

        @Override
        public void releaseResources(ChunkUploadMeta meta, ChunkMergeContext context) throws Exception {
            releaseCalls++;
            releaseMeta = meta;
            releaseContext = context;
            if (releaseThrows) {
                throw new IllegalStateException("release-failed");
            }
        }

        /**
         * 指定分片的实际尝试次数
         */
        int attemptsOf(long index) {
            AtomicInteger counter = attemptCounters.get(index);
            return counter == null ? 0 : counter.get();
        }

        /**
         * 按分片索引顺序拼回收到的内容
         */
        byte[] reassembled() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (long index = 1; index <= receivedChunks.size(); index++) {
                byte[] content = receivedChunks.get(index);
                if (content == null) {
                    throw new IllegalStateException("chunk-" + index + " is missing");
                }
                out.write(content, 0, content.length);
            }
            return out.toByteArray();
        }
    }

    /**
     * 服务端用不透明分片ID（{@code chunk#片号#摘要}）标识分片的假协议，
     * 验证{@link ChunkUploadApi#isChunkUploaded}扩展点
     */
    private static class OpaqueIdUploadApi extends FakeUploadApi {

        @Override
        public boolean isChunkUploaded(ChunkUploadMeta meta, FileChunk fileChunk) {
            // 按本协议拼出分片ID后与已有分片对账，内核不关心ID内部长什么样
            String expected = "chunk#" + fileChunk.getIndex() + "#a1b2c3d4";
            for (Long index : existingChunks) {
                if (expected.equals("chunk#" + index + "#a1b2c3d4")) {
                    return true;
                }
            }
            return false;
        }
    }
}
