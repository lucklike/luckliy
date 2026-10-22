package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.async.JavaThreadAsyncTaskExecutor;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
 * 5.参数校验：非法配置与非法入参在任何请求发出之前就被拦截；
 * 6.执行器由外部注入，内核不会创建也不关闭任何线程池。
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
        assertEquals(0, result.getRetryRounds());
        assertEquals(6, result.getSuccessChunks().size());
        assertTrue(result.getFailedChunks().isEmpty());
        assertTrue(result.getAlreadyUploadedChunks().isEmpty());
        // 合并收到的是完整协议参数
        assertEquals(1, api.mergeCalls);
        assertEquals(FILE_ID, api.mergedFileId);
        assertEquals(file.getName(), api.mergedFileName);
        assertEquals(6, api.mergedTotalChunks);
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
        assertThrows(IllegalArgumentException.class, () -> uploadExecutor.upload(file, ""));
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

        int deleteCalls;

        String deletedFileId;

        int mergeCalls;

        String mergedFileId;

        String mergedFileName;

        int mergedTotalChunks = -1;

        @Override
        public boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
            if (queryThrows) {
                throw new IllegalStateException("query-failed");
            }
            return existingChunks.contains(fileChunk.getIndex());
        }

        @Override
        public boolean uploadChunk(String fileId, FileChunk fileChunk) {
            long index = fileChunk.getIndex();
            uploadCalls.incrementAndGet();
            int attempt = attemptCounters.computeIfAbsent(index, key -> new AtomicInteger()).incrementAndGet();
            Integer limitedFailTimes = failTimes.get(index);
            boolean shouldFail = alwaysFailChunks.contains(index) || (limitedFailTimes != null && attempt <= limitedFailTimes);
            if (shouldFail) {
                if (failWithException) {
                    throw new IllegalStateException("chunk-" + index + "-failed-on-attempt-" + attempt);
                }
                return false;
            }
            receivedChunks.put(index, fileChunk.getContent());
            return true;
        }

        @Override
        public boolean mergeChunks(String fileId, File file, int totalChunks) {
            mergeCalls++;
            mergedFileId = fileId;
            mergedFileName = file.getName();
            mergedTotalChunks = totalChunks;
            if (mergeThrows) {
                throw new IllegalStateException("merge-failed");
            }
            return mergeResult;
        }

        @Override
        public boolean deleteChunks(String fileId) {
            deleteCalls++;
            deletedFileId = fileId;
            if (deleteNotImplemented) {
                // 回到协议接口的默认实现：与mergeChunks一样抛ChunkUploadNotImplementedException
                return ChunkUploadApi.super.deleteChunks(fileId);
            }
            if (deleteThrows) {
                throw new IllegalStateException("delete-failed");
            }
            // 删除成功后服务端不再持有该文件的分片
            receivedChunks.clear();
            return true;
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
        public boolean isChunkUploaded(String fileId, FileChunk fileChunk) {
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
