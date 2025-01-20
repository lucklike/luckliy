package com.luckyframework.httpclient.generalapi.shard;

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
public class ShardFile {

    private final File file;
    private final Shard shard;

    ShardFile(File file, Shard shard) {
        this.file = file;
        this.shard = shard;
    }

    /**
     * 获取文件内容
     *
     * @return 文件内容
     * @throws IOException 读取过程中可能出现IO异常
     */
    public byte[] getContent() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long size = shard.getSize();
            raf.seek(shard.getStart());
            byte[] bytes = new byte[(int) size];
            raf.read(bytes);
            return bytes;
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
    public Shard getShard() {
        return shard;
    }
}
