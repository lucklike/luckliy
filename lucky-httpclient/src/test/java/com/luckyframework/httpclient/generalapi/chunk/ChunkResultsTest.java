package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 分片结果集合与结果分类测试。
 * <pre>
 * 重点锁定两处曾经互相矛盾的语义：
 * 1.空结果集既可能是「没有分片」也可能是「全部分片失败」，但isAllSuccess在两个类中必须一致地返回true；
 * 2.结果集合对外是不可修改视图，防止调用方误改已落定的处理结果；
 * 3.执行维度与业务维度正交：FAILED／SKIPPED形态的结果不会进入业务判据函数，只按执行状态分类。
 * </pre>
 */
public class ChunkResultsTest {

    @Test
    public void emptyResultsAreTreatedAsAllSuccess() {
        ChunkResults<String> empty = new ChunkResults<>(Collections.emptyList());

        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertTrue(empty.isAllSuccess(result -> false));

        ChunkResultClassify<String> classify = empty.classify(result -> true);
        assertTrue(classify.isAllSuccess());
        assertEquals(0, classify.successSize());
        assertEquals(0, classify.failureSize());
    }

    @Test
    public void nullListBecomesEmptyResults() {
        ChunkResults<String> results = new ChunkResults<>(null);

        assertTrue(results.isEmpty());
        assertTrue(results.getResults().isEmpty());
    }

