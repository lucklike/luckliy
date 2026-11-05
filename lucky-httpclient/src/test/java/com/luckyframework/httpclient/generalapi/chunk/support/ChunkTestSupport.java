package com.luckyframework.httpclient.generalapi.chunk.support;

import com.luckyframework.httpclient.generalapi.chunk.FileChunk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 分片测试的公共夹具：造临时文件、造可校验的字节序列、拼接分片内容。
 */
public class ChunkTestSupport {

    /**
     * 生成一个长度为{@code length}的字节数组，内容随下标变化，便于定位拼接错位问题
     *
     * @param length 长度
     * @return 字节数组
     */
    public static byte[] sequence(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((i * 31 + 7) % 251);
        }
        return bytes;
    }

    /**
     * 以给定内容创建一个临时文件，JVM退出时自动清理
     *
     * @param content 文件内容
     * @return 临时文件
     */
    public static File fileWithContent(byte[] content) {
        try {
            File file = File.createTempFile("chunk-test-", ".bin");
            file.deleteOnExit();
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(content);
            }
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp file", e);
        }
    }

    /**
     * 创建一个空临时文件
     *
     * @return 长度为0的临时文件
     */
    public static File emptyFile() {
        return fileWithContent(new byte[0]);
    }

    /**
     * 创建一个临时目录，可用于校验「传入的不是常规文件」
     *
     * @return 临时目录
     */
    public static File tempDirectory() {
        File file = new File(System.getProperty("java.io.tmpdir"), "chunk-test-dir-" + System.nanoTime());
        if (!file.exists() && !file.mkdirs()) {
            throw new IllegalStateException("Failed to create temp directory");
        }
        file.deleteOnExit();
        return file;
    }

    /**
     * 依次调用每个分片的{@code getContent()}并拼接，用于与原始内容比对
     *
     * @param fileChunks 分片集合
     * @return 拼接后的全部字节
     */
    public static byte[] joinByGetContent(List<FileChunk> fileChunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (FileChunk fileChunk : fileChunks) {
            byte[] content = fileChunk.getContent();
            out.write(content, 0, content.length);
        }
        return out.toByteArray();
    }

    /**
     * 依次把每个分片的内容流式写入同一个输出流，用于与原始内容比对
     *
     * @param fileChunks 分片集合
     * @return 写入的全部字节
     */
    public static byte[] joinByWriteContentTo(List<FileChunk> fileChunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (FileChunk fileChunk : fileChunks) {
            fileChunk.writeContentTo(out);
        }
        return out.toByteArray();
    }

    /**
     * 统计一个文件切分后各分片的大小之和
     *
     * @param fileChunks 分片集合
     * @return 各分片大小之和
     */
    public static long sumOfChunkSizes(List<FileChunk> fileChunks) {
        long sum = 0;
        for (FileChunk fileChunk : fileChunks) {
            sum += fileChunk.getSize();
        }
        return sum;
    }

    /**
     * 让当前线程休眠，供分片处理逻辑制造并发重叠
     *
     * @param millis 毫秒数
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sleeping", e);
        }
    }
}
