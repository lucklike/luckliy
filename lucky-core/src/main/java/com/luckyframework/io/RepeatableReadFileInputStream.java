package com.luckyframework.io;

import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;


/**
 * 基于本地文件实现的支持重复读取的输入流
 */
public class RepeatableReadFileInputStream extends InputStream implements StorageMediumStream {

    private final long length;

    private File tempFile;
    private FileInputStream inStream;
    private boolean isDel;

    public RepeatableReadFileInputStream(@NonNull File tempFile, @NonNull InputStream in) throws IOException {
        FileUtils.createSaveFolder(tempFile.getParentFile());
        this.tempFile = tempFile;
        this.length = tempFile.length();
        FileCopyUtils.copy(in, Files.newOutputStream(tempFile.toPath()));
        this.inStream = new FileInputStream(tempFile);
    }

    public RepeatableReadFileInputStream(@NonNull String tempFilePath, @NonNull InputStream in) throws IOException {
        this(new File(tempFilePath), in);
    }

    public RepeatableReadFileInputStream(@NonNull InputStream in) throws IOException {
        this(new File(FileUtils.getLuckyTempDir("@LocalFileStore"), StringUtils.format("{}.ltemp", NanoIdUtils.randomNanoId())), in);
    }


    @Override
    public boolean deleteStorageMedium() {
        try {
            if (!isDel) {
                FileUtils.closeIgnoreException(inStream);
                isDel = Files.deleteIfExists(tempFile.toPath());
                if (isDel) {
                    inStream = null;
                    tempFile = null;
                }
            }
            return isDel;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public int read() throws IOException {
        check();
        return inStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        check();
        return inStream.read(b, off, len);
    }

    @Override
    public void reset() throws IOException {
        check();
        inStream = new FileInputStream(tempFile);
    }

    @Override
    public void close() throws IOException {
        FileUtils.closeIgnoreException(inStream);
        reset();
    }

    private void check() throws IOException {
        if (isDel) {
            throw new IOException("Storage media has been deleted");
        }
    }
}
