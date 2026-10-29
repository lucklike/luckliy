package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadMeta;
import com.luckyframework.httpclient.generalapi.upload.ChunkUploadResponse;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import com.luckyframework.httpclient.proxy.function.SerializationFunctions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import static com.luckyframework.httpclient.proxy.function.DigestFunctions.*;

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
 *       {@link #base64()}与{@link #md5()}/{@link #sha256()}等取指纹的方法都基于它，
 *       因此同样会把整片读进内存，且每次调用都重新读文件、不缓存结果；
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
     * 把本分片包装成{@link HttpFile}，供multipart协议以「文件项」形式上报
     * <pre>
     *     multipart表单里的文件项除了字节内容还必须带一个文件名，而{@link #getContent()}
     *     只有裸字节；本方法把两者一起装进{@link HttpFile}，在协议的{@code uploadChunk}
     *     实现里直接返回即可：
     *     {@code
     *     @Override
     *     default boolean uploadChunk(ChunkUploadMeta meta, FileChunk fileChunk) {
     *         return uploadChunkForm(meta.getFileId(), fileChunk.getIndex(), fileChunk.toHttpFile("blob"));
     *     }
     *
     *     @Post("chunk")
     *     boolean uploadChunkForm(@MultiData("fileId") String fileId,
     *                             @MultiData("chunkNumber") long chunkNumber,
     *                             @MultiFile("file") HttpFile chunk);
     *     }
     *     不传文件名时使用{@code 片号 + ".chunk"}（如第1片为{@code 1.chunk}），
     *     它只作为multipart表单里的filename出现，不影响分片内容，也不参与片号对账。
     *     与{@link #getContent()}一样会把整片读进内存，大分片且不想整片驻留时
     *     改用{@link #writeContentTo(OutputStream)}自行构造请求体。
     * </pre>
     *
     * @param chunkFilename 表单中使用的文件名，可以省略；传入多个时只取第一个
     * @return 以本片内容为字节、指定文件名为名称的{@link HttpFile}
     * @throws LuckyIOException 文件读取失败，如切分后文件被截断导致内容不足
     * @see #getContent()
     * @see #writeContentTo(OutputStream)
     */
    public HttpFile toHttpFile(String... chunkFilename) {
        String filename = ContainerUtils.isEmptyArray(chunkFilename)
                        ? String.format("%s.chunk", getIndex())
                        : chunkFilename[0];
        return new HttpFile(getContent(), filename);
    }

    /**
     * 计算本分片内容的标准Base64编码
     * <pre>
     *     编码对象是{@link #getContent()}，即本分片自己的那一段字节，不包含同文件其它分片；
     *     使用JDK的{@link java.util.Base64}编码器，不换行也不去掉末尾的{@code =}填充，
     *     可以直接放进URL参数、JSON字段或表单字段。
     *     每次调用都会重新读文件并重新编码，不做缓存；同一分片多处使用同一个结果时
     *     请调用方自行保存。
     * </pre>
     *
     * @return 本分片内容的Base64字符串
     * @throws IOException      序列化工具读取输入流失败
     * @throws LuckyIOException 文件读取失败，如切分后文件被截断导致内容不足
     * @see #getContent()
     */
    public String base64() throws IOException {
        return SerializationFunctions.base64(getContent());
    }

    /**
     * 计算本分片内容的MD5，返回32位小写十六进制字符串
     * <pre>
     *     最常见的分片指纹：服务端要{@code Map<片号, 分片MD5>}这类报文、或需要逐片校验内容
     *     是否损坏时，把本方法的返回值上报即可；也可用作{@code isChunkUploaded}的比对依据。
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），不是整个文件——
     *     要整个文件的内容MD5请用{@link ChunkUploadMeta#md5()}
     *     （内核侧同样基于{@link DigestFunctions#md5Hex(Object, String...)}，但按流读取，不整文件入内存）。
     *     每次调用都会重新读文件并重新计算，不做缓存。
     * </pre>
     *
     * @return 本分片内容的MD5，32位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #sha1()
     * @see #sha256()
     */
    public String md5() throws Exception {
        return md5Hex(getContent());
    }

    /**
     * 计算本分片内容的SHA-224，返回56位小写十六进制字符串
     * <pre>
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），每次调用都会重新读文件
     *     并重新计算，不做缓存；SHA-224在多数场景可被{@link #sha256()}代替，
     *     仅在服务端明确要求该算法时才需要用到。
     * </pre>
     *
     * @return 本分片内容的SHA-224，56位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #sha256()
     */
    public String sha224() throws Exception {
        return sha224Hex(getContent());
    }

    /**
     * 计算本分片内容的SHA-384，返回96位小写十六进制字符串
     * <pre>
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），每次调用都会重新读文件
     *     并重新计算，不做缓存；属于SHA-2家族中与{@link #sha512()}同族但输出更短的算法。
     * </pre>
     *
     * @return 本分片内容的SHA-384，96位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #sha512()
     */
    public String sha384() throws Exception {
        return sha384Hex(getContent());
    }

    /**
     * 计算本分片内容的SHA-512，返回128位小写十六进制字符串
     * <pre>
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），每次调用都会重新读文件
     *     并重新计算，不做缓存；安全性要求高、且服务端支持时优先用它代替{@link #md5()}。
     * </pre>
     *
     * @return 本分片内容的SHA-512，128位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #sha256()
     */
    public String sha512() throws Exception {
        return sha512Hex(getContent());
    }

    /**
     * 计算本分片内容的SHA-256，返回64位小写十六进制字符串
     * <pre>
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），每次调用都会重新读文件
     *     并重新计算，不做缓存；服务端要「按片校验」而不是整体校验时，
     *     通常用它与{@link #md5()}二者之一作为分片指纹。
     * </pre>
     *
     * @return 本分片内容的SHA-256，64位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #md5()
     */
    public String sha256() throws Exception {
        return sha256Hex(getContent());
    }

    /**
     * 计算本分片内容的SHA-1，返回40位小写十六进制字符串
     * <pre>
     *     摘要对象是本分片自己的那一段字节（{@link #getContent()}），每次调用都会重新读文件
     *     并重新计算，不做缓存；SHA-1只应用于兼容既有服务端协议，
     *     需要防碰撞能力时改用{@link #sha256()}。
     * </pre>
     *
     * @return 本分片内容的SHA-1，40位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取分片内容失败
     * @see #sha256()
     */
    public String sha1() throws Exception {
        return sha1Hex(getContent());
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
