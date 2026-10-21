package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.httpclient.generalapi.chunk.support.ChunkTestSupport;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 分片文件读取与元数据测试。
 * <pre>
 * 校验三件事：
 * 1.切分元数据自包含，单片可独立上报协议参数；
 * 2.getContent()与writeContentTo()读到的字节完全等价，且流式写出不关闭目标流；
 * 3.文件被截断时报LuckyIOException而不是静默少读。
 * </pre>
 */
public class FileChunkTest {

    @Test
    public void eachChunkCarriesSplittingMetadata() {
        byte[] content = ChunkTestSupport.sequence(26);
        File file = ChunkTestSupport.fileWithContent(content);

        List<FileChunk> fileChunks = ChunkUtils.shard(file, 5L);

        assertEquals(6, fileChunks.size());
        for (FileChunk fileChunk : fileChunks) {
            assertEquals(26L, fileChunk.getTotalSize());
            assertEquals(5L, fileChunk.getChunkSize());
            assertEquals(6, fileChunk.getTotalChunks());
            assertEquals(file, fileChunk.getFile());
        }
        // 末片实际大小小于chunkSize，用getSize()而非getChunkSize()计算
        assertEquals(1L, fileChunks.get(5).getSize());
    }

    @Test
    public void getContentConcatEqualsOriginalFile() {
        byte[] content = ChunkTestSupport.sequence(26);
        File file = ChunkTestSupport.fileWithContent(content);

        List<FileChunk> fileChunks = ChunkUtils.shard(file, 5L);

        assertArrayEquals(content, ChunkTestSupport.joinByGetContent(fileChunks));
        assertEquals(26L, ChunkTestSupport.sumOfChunkSizes(fileChunks));
    }

    @Test
    public void writeContentToEqualsGetContentAcrossBufferBoundary() {
        // 100000字节按40000切分，末片20000字节，必然多次跨越8K缓冲区
        byte[] content = ChunkTestSupport.sequence(100000);
        File file = ChunkTestSupport.fileWithContent(content);

        List<FileChunk> fileChunks = ChunkUtils.shard(file, 40000L);

        assertEquals(3, fileChunks.size());
        assertArrayEquals(ChunkTestSupport.joinByGetContent(fileChunks), ChunkTestSupport.joinByWriteContentTo(fileChunks));
        assertArrayEquals(content, ChunkTestSupport.joinByWriteContentTo(fileChunks));
    }

    @Test
    public void writeContentToKeepsTargetStreamOpen() {
        byte[] content = ChunkTestSupport.sequence(12);
        File file = ChunkTestSupport.fileWithContent(content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (FileChunk fileChunk : ChunkUtils.shard(file, 5L)) {
            fileChunk.writeContentTo(out);
        }
        // 约定由调用方负责关闭与刷新，因此写完仍可继续写入
        try {
            out.write('!');
            out.flush();
        } catch (IOException e) {
            throw new AssertionError("writeContentTo should not close the target stream", e);
        }
        byte[] written = out.toByteArray();
        assertEquals(13, written.length);
        assertEquals('!', written[12]);
    }

    @Test
    public void emptyChunkReadsNothing() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(5));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 正常切分不会产生零大小分片，这里直接构造一个空区间分片，确认读取不会误报也不会越界
        Chunk emptyRange = new Chunk();
        emptyRange.setIndex(1);
        emptyRange.setStart(0);
        emptyRange.setEnd(0);
        FileChunk fileChunk = new FileChunk(file, emptyRange, file.length(), 5L, 1);

        assertEquals(0, fileChunk.getContent().length);
        fileChunk.writeContentTo(out);
        assertEquals(0, out.size());
    }

    @Test
    public void truncatedFileFailsFast() throws IOException {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(26));
        List<FileChunk> fileChunks = ChunkUtils.shard(file, 5L);

        // 切分之后文件被截断，读取第三片时区间已越界
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(8L);
        }

        fileChunks.get(0).getContent();
        assertThrows(LuckyIOException.class, () -> fileChunks.get(2).getContent());
        assertThrows(LuckyIOException.class, () -> fileChunks.get(2).writeContentTo(new ByteArrayOutputStream()));
    }

    @Test
    public void chunkAccessorsMirrorPosition() {
        File file = ChunkTestSupport.fileWithContent(ChunkTestSupport.sequence(11));

        FileChunk last = ChunkUtils.shard(file, 5L).get(2);

        assertEquals(3L, last.getIndex());
        assertEquals(10L, last.getStart());
        assertEquals(11L, last.getEnd());
        assertEquals(1L, last.getSize());
        // getChunkSize()是切分依据（恒为5），末片的真实大小看getSize()
        assertEquals(5L, last.getChunkSize());
        assertEquals(last.getStart(), last.getChunk().getStart());
        assertEquals(last.getEnd(), last.getChunk().getEnd());
        assertTrue(last.toString().contains("totalChunks=3"));
    }

    @Test
    public void emptyFileHasNoChunks() {
        File file = ChunkTestSupport.emptyFile();

        List<FileChunk> fileChunks = ChunkUtils.shard(file, 5L);

        assertTrue(fileChunks.isEmpty());
    }

    @Test
    public void illegalFileArgumentIsRejected() {
        File missing = new File(ChunkTestSupport.tempDirectory(), "not-exists.bin");
        File directory = ChunkTestSupport.tempDirectory();

        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard((File) null, 5L));
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(missing, 5L));
        assertThrows(IllegalArgumentException.class, () -> ChunkUtils.shard(directory, 5L));
    }
}
