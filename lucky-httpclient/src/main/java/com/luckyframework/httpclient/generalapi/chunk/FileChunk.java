package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.exception.LuckyIOException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * 分片文件。
 * <pre>
 *     {@link Chunk}与源文件的绑定关系，即「某个文件的某一段字节」，是分片上传的最小操作单位。
 *     除位置信息外，本类还自包含一份切分元数据（总大小、单片大小、分片总数），
 *     使任意一个分片都能独立回答「我是第几片／共几片／文件多大」，
 *     这正是分片上传协议上报{@code chunkIndex/totalChunks/fileSize}所必需的，
 *     也让单片可以脱离分片集合被单独重试或补传。
 *
 *     本类是只读值对象：
 *     1.实例只能由{@link ChunkUtils#shard(File, long)}在包内创建；
 *     2.除读取内容的方法外不产生任何IO。
 *
 *     读取语义：
 *     1.{@link #getContent()}与{@link #writeContentTo(OutputStream)}每次调用都会新开门并关闭一个
 *       {@link RandomAccessFile}，分片很多时会产生开关开销，但换来的是无需管理文件句柄生命周期，
 *       且单片可被任意线程独立读取；
 *     2.{@code getContent()}用readFully把整片读入内存，峰值内存约为并发数×分片大小，
 *       分片较大或内存紧张时请改用{@code writeContentTo()}；
 *     3.文件在切分后被改动（长度变短）时，读取会抛出{@link LuckyIOException}。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 04:12
 * @see Chunk
 * @see Chunks
 * @see ChunkUtils
 * @see FileChunkHandle
 */
public class FileChunk {

    /**
     * 流式读取时的缓冲区大小，与分片大小无关，故写入过程的内存占用恒定
     */
    private static final int COPY_BUFFER_SIZE = 8192;

    /**
     * 分片所属的原始文件
     */
    private final File file;

    /**
     * 本分片的位置信息（索引、开始位置、结束位置）
     */
    private final Chunk chunk;

    /**
     * 原始文件的总大小
     */
    private final long totalSize;

    /**
     * 切分时使用的单片大小，最后一片可能小于该值
     */
    private final long chunkSize;

    /**
     * 分片总数
     */
    private final int totalChunks;

    /**
     * 构造分片文件，包内可见，实例统一由{@link ChunkUtils#shard(File, long)}创建
     *
     * @param file        原始文件
     * @param chunk       本分片的位置信息
     * @param totalSize   原始文件的总大小
     * @param chunkSize   切分时使用的单片大小
     * @param totalChunks 分片总数
     */
    FileChunk(File file, Chunk chunk, long totalSize, long chunkSize, int totalChunks) {
        this.file = file;
        this.chunk = chunk;
        this.totalSize = totalSize;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
    }

    /**
     * 获取文件内容
     * <pre>
     *     从{@link #getStart()}位置读取{@link #getSize()}个字节，整片驻留内存；
     *     分片大小为0时返回长度为0的数组（正常切分不会产生0大小分片）。
     *     需要上传大分片且不想整片进内存时，改用{@link #writeContentTo(OutputStream)}。
     * </pre>
     *
     * @return 文件内容
     * @throws LuckyIOException 文件读取失败，如切分后文件被截断导致内容不足
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
     * 将文件内容流式写出
     * <pre>
     *     从{@link #getStart()}位置起分块读取{@link #getSize()}个字节并写入目标输出流，
     *     全程只占用{@value #COPY_BUFFER_SIZE}字节缓冲区，内存与分片大小无关，适合构造
     *     multipart请求体等不便整片载入内存的场景；
     *     本方法不会关闭也不刷新传入的输出流，由调用方负责。
     * </pre>
     *
     * @param out 内容写出目标流
     * @throws LuckyIOException 文件读取或流写入失败
     */
    public void writeContentTo(OutputStream out) {
        long remaining = chunk.getSize();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(chunk.getStart());
            byte[] buffer = new byte[(int) Math.min(COPY_BUFFER_SIZE, Math.max(remaining, 0))];
            while (remaining > 0) {
                int len = (int) Math.min(buffer.length, remaining);
                raf.readFully(buffer, 0, len);
                out.write(buffer, 0, len);
                remaining -= len;
            }
        } catch (IOException e) {
            throw new LuckyIOException(e, "Failed to write file chunk: [{}]({}-{}){}", chunk.getIndex(), chunk.getStart(), chunk.getEnd(), file.getName());
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
     * @return 分片索引，从1开始
     */
    public long getIndex() {
        return chunk.getIndex();
    }

    /**
     * 获取分片开始位置
     *
     * @return 分片开始位置（含）
     */
    public long getStart() {
        return chunk.getStart();
    }

    /**
     * 获取分片结束位置
     *
     * @return 分片结束位置（不含）
     */
    public long getEnd() {
        return chunk.getEnd();
    }

    /**
     * 获取分片大小
     *
     * @return 分片大小，即本分片包含的字节数
     */
    public long getSize() {
        return chunk.getSize();
    }

    /**
     * 获取原始文件的总大小
     *
     * @return 原始文件的总大小
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * 获取切分时使用的单片大小
     *
     * @return 单片大小，最后一片的实际大小请用{@link #getSize()}
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * 获取分片总数
     * <pre>
     *     空文件为0，此时{@link #getIndex()}不存在大于等于1的分片；
     *     满足分片上传协议中上报{@code totalChunks}的需求，无需再回查分片集合。
     * </pre>
     *
     * @return 分片总数
     */
    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * 以{@code FileChunk{file=, chunk=, totalChunks=}}形式输出分片文件信息，便于日志与问题定位
     *
     * @return 分片文件信息描述
     */
    @Override
    public String toString() {
        return "FileChunk{" +
                "file=" + file.getName() +
                ", chunk=" + chunk +
                ", totalChunks=" + totalChunks +
                '}';
    }
}
