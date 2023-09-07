package com.luckyframework.httpclient.core;

import com.luckyframework.io.MultipartFile;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Supplier;

/**
 * Http文件类型
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/1 15:58
 */
public class HttpFile implements InputStreamSource {

    private final InputStreamSource inputStreamSource;
    private final Supplier<String> fileNameSupp;

    private InputStream inputStream;
    private String fileName;

    public HttpFile(InputStreamSource inputStreamSource, Supplier<String> fileNameSupp) {
        this.inputStreamSource = inputStreamSource;
        this.fileNameSupp = fileNameSupp;
    }

    public HttpFile(File file) {
        this(() -> Files.newInputStream(file.toPath()), file::getName);
    }

    public HttpFile(File file, String fileName) {
        this(() -> Files.newInputStream(file.toPath()), () -> fileName);
    }

    public HttpFile(MultipartFile multipartFile) {
        this(multipartFile, multipartFile::getOriginalFileName);
    }

    public HttpFile(MultipartFile multipartFile, String fileName) {
        this(multipartFile, () -> fileName);
    }

    public HttpFile(Resource resource) {
        this(resource, resource::getFilename);
    }

    public HttpFile(Resource resource, String fileName) {
        this(resource, () -> fileName);
    }

    public HttpFile(InputStream inputStream, String fileName) {
        this(() -> inputStream, () -> fileName);
    }

    @NotNull
    public InputStream getInputStream() throws IOException {
        if (this.inputStream == null) {
            return this.inputStreamSource.getInputStream();
        }
        return this.inputStream;
    }

    public String getFileName() {
        if (this.fileName == null) {
            this.fileName = this.fileNameSupp.get();
        }
        return this.fileName;
    }
}
