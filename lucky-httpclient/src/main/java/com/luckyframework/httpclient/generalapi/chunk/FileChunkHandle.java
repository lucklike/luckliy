package com.luckyframework.httpclient.generalapi.chunk;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.luckyframework.httpclient.generalapi.download.RangeDownloadApi.DEFAULT_RANGE_SIZE;

/**
 * 文件分片处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 05:05
 */
public class FileChunkHandle {

    private final File file;
    private final long chunkSize;

    private FileChunkHandle(File file, long chunkSize) {
        this.file = file;
        this.chunkSize = chunkSize;
    }

    /**
     * 获取一个文件分片处理器
     *
     * @param file      原始文件对象
     * @param chunkSize 分片大小
     * @return 文件分片处理器
     */
    public static FileChunkHandle of(File file, long chunkSize) {
        return new FileChunkHandle(file, chunkSize);
    }

    /**
     * 获取一个文件分片处理器, 5M一个分片
     *
     * @param file 原始文件对象
     * @return 文件分片处理器
     */
    public static FileChunkHandle of(File file) {
        return new FileChunkHandle(file, DEFAULT_RANGE_SIZE);
    }


    /**
     * 分片文件处理
     *
     * @param consumer   分片文件消费者
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     */
    public void handle(Consumer<FileChunk> consumer, Runnable onComplete) {
        List<FileChunk> fileChunks = getFileChunks();
        for (FileChunk fileChunk : fileChunks) {
            consumer.accept(fileChunk);
        }
        onComplete.run();
    }

    /**
     * 异步分片文件处理
     *
     * @param executor   异步执行器
     * @param consumer   分片文件消费者
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     */
    public void asyncHandle(Executor executor, Consumer<FileChunk> consumer, Runnable onComplete) {
        List<FileChunk> fileChunks = getFileChunks();
        CompletableFuture<?>[] futureArray = new CompletableFuture[fileChunks.size()];

        for (int i = 0; i < fileChunks.size(); i++) {
            final FileChunk fileChunk = fileChunks.get(i);
            futureArray[i] = CompletableFuture.runAsync(() -> consumer.accept(fileChunk), executor);
        }

        CompletableFuture.allOf(futureArray).thenRun(onComplete).join();
    }

    /**
     * 异步分片文件处理，严格控制并发数
     *
     * @param consumer       分片文件消费者
     * @param onComplete     所有分片文件处理完成后需要执行的逻辑
     * @param maxConcurrency 最大并发数
     */
    public void asyncHandle(Consumer<FileChunk> consumer, Runnable onComplete, int maxConcurrency) {
        List<FileChunk> fileChunks = getFileChunks();
        int fileSize = fileChunks.size();
        int maxThreadSize = Math.min(fileSize, maxConcurrency);
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreadSize);
        asyncHandle(executorService, consumer, () -> {
            onComplete.run();
            executorService.shutdown();
        });
    }

    /**
     * 获取所有分片文件集合
     *
     * @return 所有分片文件集合
     */
    public List<FileChunk> getFileChunks() {
        return ChunkUtils.shard(file, chunkSize);
    }

}
