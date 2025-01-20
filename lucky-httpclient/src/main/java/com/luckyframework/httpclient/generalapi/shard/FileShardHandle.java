package com.luckyframework.httpclient.generalapi.shard;

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
public class FileShardHandle {

    private final File file;
    private final long shardSize;

    private FileShardHandle(File file, long shardSize) {
        this.file = file;
        this.shardSize = shardSize;
    }

    /**
     * 获取一个文件分片处理器
     *
     * @param file      原始文件对象
     * @param shardSize 分片大小
     * @return 文件分片处理器
     */
    public static FileShardHandle of(File file, long shardSize) {
        return new FileShardHandle(file, shardSize);
    }

    /**
     * 获取一个文件分片处理器, 5M一个分片
     *
     * @param file 原始文件对象
     * @return 文件分片处理器
     */
    public static FileShardHandle of(File file) {
        return new FileShardHandle(file, DEFAULT_RANGE_SIZE);
    }


    /**
     * 分片文件处理
     *
     * @param consumer   分片文件消费者
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     */
    public void handle(Consumer<ShardFile> consumer, Runnable onComplete) {
        List<ShardFile> shardFiles = getShardFiles();
        for (ShardFile shardFile : shardFiles) {
            consumer.accept(shardFile);
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
    public void asyncHandle(Executor executor, Consumer<ShardFile> consumer, Runnable onComplete) {
        List<ShardFile> shardFiles = getShardFiles();
        CompletableFuture<?>[] futureArray = new CompletableFuture[shardFiles.size()];

        for (int i = 0; i < shardFiles.size(); i++) {
            final ShardFile shardFile = shardFiles.get(i);
            futureArray[i] = CompletableFuture.runAsync(() -> consumer.accept(shardFile), executor);
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
    public void asyncHandle(Consumer<ShardFile> consumer, Runnable onComplete, int maxConcurrency) {
        List<ShardFile> shardFiles = getShardFiles();
        int fileSize = shardFiles.size();
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
    public List<ShardFile> getShardFiles() {
        return ShardUtils.shard(file, shardSize);
    }

}
