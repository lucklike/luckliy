package com.luckyframework.httpclient.generalapi.chunk;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 分片切分计算测试。
 * <pre>
 * 覆盖切分规则的四条约定：
 * 1.分片数向上取整，总大小恰为分片大小整数倍时不会多出末尾空分片；
 * 2.区间为[start,end)且各分片大小之和恒等于总大小；
 * 3.分片索引从1开始、按开始位置递增；
 * 4.总大小为0返回空集合，非法入参在入口即拦截。
 * </pre>
 */
public class ChunkUtilsTest {

    private static final long MAX_CHUNK_SIZE = ChunkConstants.MAX_CHUNK_SIZE;

    @Test
    public void exactMultipleHasNoTrailingEmptyChunk() {
        Chunks chunks = ChunkUtils.shard(10L, 5L);

        assertEquals(2, chunks.getShardCount());
        assertChunk(chunks.getChunks().get(0), 1, 0, 5);
        assertChunk(chunks.getChunks().get(1), 2, 5, 10);
    }

    @Test
    public void remainderProducesShortLastChunk() {
        Chunks chunks = ChunkUtils.shard(11L, 5L);

        assertEquals(3, chunks.getShardCount());
        assertChunk(chunks.getChunks().get(2), 3, 10, 11);
        assertEquals(1L, chunks.getChunks().get(2).getSize());
    }

    @Test
    public void singleByteProducesSingleChunk() {
        Chunks chunks = ChunkUtils.shard(1L, 5L);

        assertEquals(1, chunks.getShardCount());
        assertChunk(chunks.getChunks().get(0), 1, 0, 1);
    }

    @Test
    public void zeroTotalSizeProducesNoChunks() {
        Chunks chunks = ChunkUtils.shard(0L, 5L);

        assertEquals(0, chunks.getShardCount());
        assertTrue(chunks.getChunks().isEmpty());
        assertEquals(0L, chunks.getTotal());
    }

    @Test
    public void chunkSizesSumToTotal() {
        long total = 26L;
        Chunks chunks = ChunkUtils.shard(total, 5L);

        long sum = 0;
        for (Chunk chunk : chunks.getChunks()) {
            sum += chunk.getSize();
        }
        assertEquals(total, sum);
    }

    @Test
    public void indicesAndRangesAreContiguous() {
        Chunks chunks = ChunkUtils.shard(1000L, 7L);
        List<Chunk> list = chunks.getChunks();

        // 向上取整：1000/7=142余6，共143片
        assertEquals(143, list.size());
        long previousEnd = 0L;
        for (int i = 0; i < list.size(); i++) {
            Chunk chunk = list.get(i);
            assertEquals(i + 1, chunk.getIndex());
            assertEquals(previousEnd, chunk.getStart());
            previousEnd = chunk.getEnd();
        }
        assertEquals(1000L, previousEnd);
    }

    @Test
    public void largeInputKeepsCeilingChunkCount() {
        // 3GB按5M切分：3GB不是5M的整数倍，向上取整应为615片而不是整除的614片；
        // 分片数用(total-1)/chunkSize+1计算，避开(total+chunkSize)/chunkSize在大数值下相加溢出的风险
        long total = 3L * 1024 * 1024 * 1024;
        long chunkSize = ChunkConstants.DEFAULT_CHUNK_SIZE;

        Chunks chunks = ChunkUtils.shard(total, chunkSize);

        long sum = 0;
        for (Chunk chunk : chunks.getChunks()) {
            sum += chunk.getSize();
        }
        assertEquals(total, sum);
        assertEquals((total - 1) / chunkSize + 1, chunks.getShardCount());
    }

    @Test
    public void shardKeepsSplittingBasis() {
        Chunks chunks = ChunkUtils.shard(26L, 5L);

        assertEquals(26L, chunks.getTotal());
        assertEquals(5L, chunks.getChunkSize());
    }

    @Test
    public void negativeTotalSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(-1L, 5L));
    }

    @Test
    public void nonPositiveChunkSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(10L, -1L));
    }

    @Test
    public void oversizeChunkSizeIsRejected() {
        // 分片内容最终由byte[]承载，超过int上限在切分入口即拦截，而不是留到读取时才报误导性异常
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(10L, MAX_CHUNK_SIZE + 1));
        assertEquals(1, ChunkUtils.shard(10L, MAX_CHUNK_SIZE).getShardCount());
    }

    private static void assertChunk(Chunk chunk, long index, long start, long end) {
        assertEquals(index, chunk.getIndex());
        assertEquals(start, chunk.getStart());
        assertEquals(end, chunk.getEnd());
    }
}
