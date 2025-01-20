package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.exception.LuckyIOException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 分片文件
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 04:12
 */
public class FileChunk {

    private final File file;
    private final Chunk chunk;

    FileChunk(File file, Chunk chunk) {
        this.file = file;
        this.chunk = chunk;
    }

    /**
     * 获取文件内容
     *
     * @return 文件内容
     */
    public byte[] getContent() {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long size = chunk.getSize();
            raf.seek(chunk.getStart());
            byte[] bytes = new byte[(int) size];
            raf.readFully(bytes);
            return bytes;
        } catch (IOException e) {
            throw new LuckyIOException(e, "Failed to get file chunk: [{}]({}-{}){}", chunk.getIndex(), chunk.getStart(), chunk.getEnd(), file.getName());
        }
    }

    /**
     * 获取原始文件实例
     *
     * @return 原始文件实例
     */
    public File getFile() {
        return file;
    }

    /**
     * 获取分片信息
     *
     * @return 分片信息
     */
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * 获取分片索引信息
     *
     * @return 分片索引信息
     */
    public long getIndex() {
        return chunk.getIndex();
    }

    /**
     * 获取分片开始位置
     *
     * @return 分片开始位置
     */
    public long getStart() {
        return chunk.getStart();
    }

    /**
     * 获取分片结束位置
     *
     * @return 分片结束位置
     */
    public long getEnd() {
        return chunk.getEnd();
    }

    /**
     * 获取分片大小
     *
     * @return 分片大小
     */
    public long getSize() {
        return chunk.getSize();
    }
}
