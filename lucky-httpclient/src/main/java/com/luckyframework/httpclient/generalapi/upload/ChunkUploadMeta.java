package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import com.luckyframework.httpclient.proxy.function.SerializationFunctions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 分片上传的文件级元信息。
 * <pre>
 *     本类回答「这一次上传的是什么文件、按什么粒度切、一共几片、归属哪个标识」，
 *     是{@link ChunkUploadApi}里那些「拿不到{@link FileChunk}
 *     却需要上报文件级参数」的协议动作的统一入参：
 *     1.{@link ChunkUploadApi#initUpload}：申请服务端生成的标识，通常要报文件名、总大小、分片总数；
 *     2.{@link ChunkUploadApi#checkInstantUpload}：秒传探测通常要报文件名、总大小，以及实现方自行计算的内容摘要；
 *     3.{@link ChunkUploadApi#isChunkUploaded}/{@link ChunkUploadApi#uploadChunk}：分片级动作，除片号与内容外
 *       还要报文件标识，因此与本类一起作为入参；
 *     4.{@link ChunkUploadApi#mergeChunks}：合并通知通常要报文件名、总大小、分片总数，
 *       需要「分片清单」（片号列表、逐片MD5）时由另一个入参{@link ChunkMergeContext}提供；
 *     5.{@link ChunkUploadApi#deleteChunks}：失败清理只需要文件标识；
 *     6.{@link ChunkUploadApi#releaseResources}：上传结束后必然会执行一次的收尾动作，
 *       需要文件级参数时同样从这里取。
 *     与{@link FileChunk}的分工：
 *     {@code FileChunk}描述「某一片」，自带{@code index/start/end/size}并复用本类的切分依据；
 *     本类描述「整个文件」，进入分片流程后各动作收到的实例fileId均已确定
 *     （例外：{@link ChunkUploadApi#releaseResources}在申请标识失败时也会被调用，那一份fileId仍为null）。
 *
 *     本类是不可变值对象，由{@link ChunkUploadExecutor}在流程中创建：
 *     1.{@link #getFileId()}在{@link ChunkUploadApi#initUpload}阶段可能为{@code null}——
 *       那一刻要申请的正是这个标识，拿到之后再通过{@link #withFileId(String)}派生一份完整实例；
 *     2.属性读取（{@code getFile/getFileId/getFileName/getTotalSize/getChunkSize/getTotalChunks}）
 *       不产生任何IO；内容读取与取指纹的方法会读文件，见下一段。
 *
 *     内容读取与取指纹：{@link #getContent()}、{@link #writeContentTo}、{@link #toHttpFile}、
 *     {@link #base64()}、{@link #md5()}、{@link #sha1()}、{@link #sha224()}、{@link #sha256()}、
 *     {@link #sha384()}、{@link #sha512()}与{@link FileChunk}的同名方法一一对应，
 *     区别只在作用范围：{@code FileChunk}读的是「某一片」，本类读的是「整个文件」。
 *     因此整文件校验（秒传探测、合并前的文件指纹）用{@code meta.md5()}，
 *     逐片校验用{@code fileChunk.md5()}，两者不可互相冒充；
 *     取指纹走流式计算，不会把整个文件读进内存，而{@link #getContent()}与{@link #toHttpFile}
 *     会把整个文件读进内存、{@link #base64()}还会额外放大约4/3倍，大文件不要调用这三个方法；
 *     所有方法每次调用都重新读文件且不缓存结果，同一份内容多处使用时请调用方自行复用。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-29 10:30
 * @see ChunkUploadApi
 * @see ChunkUploadExecutor
 * @see com.luckyframework.httpclient.generalapi.chunk.FileChunk
 */
public class ChunkUploadMeta {

    /**
     * 待上传的原始文件
     */
    private final File file;

    /**
     * 文件唯一标识（部分服务端称为任务ID），申请阶段可能为null
     */
    private final String fileId;

    /**
     * 服务端保存时使用的文件名，取自{@code @ChunkUpload}的fileName属性，未配置时为原始文件名
     */
    private final String fileName;

    /**
     * 原始文件的总字节数
     */
    private final long totalSize;

    /**
     * 切分时使用的单片字节数，最后一片可能小于该值
     */
    private final long chunkSize;

    /**
     * 分片总数，空文件为0
     */
    private final int totalChunks;

    /**
     * 构造文件级元信息，实例由{@link ChunkUploadExecutor}创建
     *
     * @param file        待上传的原始文件
     * @param fileId      文件唯一标识，申请阶段可为null
     * @param fileName    服务端保存时使用的文件名
     * @param totalSize   原始文件的总字节数
     * @param chunkSize   切分时使用的单片字节数
     * @param totalChunks 分片总数，空文件为0
     */
    private ChunkUploadMeta(File file, String fileId, String fileName, long totalSize, long chunkSize, int totalChunks) {
        this.file = file;
        this.fileId = fileId;
        this.fileName = fileName;
        this.totalSize = totalSize;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
    }

    /**
     * 创建文件级元信息
     *
     * @param file        待上传的原始文件
     * @param fileId      文件唯一标识，申请阶段可为null
     * @param fileName    服务端保存时使用的文件名
     * @param totalSize   原始文件的总字节数
     * @param chunkSize   切分时使用的单片字节数
     * @param totalChunks 分片总数，空文件为0
     * @return 文件级元信息
     */
    public static ChunkUploadMeta of(File file, String fileId, String fileName, long totalSize, long chunkSize, int totalChunks) {
        return new ChunkUploadMeta(file, fileId, fileName, totalSize, chunkSize, totalChunks);
    }

    /**
     * 派生一份带指定文件标识的元信息
     * <pre>
     *     用于{@link ChunkUploadApi#initUpload}申请到服务端标识之后：
     *     申请阶段传入的实例{@code fileId}为null，本方法返回的新实例补齐该字段，
     *     后续的秒传探测与合成都使用新实例，避免在不可变对象上开洞。
     * </pre>
     *
     * @param fileId 文件唯一标识
     * @return 补齐文件标识后的新实例
     */
    public ChunkUploadMeta withFileId(String fileId) {
        return new ChunkUploadMeta(file, fileId, fileName, totalSize, chunkSize, totalChunks);
    }

    /**
     * 派生一份带指定分片总数的元信息
     * <pre>
     *     用于{@link ChunkUploadResult}：结果描述「本次实际发生了什么」，而秒传命中时
     *     一片都没有切，分片总数应记为0；申请与探测阶段上报的仍是按配置算出的片数，
     *     两者不能共用同一个实例。
     * </pre>
     *
     * @param totalChunks 分片总数
     * @return 替换分片总数后的新实例
     */
    public ChunkUploadMeta withTotalChunks(int totalChunks) {
        return new ChunkUploadMeta(file, fileId, fileName, totalSize, chunkSize, totalChunks);
    }

    /**
     * 获取待上传的原始文件
     * <pre>
     *     内容摘要（MD5等）、最后修改时间等协议参数需要自行从本对象取用，
     *     框架不预计算摘要，避免在不开启秒传时白读一遍文件。
     * </pre>
     *
     * @return 原始文件
     */
    public File getFile() {
        return file;
    }

    /**
     * 获取文件唯一标识
     *
     * @return 文件唯一标识，{@link ChunkUploadApi#initUpload}阶段可能为null
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * 获取服务端保存时使用的文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取原始文件的总字节数
     *
     * @return 总字节数
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * 获取切分时使用的单片字节数
     *
     * @return 单片字节数，最后一片的实际大小请用{@code FileChunk#getSize()}
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * 获取分片总数
     *
     * @return 分片总数，空文件为0
     */
    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * 读取整个文件的内容
     * <pre>
     *     与{@link FileChunk#getContent()}的区别只在范围：那个从分片起点读{@code size}个字节，
     *     本方法读整个文件。整个文件驻留内存，只适用于小文件（签名、配置、缩略图等）；
     *     几十MB以上的文件请改用{@link #writeContentTo(OutputStream)}，只需要指纹时用
     *     {@link #md5()}/{@link #sha256()}，它们内部流式读取，不会把文件读进内存。
     * </pre>
     *
     * @return 文件内容
     * @throws LuckyIOException 文件读取失败，或文件大小超过{@link Integer#MAX_VALUE}而无法用单个数组承载
     */
    public byte[] getContent() {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new LuckyIOException(e, "Failed to read file: [{}]", file.getName());
        }
    }

    /**
     * 将整个文件的内容流式写出
     * <pre>
     *     与{@link FileChunk#writeContentTo(OutputStream)}的区别只在范围：那个只写一片，本方法写整个文件。
     *     内容按缓冲区拷贝，内存占用与文件大小无关，适合构造上传请求体等不便整文件载入内存的场景；
     *     本方法不会关闭也不刷新传入的输出流，由调用方负责。
     * </pre>
     *
     * @param out 内容写出目标流
     * @throws LuckyIOException 文件读取或流写入失败
     */
    public void writeContentTo(OutputStream out) {
        try {
            Files.copy(file.toPath(), out);
        } catch (IOException e) {
            throw new LuckyIOException(e, "Failed to write file: [{}]", file.getName());
        }
    }

    /**
     * 把整个文件包装成{@link HttpFile}，供multipart协议以「文件项」形式上报
     * <pre>
     *     与{@link FileChunk#toHttpFile}的区别只在范围：那个包装一片，本方法包装整个文件；
     *     默认文件名用{@link #getFileName()}（服务端保存时使用的名称），而不是{@code getFile().getName()}——
     *     注解指定了{@code fileName}属性时两者不同，上报的文件名应与合并时保持一致。
     *     整个文件会读进内存，大文件请改用{@code @MultiFile}直接接收{@link #getFile()}，
     *     或先用{@link #writeContentTo(OutputStream)}自行构造请求体。
     * </pre>
     *
     * @param httpFilename 表单中使用的文件名，可以省略；传入多个时只取第一个
     * @return 以整个文件内容为字节、指定文件名为名称的{@link HttpFile}
     * @throws LuckyIOException 文件读取失败，或文件大小超过{@link Integer#MAX_VALUE}
     * @see #getContent()
     * @see #writeContentTo(OutputStream)
     */
    public HttpFile toHttpFile(String... httpFilename) {
        String filename = !ContainerUtils.isEmptyArray(httpFilename)
                ? httpFilename[0]
                : (fileName == null || fileName.isEmpty() ? file.getName() : fileName);
        return new HttpFile(getContent(), filename);
    }

    /**
     * 计算整个文件内容的标准Base64编码
     * <pre>
     *     编码对象是整个文件，与{@link FileChunk#base64()}（那一片）区分开；
     *     使用JDK的{@link java.util.Base64}编码器，不换行也不去掉末尾的{@code =}填充，
     *     可以直接放进URL参数、JSON字段或表单字段。
     *     Base64结果约为原文的4/3倍，文件稍大就不适合放进报文；需要整文件指纹时改用
     *     {@link #md5()}或{@link #sha256()}（流式计算，内存占用恒定）。
     * </pre>
     *
     * @return 整个文件内容的Base64字符串
     * @throws IOException 序列化工具读取文件失败
     * @see #md5()
     */
    public String base64() throws IOException {
        return SerializationFunctions.base64(file);
    }

    /**
     * 计算整个文件内容的MD5，返回32位小写十六进制字符串
     * <pre>
     *     最常见的整文件指纹：秒传探测（{@link ChunkUploadApi#checkInstantUpload}）要报内容摘要、
     *     或合并前让服务端校验文件完整性时，上报本方法的返回值即可；
     *     它也是内核在服务端不生成标识时采用的fileId兜底方案。
     *     摘要对象是整个文件，不是某一片——逐片指纹请用{@link FileChunk#md5()}。
     *     内部按流读取，内存占用恒定，大文件也适用；每次调用都会重新读一遍文件，不做缓存。
     * </pre>
     *
     * @return 整个文件内容的MD5，32位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see FileChunk#md5()
     * @see #sha256()
     */
    public String md5() throws Exception {
        return DigestFunctions.md5Hex(file);
    }

    /**
     * 计算整个文件内容的SHA-224，返回56位小写十六进制字符串
     * <pre>
     *     摘要对象是整个文件，每次调用都会重新读文件并重新计算，不做缓存；
     *     SHA-224在多数场景可被{@link #sha256()}代替，仅在服务端明确要求该算法时才需要用到。
     * </pre>
     *
     * @return 整个文件内容的SHA-224，56位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see #sha256()
     */
    public String sha224() throws Exception {
        return DigestFunctions.sha224Hex(file);
    }

    /**
     * 计算整个文件内容的SHA-384，返回96位小写十六进制字符串
     * <pre>
     *     摘要对象是整个文件，每次调用都会重新读文件并重新计算，不做缓存；
     *     属于SHA-2家族中与{@link #sha512()}同族但输出更短的算法。
     * </pre>
     *
     * @return 整个文件内容的SHA-384，96位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see #sha512()
     */
    public String sha384() throws Exception {
        return DigestFunctions.sha384Hex(file);
    }

    /**
     * 计算整个文件内容的SHA-512，返回128位小写十六进制字符串
     * <pre>
     *     摘要对象是整个文件，每次调用都会重新读文件并重新计算，不做缓存；
     *     安全性要求高、且服务端支持时优先用它代替{@link #md5()}。
     * </pre>
     *
     * @return 整个文件内容的SHA-512，128位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see #sha256()
     */
    public String sha512() throws Exception {
        return DigestFunctions.sha512Hex(file);
    }

    /**
     * 计算整个文件内容的SHA-256，返回64位小写十六进制字符串
     * <pre>
     *     摘要对象是整个文件，每次调用都会重新读文件并重新计算，不做缓存；
     *     服务端要「整体校验」而不是「按片校验」时，通常用它与{@link #md5()}二者之一作为文件指纹，
     *     而逐片指纹看{@link FileChunk#sha256()}。
     * </pre>
     *
     * @return 整个文件内容的SHA-256，64位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see #md5()
     * @see FileChunk#sha256()
     */
    public String sha256() throws Exception {
        return DigestFunctions.sha256Hex(file);
    }

    /**
     * 计算整个文件内容的SHA-1，返回40位小写十六进制字符串
     * <pre>
     *     摘要对象是整个文件，每次调用都会重新读文件并重新计算，不做缓存；
     *     SHA-1只应用于兼容既有服务端协议，需要防碰撞能力时改用{@link #sha256()}。
     * </pre>
     *
     * @return 整个文件内容的SHA-1，40位小写十六进制
     * @throws Exception 摘要算法初始化失败，或读取文件失败
     * @see #sha256()
     */
    public String sha1() throws Exception {
        return DigestFunctions.sha1Hex(file);
    }

    /**
     * 以{@code ChunkUploadMeta{file=, fileId=, ...}}形式输出文件级元信息，便于日志与问题定位
     *
     * @return 文件级元信息描述
     */
    @Override
    public String toString() {
        return "ChunkUploadMeta{" +
                "file=" + (file == null ? null : file.getName()) +
                ", fileId=" + fileId +
                ", fileName=" + fileName +
                ", totalSize=" + totalSize +
                ", chunkSize=" + chunkSize +
                ", totalChunks=" + totalChunks +
                '}';
    }
}
