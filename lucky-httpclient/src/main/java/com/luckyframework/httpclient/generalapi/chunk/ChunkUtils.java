package com.luckyframework.httpclient.generalapi.chunk;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片工具类。
 * <pre>
 *     只负责「按固定字节数把总大小切成若干分片」这一件纯计算的事，不做任何IO与网络操作：
 *     1.{@link #shard(long, long)}：按总大小切分，返回带切分依据的{@link Chunks}；
 *     2.{@link #shard(File, long)}：先按文件长度切分，再为每个分片绑定文件得到{@link FileChunk}。
 *
 *     切分规则：
 *     1.分片数按向上取整计算，总大小恰为分片大小整数倍时不会多出末尾空分片；
 *     2.分片区间为[start,end)，各分片大小之和恒等于总大小；
 *     3.分片索引从1开始，分片按开始位置递增排列；
 *     4.总大小为0（空文件）时返回空分片集合，表示无需处理任何分片。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 03:33
 * @see Chunk
 * @see Chunks
 * @see FileChunk
 * @see FileChunkHandle
 */
public class ChunkUtils {

    /**
     * 获取某个文件的分片信息
     * <pre>
     *     空文件（长度为0）返回空集合，表示无需上传任何分片。
     *     每个返回的{@link FileChunk}都自带切分元数据（总大小、单片大小、分片总数），
     *     可单独上报或重试而无需依赖分片集合。
     * </pre>
     *
     * @param file      文件
     * @param chunkSize 分片大小
     * @return 分片信息
     * @throws IllegalArgumentException 文件为null、不存在、不是常规文件，
     *                                  或分片大小不大于0、超过Integer.MAX_VALUE
     */
    @NonNull
    public static List<FileChunk> shard(@NonNull File file, long chunkSize) {

        Assert.notNull(file, "File must not be null");
        Assert.isTrue(file.exists(), "File does not exist");
        Assert.isTrue(file.isFile(), "File is not a file");

        Chunks chunks = shard(file.length(), chunkSize);
        int totalChunks = chunks.getShardCount();
        List<FileChunk> fileChunks = new ArrayList<>(totalChunks);
        for (Chunk chunk : chunks.getChunks()) {
            fileChunks.add(new FileChunk(file, chunk, chunks.getTotal(), chunkSize, totalChunks));
        }
        return fileChunks;
    }

    /**
     * 获取分片信息
     * <pre>
     *     分片数按向上取整计算，因此总大小恰好为分片大小整数倍时不会产生末尾空分片；
     *     总大小为0时返回空分片集合。分片区间为[start,end)，即end不属于本分片。
     * </pre>
     *
     * @param totalSize 总大小
     * @param chunkSize 分片大小
     * @return 分片信息
     * @throws IllegalArgumentException 总大小小于0，或分片大小不大于0、超过Integer.MAX_VALUE
     */
    @NonNull
    public static Chunks shard(long totalSize, long chunkSize) {

        Assert.isTrue(totalSize >= 0L, "Total size must not be negative");
        Assert.isTrue(chunkSize > 0L, "Chunk size must be greater than 0");
        Assert.isTrue(chunkSize <= ChunkConstants.MAX_CHUNK_SIZE,
                "Chunk size must not be greater than " + ChunkConstants.MAX_CHUNK_SIZE);

        Chunks chunkList = new Chunks();
        chunkList.setChunkSize(chunkSize);
        chunkList.setTotal(totalSize);

        // 用 (totalSize-1)/chunkSize+1 代替 (totalSize+chunkSize)/chunkSize 做向上取整，既避免整除时多切一空分片，也避免相加溢出
        long chunkCount = totalSize == 0L ? 0L : (totalSize - 1) / chunkSize + 1;
        List<Chunk> chunks = new ArrayList<>((int) Math.min(chunkCount, Integer.MAX_VALUE));
        for (long i = 0; i < chunkCount; i++) {
            long begin = i * chunkSize;
            Chunk chunk = new Chunk();
            chunk.setIndex(i + 1);
            chunk.setStart(begin);
            chunk.setEnd(Math.min(begin + chunkSize, totalSize));
            chunks.add(chunk);
        }

        chunkList.setChunks(chunks);
        return chunkList;
    }
}
