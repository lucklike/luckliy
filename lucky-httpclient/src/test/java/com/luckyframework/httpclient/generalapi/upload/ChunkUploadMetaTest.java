package com.luckyframework.httpclient.generalapi.upload;

import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.generalapi.chunk.ChunkUtils;
import com.luckyframework.httpclient.generalapi.chunk.FileChunk;
import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import com.luckyframework.httpclient.proxy.function.DigestFunctions;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 文件级元信息的内容读取与取指纹测试。
 * <pre>
 * 校验{@link ChunkUploadMeta}上与{@link FileChunk}同名的那批方法：
 * 1.作用范围是整个文件，而不是某一片；
 * 2.摘要与Base64的结果与直接对整文件计算的一致，且按流读取，不要求整文件入内存；
 * 3.默认文件名取服务端保存时使用的名称，而不是原始文件名。
 * </pre>
 */
public class ChunkUploadMetaTest {

    private static final long CHUNK_SIZE = 5L;

    @Test
    public void contentMethodsCoverTheWholeFile() {
        byte[] content = ChunkTestSupport.sequence(26);
        File file = ChunkTestSupport.fileWithContent(content);
        ChunkUploadMeta meta = metaOf(file);

        assertArrayEquals(content, meta.getContent());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        meta.writeContentTo(out);
        assertArrayEquals(content, out.toByteArray());
        // 与FileChunk保持一致：不关闭也不刷新传入的流，由调用方负责，因此还能继续往后写
        out.write('!');
        assertEquals('!', out.toByteArray()[26]);
    }

    @Test
    public void digestCoversWholeFileNotSingleChunk() throws Exception {
        byte[] content = ChunkTestSupport.sequence(26);
        File file = ChunkTestSupport.fileWithContent(content);
        ChunkUploadMeta meta = metaOf(file);

        assertEquals(DigestFunctions.md5Hex(content), meta.md5());

        // 同名方法的范围不同：分片摘要只覆盖那一片，文件摘要覆盖整个文件，二者不可互相冒充
        List<FileChunk> fileChunks = ChunkUtils.shard(file, CHUNK_SIZE);
        FileChunk firstChunk = fileChunks.get(0);
        assertEquals(DigestFunctions.md5Hex(firstChunk.getContent()), firstChunk.md5());
        assertNotEquals(firstChunk.md5(), meta.md5());
    }

    @Test
    public void digestAndBase64FollowStandardAlgorithms() throws Exception {
        byte[] content = ChunkTestSupport.sequence(26);
        ChunkUploadMeta meta = metaOf(ChunkTestSupport.fileWithContent(content));

        assertEquals(32, meta.md5().length());
        assertEquals(40, meta.sha1().length());
        assertEquals(56, meta.sha224().length());
        assertEquals(64, meta.sha256().length());
        assertEquals(96, meta.sha384().length());
        assertEquals(128, meta.sha512().length());
        // 十六进制一律小写，与服务端按字符串比对时不会因大小写不一致而错判
        assertEquals(meta.md5(), meta.md5().toLowerCase());

        // JDK标准Base64：不换行、保留末尾的=填充
        assertEquals(Base64.getEncoder().encodeToString(content), meta.base64());
    }

    @Test
    public void toHttpFilePrefersServerSideFileName() throws Exception {
        byte[] content = ChunkTestSupport.sequence(13);
        File file = ChunkTestSupport.fileWithContent(content);
        ChunkUploadMeta meta = metaOf(file);

        // 默认用fileName（服务端保存时使用的名称），而不是原始文件名
        HttpFile defaultNamed = meta.toHttpFile();
        assertEquals("srv-name.bin", defaultNamed.getFileName());
        assertNotEquals(file.getName(), defaultNamed.getFileName());
        assertArrayEquals(content, readAll(defaultNamed));

        // 显式传入时只用第一个，与FileChunk#toHttpFile的约定一致
        HttpFile explicitNamed = meta.toHttpFile("blob", "ignored");
        assertEquals("blob", explicitNamed.getFileName());
        assertArrayEquals(content, readAll(explicitNamed));
    }

    @Test
    public void missingFileFailsFast() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(8));
        ChunkUploadMeta meta = metaOf(file);
        assertTrue(file.delete());

        assertThrows(LuckyIOException.class, meta::getContent);
        assertThrows(LuckyIOException.class, () -> meta.writeContentTo(new ByteArrayOutputStream()));
        assertThrows(IOException.class, meta::base64);
    }

    /**
     * 以给定文件构造文件级元信息，文件名故意与原始文件名不同，
     * 便于校验默认文件名取的是哪一个
     *
     * @param file 待上传的文件
     * @return 文件级元信息
     */
    private static ChunkUploadMeta metaOf(File file) {
        int totalChunks = ChunkUtils.shard(file, CHUNK_SIZE).size();
        return ChunkUploadMeta.of(file, "file-id-001", "srv-name.bin", file.length(), CHUNK_SIZE, totalChunks);
    }

    /**
     * 把{@link HttpFile}的内容完整读出
     *
     * @param httpFile 待读取的文件项
     * @return 内容字节
     * @throws IOException 读取失败
     */
    private static byte[] readAll(HttpFile httpFile) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = httpFile.getInputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
        return out.toByteArray();
    }
}
