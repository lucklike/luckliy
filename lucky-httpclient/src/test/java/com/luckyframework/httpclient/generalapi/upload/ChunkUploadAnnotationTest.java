package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.httpclient.generalapi.chunk.ChunkResult;
import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadMultipartTestApi;
import com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadServerFileIdTestApi;
import com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestApi;
import com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.logging.BeautifulLoggerPrintHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.CHUNK_MULTIPART_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.CHUNK_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.CREATE_TASK_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.DELETE_CHUNKS_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.INSTANT_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.MERGE_PATH;
import static com.luckyframework.httpclient.generalapi.upload.support.ChunkUploadTestServer.UPLOAD_CHUNKS_PATH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link ChunkUpload}注解的端到端集成测试。
 * <pre>
 *     与{@link ChunkUploadExecutorTest}只验证编排内核不同，本测试用真实的HTTP代理对象
 *     （{@link HttpClientProxyObjectFactory}生成的{@link ChunkUploadTestApi}）与本地假服务端
 *     （{@link ChunkUploadTestServer}）跑完整链路，验证点：
 *       1.注解能正确解析出文件、fileId、文件名，并把协议方法落到真实请求上；
 *       2.分片全部上传后服务端合并出的内容与原文件逐字节一致；
 *       3.业务返回false、HTTP 500异常两种失败都能被重试并最终传成功；
 *       4.始终失败时分片不会漏传、也不会发起合并请求，且不会抛异常；
 *       5.断点续传时服务端已有的分片不再重复上传；
 *       6.方法返回值支持{@link ChunkUploadResult}与{@code boolean}两种声明；
 *       7.数值属性（chunkSize/maxConcurrency/maxRetryCount/retryBackoffMillis）支持SpEL表达式；
 *       8.开启deleteOnMergeFailure后，合并失败会触发删除服务端分片，未开启时不删除；
 *       9.开启instantUpload后，命中秒传不再上传任何分片，
 *         合并/秒传响应里的服务端内部文件ID透传到{@link ChunkUploadResult#getServerData()}；
 *       10.注解不配置fileId时先调initUpload由服务端生成标识，该标识贯穿分片与合并；
 *         配置了fileId时直接使用配置值，不再发起申请请求。
 *
 *     测试文件26字节、分片大小5字节，固定切出6个分片（前5片各5字节，最后1片1字节）。
 * </pre>
 */
public class ChunkUploadAnnotationTest {

    private static final int FILE_LENGTH = 26;
    private static final int TOTAL_CHUNKS = 6;

    private ChunkUploadTestServer server;
    private ChunkUploadTestApi api;
    private ChunkUploadMultipartTestApi multipartApi;
    private ChunkUploadServerFileIdTestApi serverFileIdApi;
    private byte[] content;
    private File file;

    @Before
    public void setUp() throws Exception {
        server = new ChunkUploadTestServer();
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.setLoggerHandler(new BeautifulLoggerPrintHandler());
        api = factory.getProxyObject(ChunkUploadTestApi.class);
        multipartApi = factory.getProxyObject(ChunkUploadMultipartTestApi.class);
        serverFileIdApi = factory.getProxyObject(ChunkUploadServerFileIdTestApi.class);
        content = ChunkTestSupport.sequence(FILE_LENGTH);
        file = ChunkTestSupport.fileWithContent(content);
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * 正常链路：6个分片各上传一次，合并一次，服务端拼出的内容与原文件完全一致
     */
    @Test
    public void allChunksAreUploadedAndServerMergesOriginalContent() {
        ChunkUploadResult result = api.upload(file);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertEquals(TOTAL_CHUNKS, result.getTotalChunks());
        assertEquals(FILE_LENGTH, result.getTotalSize());
        assertEquals(ChunkUploadTestApi.TEST_CHUNK_SIZE, result.getChunkSize());
        assertEquals(file.getName(), result.getFileName());
        assertEquals("未出现失败时不需要重传", 0, result.getRetryRounds());
        assertEquals(TOTAL_CHUNKS, result.getSuccessChunks().size());
        assertTrue(result.getFailedChunks().isEmpty());
        assertTrue("服务端没有已有分片时不应记录跳过", result.getAlreadyUploadedChunks().isEmpty());
        // 假服务端合并成功后返回内部文件ID，应透传到结果
        assertEquals("srv-" + result.getFileId(), result.getServerData());

        assertEquals("每个分片应当只发一次请求", TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertEquals("上传前每个分片查询一次是否已上传", TOTAL_CHUNKS, server.hit(UPLOAD_CHUNKS_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
        assertEquals(file.getName(), server.getMergedFileName());
        assertEquals(TOTAL_CHUNKS, server.getMergedTotalChunks());
        assertArrayEquals(content, server.mergedContent(result.getFileId()));
    }

    /**
     *  fileId留空时用文件内容MD5作为标识，同一份文件重复上传应当能从断点继续
     */
    @Test
    public void emptyFileIdAttributeFallsBackToMd5OfContent() {
        ChunkUploadResult first = api.upload(file);
        assertTrue(first.isSuccess());
        String fileId = first.getFileId();
        assertNotNull(fileId);
        assertEquals("MD5是32位十六进制串", 32, fileId.length());

        // 第二次上传时服务端已有全部分片，理应一片都不再传
        ChunkUploadResult second = api.upload(file);
        assertTrue(second.isSuccess());
        assertEquals(fileId, second.getFileId());
        assertEquals("已存在的分片应当被记录为跳过", Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L),
                second.getAlreadyUploadedChunks());
        assertTrue(second.getSuccessChunks().isEmpty());
        assertEquals("分片请求总数不应增加", TOTAL_CHUNKS, server.hit(CHUNK_PATH));
    }

    /**
     * 服务端生成文件标识：注解不配置fileId时先创建上传任务，任务ID贯穿分片与合并
     */
    @Test
    public void serverGeneratedFileIdIsUsedForChunksAndMerge() {
        ChunkUploadResult result = serverFileIdApi.uploadWithServerFileId(file);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertEquals("未配置fileId时应发起一次任务创建", 1, server.hit(CREATE_TASK_PATH));
        // 申请时上报的是文件级参数，此时服务端还没返回标识
        assertEquals(file.getName(), server.getTaskFileName());
        assertEquals((long) FILE_LENGTH, server.getTaskTotalSize());
        assertEquals(TOTAL_CHUNKS, server.getTaskTotalChunks());
        assertTrue(result.isInitRequired());
        assertNull(result.getInitError());

        String fileId = result.getFileId();
        assertNotNull("服务端生成的标识应记录到结果中", fileId);
        assertTrue(fileId, fileId.startsWith("srv-task-"));
        // 任务ID要贯穿分片与合并，服务端按它得到了完整内容
        assertEquals(TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
        assertArrayEquals(content, server.mergedContent(fileId));
    }

    /**
     * 注解配置了fileId：直接使用配置值，不再为同一个文件向服务端申请任务
     */
    @Test
    public void configuredFileIdIsUsedWithoutInitRequest() {
        ChunkUploadResult result = serverFileIdApi.uploadWithConfiguredFileId(file);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertEquals("配置了fileId时不应再申请任务", 0, server.hit(CREATE_TASK_PATH));
        assertFalse(result.isInitRequired());
        assertEquals("client-file-id", result.getFileId());
        assertEquals(TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertArrayEquals(content, server.mergedContent("client-file-id"));
    }

    /**
     * 分片接口返回false（业务失败）时只重传失败的那一片，最终内容依然完整
     */
    @Test
    public void chunkReturningFalseIsRetriedUntilSuccess() {
        server.failChunk(2, 1);

        ChunkUploadResult result = api.upload(file);

        assertTrue(result.isSuccess());
        assertEquals("失败分片重传一轮", 1, result.getRetryRounds());
        assertEquals("第2片失败一次后重传", 2, server.attemptsOf(2));
        assertEquals("其余分片不应重复上传", 1, server.attemptsOf(1));
        assertEquals(TOTAL_CHUNKS + 1, server.hit(CHUNK_PATH));
        assertArrayEquals(content, server.mergedContent(result.getFileId()));
    }

    /**
     * 分片接口返回HTTP 500（执行异常）时同样纳入重试，且异常不会打断整个流程
     */
    @Test
    public void chunkThrowingHttpExceptionIsRetriedUntilSuccess() {
        server.setFailMode(ChunkUploadTestServer.FailMode.THROW_EXCEPTION);
        server.failChunk(4, 2);

        ChunkUploadResult result = api.upload(file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getRetryRounds());
        assertEquals(3, server.attemptsOf(4));
        assertArrayEquals(content, server.mergedContent(result.getFileId()));
    }

    /**
     * 始终失败的分片在重试用尽后如实上报，且不会发起合并请求，也不抛异常
     */
    @Test
    public void permanentlyFailedChunkIsReportedWithoutMerging() {
        server.alwaysFailChunk(3);

        ChunkUploadResult result = api.upload(file);

        assertFalse("分片未全部成功时整体不算成功", result.isSuccess());
        assertFalse(result.isAllChunksUploaded());
        assertFalse(result.isMerged());
        assertNull("未发起合并时不应记录合并异常", result.getMergeError());
        assertEquals(2, result.getRetryRounds());

        List<ChunkResult<Boolean>> failedChunks = result.getFailedChunks();
        assertEquals(1, failedChunks.size());
        assertEquals(3L, failedChunks.get(0).getFileChunk().getIndex());
        assertEquals("首轮+2轮重传", 3, server.attemptsOf(3));
        assertEquals("其余分片仍应上传成功", TOTAL_CHUNKS - 1, result.getSuccessChunks().size());
        assertEquals("不应通知服务端合并", 0, server.hit(MERGE_PATH));
        assertNull("分片不完整时服务端不应有合并结果", server.mergedContent(result.getFileId()));
    }

    /**
     * 断点续传：服务端已有的分片直接跳过，只补传缺失的分片
     */
    @Test
    public void chunksAlreadyOnServerAreNotUploadedAgain() {
        String fileId = "resume-case";
        server.putChunk(fileId, 1, chunkBytes(1));
        server.putChunk(fileId, 3, chunkBytes(3));

        ChunkUploadResult result = api.uploadWithFileId(file, fileId);

        assertTrue(result.isSuccess());
        assertEquals(Arrays.asList(1L, 3L), result.getAlreadyUploadedChunks());
        assertEquals("注解配置的fileId直接使用", fileId, result.getFileId());
        assertEquals("只补传缺失的4个分片", TOTAL_CHUNKS - 2, result.getSuccessChunks().size());
        assertEquals(TOTAL_CHUNKS - 2, server.hit(CHUNK_PATH));
        assertArrayEquals(content, server.mergedContent(fileId));
    }

    /**
     * 合并失败时异常被记录进结果而不是抛给调用方
     */
    @Test
    public void mergeFailureIsRecordedInsteadOfThrown() {
        server.setFailMerge(true);

        ChunkUploadResult result = api.upload(file);

        assertTrue("分片本身是全部上传成功的", result.isAllChunksUploaded());
        assertFalse(result.isMerged());
        assertFalse("合并失败整体不算成功", result.isSuccess());
        assertNull("合并失败时不应记录服务端数据", result.getServerData());
        assertEquals(TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
    }

    /**
     * 秒传命中：不再上传任何分片也不发起合并，服务端返回的内部文件ID透传到结果
     */
    @Test
    public void instantUploadHitSkipsEveryChunkAndMerge() {
        String fileId = "instant-hit";
        // 先用正常链路传完，让服务端持有该文件（/instant端点据此返回命中）
        assertTrue(api.uploadWithFileId(file, fileId).isSuccess());
        int chunkHitsBefore = server.hit(CHUNK_PATH);
        int mergeHitsBefore = server.hit(MERGE_PATH);

        ChunkUploadResult result = api.uploadWithInstant(file, fileId);

        assertTrue("命中秒传整体算成功，实际结果: " + result, result.isSuccess());
        assertTrue(result.isInstantUploadRequired());
        assertTrue(result.isInstantUploaded());
        assertEquals("命中秒传不应再上传分片", chunkHitsBefore, server.hit(CHUNK_PATH));
        assertEquals("命中秒传不应再发起合并", mergeHitsBefore, server.hit(MERGE_PATH));
        assertEquals(1, server.hit(INSTANT_PATH));
        assertEquals("srv-" + fileId, result.getServerData());
    }

    /**
     * 秒传未命中：只记一次探测，照常走全量分片与合并流程
     */
    @Test
    public void instantUploadMissFallsBackToNormalUpload() {
        String fileId = "instant-miss";

        ChunkUploadResult result = api.uploadWithInstant(file, fileId);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertTrue(result.isInstantUploadRequired());
        assertFalse(result.isInstantUploaded());
        assertEquals(1, server.hit(INSTANT_PATH));
        assertEquals(TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
        // serverData来自合并而非秒传
        assertEquals("srv-" + fileId, result.getServerData());
        assertArrayEquals(content, server.mergedContent(fileId));
    }

    /**
     * 返回值声明为boolean时，只给出整体是否成功
     */
    @Test
    public void booleanReturnValueReflectsOverallSuccess() {
        assertTrue("正常链路应返回true", api.uploadQuietly(file));
        assertEquals(1, server.hit(MERGE_PATH));

        // 换一个内容不同的文件，使fileId（内容MD5）与上一次不同，避免被断点续传跳过
        File anotherFile = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(FILE_LENGTH + 1));
        server.alwaysFailChunk(5);
        assertFalse("存在失败分片时应返回false", api.uploadQuietly(anotherFile));
    }

    /**
     * 返回值声明为void时仍能跑完整个上传流程，只是不交出结果对象
     */
    @Test
    public void voidReturnValueStillCompletesTheWholeFlow() {
        api.uploadSilently(file);

        assertEquals(TOTAL_CHUNKS, server.hit(CHUNK_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
        assertEquals(file.getName(), server.getMergedFileName());
        assertArrayEquals(content, server.mergedContent("silent-upload"));
    }

    /**
     * 文档里的表单协议写法（{@code @MultiData}+{@code @MultiFile}上报分片，{@code @Query}上报参数，
     * {@code @RespConvert}取JSON包装结果的data）同样能跑通完整上传流程
     */
    @Test
    public void multipartProtocolFromDocsWorksEndToEnd() {
        ChunkUploadResult result = multipartApi.upload(file);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertEquals("上传前每个分片查询一次是否已上传", TOTAL_CHUNKS, server.hit(UPLOAD_CHUNKS_PATH));
        assertEquals("分片走multipart表单", TOTAL_CHUNKS, server.hit(CHUNK_MULTIPART_PATH));
        assertEquals("不应走二进制请求体的接口", 0, server.hit(CHUNK_PATH));
        assertEquals(1, server.hit(MERGE_PATH));
        assertArrayEquals(content, server.mergedContent(result.getFileId()));
    }

    /**
     * 数值属性声明为String后，字面量与SpEL表达式同道解析：
     * chunkSize/maxConcurrency/maxRetryCount/retryBackoffMillis全部引用方法参数，
     * 验证表达式结果真实生效（切出6片、失败1次后按表达式给出的轮数重传成功）
     */
    @Test
    public void numericAttributesSupportSpelExpressions() {
        server.failChunk(2, 1);

        ChunkUploadResult result = api.uploadWithSpelOptions(file, "spel-upload", 5L, 2, 0L);

        assertTrue("上传应当成功，实际结果: " + result + "；失败详情: " + describeFailures(result), result.isSuccess());
        assertEquals("chunkSize表达式#{chunkSize}应真实生效", 5L, result.getChunkSize());
        assertEquals(TOTAL_CHUNKS, result.getTotalChunks());
        assertEquals("maxRetryCount表达式生效：第2片失败一次后重传成功", 1, result.getRetryRounds());
        assertArrayEquals(content, server.mergedContent("spel-upload"));
    }

    /**
     * 开启deleteOnMergeFailure后，合并失败应触发删除，服务端的分片被回收；
     * 默认（未开启）时合并失败不删除，分片保留待下次续传
     */
    @Test
    public void mergeFailureDeletesServerChunksWhenEnabled() {
        server.setFailMerge(true);

        ChunkUploadResult result = api.uploadWithChunkDelete(file);

        assertFalse("合并失败整体不算成功", result.isSuccess());
        assertTrue("分片本身全部上传成功", result.isAllChunksUploaded());
        assertTrue(result.isDeleteRequired());
        assertTrue(result.isChunksDeleted());
        assertEquals("合并失败后应当发起一次删除", 1, server.hit(DELETE_CHUNKS_PATH));
        assertTrue("删除后服务端不应再持有分片", server.receivedIndexes(result.getFileId()).isEmpty());

        // 对照：普通upload未开启deleteOnMergeFailure，合并失败不触发删除
        int deleteHitsBefore = server.hit(DELETE_CHUNKS_PATH);
        ChunkUploadResult kept = api.upload(file);
        assertFalse(kept.isSuccess());
        assertFalse(kept.isDeleteRequired());
        assertEquals("未开启时不应多发删除请求", deleteHitsBefore, server.hit(DELETE_CHUNKS_PATH));
    }

    /**
     * 把失败分片的异常信息拼成一行，测试不通过时便于定位问题
     *
     * @param result 上传结果
     * @return 失败详情描述
     */
    private static String describeFailures(ChunkUploadResult result) {
        StringBuilder text = new StringBuilder();
        for (ChunkResult<Boolean> chunkResult : result.getFailedChunks()) {
            if (text.length() > 0) {
                text.append(" | ");
            }
            text.append("chunk-").append(chunkResult.getFileChunk().getIndex())
                    .append('=').append(chunkResult.getResult())
                    .append('(').append(chunkResult.getError()).append(')');
        }
        return text.toString();
    }

    /**
     * 取原始内容中指定分片（从1开始）对应的字节，用于预置服务端已有分片
     *
     * @param index 分片索引
     * @return 该分片的内容
     */
    private byte[] chunkBytes(long index) {
        int from = (int) ((index - 1) * ChunkUploadTestApi.TEST_CHUNK_SIZE);
        int to = (int) Math.min(index * ChunkUploadTestApi.TEST_CHUNK_SIZE, content.length);
        return Arrays.copyOfRange(content, from, to);
    }
}