    @Test
    public void classifySplitsResultsAndKeepsChunkOrder() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "ok"),
                ChunkResult.of(fileChunk(1), "refused"),
                ChunkResult.of(fileChunk(2), "ok")
        );

        ChunkResultClassify<String> classify = new ChunkResults<>(list).classify("ok"::equals);

        assertEquals(2, classify.successSize());
        assertEquals(1, classify.failureSize());
        assertFalse(classify.isAllSuccess());
        // 两组内部都保持原有分片顺序
        assertEquals(Arrays.asList(1L, 3L), indicesOf(classify.getSuccess()));
        assertEquals(Collections.singletonList(2L), indicesOf(classify.getFailure()));
    }

    @Test
    public void isAllSuccessReflectsEveryFailure() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "ok"),
                ChunkResult.of(fileChunk(1), "ok")
        );
        ChunkResults<String> results = new ChunkResults<>(list);

        assertTrue(results.isAllSuccess("ok"::equals));
        assertFalse(results.isAllSuccess("nope"::equals));
    }

    @Test
    public void gettersExposeClassifiedGroups() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "ok"),
                ChunkResult.of(fileChunk(1), "fail")
        );
        ChunkResults<String> results = new ChunkResults<>(list);

        assertEquals(1, results.getSuccessResults("ok"::equals).size());
        assertEquals(1, results.getFailureResults("ok"::equals).size());
        // 失败的是第二片，结果集必须保留定位到具体分片的能力
        assertEquals(2L, results.getFailureResults("ok"::equals).getResults().get(0).getFileChunk().getIndex());
    }

    @Test
    public void forEachVisitsEveryResult() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "a"),
                ChunkResult.of(fileChunk(1), "b")
        );
        ChunkResults<String> results = new ChunkResults<>(list);

        List<String> visited = new ArrayList<>();
        results.forEach(result -> visited.add(result.getResult()));
        assertEquals(Arrays.asList("a", "b"), visited);

        List<String> successVisited = new ArrayList<>();
        results.classify(ignored -> true).successForEach(result -> successVisited.add(result.getResult()));
        assertEquals(Arrays.asList("a", "b"), successVisited);

        List<String> failureVisited = new ArrayList<>();
        results.classify(ignored -> false).failureForEach(result -> failureVisited.add(result.getResult()));
        assertEquals(Arrays.asList("a", "b"), failureVisited);
    }

    @Test
    public void resultViewIsUnmodifiable() {
        List<ChunkResult<String>> source = new ArrayList<>();
        source.add(ChunkResult.of(fileChunk(0), "ok"));
        ChunkResults<String> results = new ChunkResults<>(source);

        assertThrows(UnsupportedOperationException.class, () -> results.getResults().add(ChunkResult.of(fileChunk(1), "x")));

        // 构造时拷贝了一份，后续修改入参列表不会影响已落定的结果集
        source.add(ChunkResult.of(fileChunk(2), "late"));
        assertEquals(1, results.size());
    }

    @Test
    public void chunkResultKeepsChunkAndResult() {
        FileChunk fileChunk = fileChunk(0);
        ChunkResult<String> result = ChunkResult.of(fileChunk, "etag");

        assertEquals(fileChunk.getIndex(), result.getFileChunk().getIndex());
        assertEquals("etag", result.getResult());
        assertNull(ChunkResult.of(fileChunk, null).getResult());
    }

    /**
     * 业务判据函数不应收到FAILED／SKIPPED形态的null返回值，这类分片直接归入失败组
     */
    @Test
    public void nonExecutedResultsBypassTheBusinessSuccessFunction() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "ok"),
                ChunkResult.failed(fileChunk(1), new IllegalStateException("boom")),
                ChunkResult.skipped(fileChunk(2))
        );
        ChunkResults<String> results = new ChunkResults<>(list);

        // 判据函数遇到null就失败，以此验证它根本没被调用到FAILED/SKIPPED上
        Function<String, Boolean> strictNeverSeeNull = result -> {
            Objects.requireNonNull(result, "business function must not receive a non-executed chunk");
            return "ok".equals(result);
        };
        assertFalse(results.isAllSuccess(strictNeverSeeNull));
        ChunkResultClassify<String> classify = results.classify(strictNeverSeeNull);
        assertEquals(1, classify.successSize());
        assertEquals(2, classify.failureSize());
    }

    @Test
    public void executionDimensionViewsReflectStatus() {
        List<ChunkResult<String>> list = Arrays.asList(
                ChunkResult.of(fileChunk(0), "ok"),
                ChunkResult.failed(fileChunk(1), new IllegalStateException("boom")),
                ChunkResult.skipped(fileChunk(2))
        );
        ChunkResults<String> results = new ChunkResults<>(list);

        assertTrue(results.hasExecutionFailure());
        assertEquals(Collections.singletonList(2L), indicesOf(results.getResults(ChunkResultStatus.FAILED)));
        assertEquals(Collections.singletonList(3L), indicesOf(results.getResults(ChunkResultStatus.SKIPPED)));
        assertEquals(Collections.singletonList(1L), indicesOf(results.getResults(ChunkResultStatus.SUCCESS)));
        assertThrows(IllegalArgumentException.class, () -> results.getResults(null));
        // 待重传集合包含异常与被跳过两类，顺序与分片顺序一致
        assertEquals(Arrays.asList(2L, 3L), chunkIndicesOf(results.getUncompletedChunks()));
    }

    @Test
    public void allExecutedResultsHaveNoExecutionFailure() {
        List<ChunkResult<String>> list = Collections.singletonList(ChunkResult.of(fileChunk(0), "refused"));
        ChunkResults<String> results = new ChunkResults<>(list);

        // 执行维度成功不代表业务维度成功，两个维度互不影响
        assertFalse(results.hasExecutionFailure());
        assertEquals(1, results.getResults(ChunkResultStatus.SUCCESS).size());
        assertTrue(results.getUncompletedChunks().isEmpty());
        assertFalse(results.isAllSuccess("ok"::equals));
    }

    @Test
    public void chunkResultFactoriesCarryExpectedStatus() {
        FileChunk fileChunk = fileChunk(0);
        IllegalStateException error = new IllegalStateException("boom");

        assertEquals(ChunkResultStatus.SUCCESS, ChunkResult.of(fileChunk, "ok").getStatus());
        assertTrue(ChunkResult.of(fileChunk, "ok").isSuccess());
        assertEquals(ChunkResultStatus.FAILED, ChunkResult.failed(fileChunk, error).getStatus());
        assertSame(error, ChunkResult.failed(fileChunk, error).getError());
        assertEquals(ChunkResultStatus.SKIPPED, ChunkResult.skipped(fileChunk).getStatus());
        assertNull(ChunkResult.skipped(fileChunk).getError());
        // error为null时无法区分失败原因，直接拒绝
        assertThrows(IllegalArgumentException.class, () -> ChunkResult.failed(fileChunk, null));
    }

    private static List<Long> chunkIndicesOf(List<FileChunk> fileChunks) {
        List<Long> indices = new ArrayList<>();
        for (FileChunk fileChunk : fileChunks) {
            indices.add(fileChunk.getIndex());
        }
        return indices;
    }

    private static List<Long> indicesOf(ChunkResults<String> results) {
        List<Long> indices = new ArrayList<>();
        results.forEach(result -> indices.add(result.getFileChunk().getIndex()));
        return indices;
    }

    private static FileChunk fileChunk(int position) {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(15));
        // 5字节一片共3片，用切分结果保证FileChunk的元数据与真实切分一致
        return ChunkUtils.shard(file, 5L).get(position);
    }
}
