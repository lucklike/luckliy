package com.luckyframework.io;

import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 基于本地文件实现的有存储介质的InputStreamSource
 */
public class FileStorageInputStreamSource implements StorageMediumStream, InputStreamSource {

    private final long length;

    private  File tempFile;
    private boolean isDel;

    public FileStorageInputStreamSource(@NonNull File tempFile, @NonNull InputStream in) throws IOException {
        FileUtils.createSaveFolder(tempFile.getParentFile());
        this.tempFile = tempFile;
        this.length = tempFile.length();
        FileCopyUtils.copy(in, Files.newOutputStream(tempFile.toPath()));
    }

    public FileStorageInputStreamSource(@NonNull String tempFilePath, @NonNull InputStream in) throws IOException {
        this(new File(tempFilePath), in);
    }

    public FileStorageInputStreamSource(@NonNull InputStream in) throws IOException {
        this(new File(FileUtils.getLuckyTempDir("@LocalFileStore"), StringUtils.format("{}.ltemp", NanoIdUtils.randomNanoId())), in);
    }

    @Override
    public boolean deleteStorageMedium() {
        try {
            if (!isDel) {
                isDel = Files.deleteIfExists(tempFile.toPath());
                if (isDel) {
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

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        if (isDel) {
            throw new IOException("Storage media has been deleted");
        }
        return Files.newInputStream(tempFile.toPath());
    }
}
