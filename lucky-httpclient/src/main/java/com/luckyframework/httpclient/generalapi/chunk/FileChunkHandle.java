package com.luckyframework.httpclient.generalapi.chunk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * @param onHandle   分片文件处理逻辑
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     */
    public void handle(Consumer<FileChunk> onHandle, Runnable onComplete) {
        List<FileChunk> fileChunks = getFileChunks();
        for (FileChunk fileChunk : fileChunks) {
            onHandle.accept(fileChunk);
        }
        onComplete.run();
    }

    /**
     * 分片文件处理
     *
     * @param onHandle   分片文件的处理结果
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     * @param <T>        分片文件处理结果类型
     */
    public <T> void handle(Function<FileChunk, T> onHandle, Consumer<List<T>> onComplete) {
        List<FileChunk> fileChunks = getFileChunks();
        List<T> handleResultList = new ArrayList<>(fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            handleResultList.add(onHandle.apply(fileChunk));
        }
        onComplete.accept(handleResultList);
    }

    /**
     * 异步分片文件处理
     *
     * @param executor   异步执行器
     * @param onHandle   分片文件处理逻辑
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     */
    public void asyncHandle(Executor executor, Consumer<FileChunk> onHandle, Runnable onComplete) {
        List<FileChunk> fileChunks = getFileChunks();
        CompletableFuture<?>[] futureArray = new CompletableFuture[fileChunks.size()];

        for (int i = 0; i < fileChunks.size(); i++) {
            final FileChunk fileChunk = fileChunks.get(i);
            futureArray[i] = CompletableFuture.runAsync(() -> onHandle.accept(fileChunk), executor);
        }

        CompletableFuture.allOf(futureArray).thenRun(onComplete).join();
    }

    /**
     * 异步分片文件处理
     *
     * @param executor   异步执行器
     * @param onHandle   分片文件处理逻辑
     * @param onComplete 所有分片文件处理完成后需要执行的逻辑
     * @param <T>        分片文件处理结果类型
     */
    @SuppressWarnings("unchecked")
    public <T> void asyncHandle(Executor executor, Function<FileChunk, T> onHandle, Consumer<List<T>> onComplete) {
        List<FileChunk> fileChunks = getFileChunks();

        CompletableFuture<?>[] futureArray = new CompletableFuture[fileChunks.size()];
        for (int i = 0; i < fileChunks.size(); i++) {
            final FileChunk fileChunk = fileChunks.get(i);
            futureArray[i] = CompletableFuture.supplyAsync(() -> onHandle.apply(fileChunk), executor);
        }

        CompletableFuture.allOf(futureArray).thenRun(() -> {
            List<?> hendleResultList = Stream.of(futureArray).map(CompletableFuture::join).collect(Collectors.toList());
            onComplete.accept((List<T>) hendleResultList);
        }).join();
    }

    /**
     * 异步分片文件处理，严格控制并发数
     *
     * @param onHandle       分片文件处理逻辑
     * @param onComplete     所有分片文件处理完成后需要执行的逻辑
     * @param maxConcurrency 最大并发数
     */
    public void asyncHandle(Consumer<FileChunk> onHandle, Runnable onComplete, int maxConcurrency) {
        ExecutorService executorService = null;
        try {
            List<FileChunk> fileChunks = getFileChunks();
            int fileSize = fileChunks.size();
            int maxThreadSize = Math.min(fileSize, maxConcurrency);
            executorService = Executors.newFixedThreadPool(maxThreadSize);
            asyncHandle(executorService, onHandle, onComplete);
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    /**
     * 异步分片文件处理，严格控制并发数
     *
     * @param onHandle       分片文件处理逻辑
     * @param onComplete     所有分片文件处理完成后需要执行的逻辑
     * @param maxConcurrency 最大并发数
     */
    public <T> void asyncHandle(Function<FileChunk, T> onHandle, Consumer<List<T>> onComplete, int maxConcurrency) {
        ExecutorService executorService = null;
        try {
            List<FileChunk> fileChunks = getFileChunks();
            int fileSize = fileChunks.size();
            int maxThreadSize = Math.min(fileSize, maxConcurrency);
            executorService = Executors.newFixedThreadPool(maxThreadSize);
            asyncHandle(executorService, onHandle, onComplete);
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }

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
