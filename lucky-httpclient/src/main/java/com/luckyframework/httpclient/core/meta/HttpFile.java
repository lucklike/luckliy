package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.UnitUtils;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
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
    private final String descriptor;

    public HttpFile(InputStreamSource inputStreamSource, Supplier<String> fileNameSupp, String descriptor) {
        this.inputStreamSource = inputStreamSource;
        this.fileNameSupp = fileNameSupp;
        this.descriptor = descriptor;
    }

    public HttpFile(File file) {
        this(() -> Files.newInputStream(file.toPath()), file::getName, file.getAbsolutePath());
    }

    public HttpFile(File file, String fileName) {
        this(() -> Files.newInputStream(file.toPath()), () -> fileName, file.getAbsolutePath());
    }

    public HttpFile(MultipartFile multipartFile) {
        this(multipartFile, multipartFile::getOriginalFileName, "[MultipartFile] " + multipartFile.getOriginalFileName());
    }

    public HttpFile(MultipartFile multipartFile, String fileName) {
        this(multipartFile, () -> fileName, "[MultipartFile] " + multipartFile.getOriginalFileName());
    }

    public HttpFile(Resource resource) {
        this(resource, resource::getFilename, resource.getDescription());
    }

    public HttpFile(Resource resource, String fileName) {
        this(resource, () -> fileName, resource.getDescription());
    }

    public HttpFile(InputStream inputStream, String fileName) {
        this(() -> inputStream, () -> fileName, "[InputStream] " + fileName);
    }

    public HttpFile(byte[] bytes, String fileName) {
        this(() -> new ByteArrayInputStream(bytes), () -> fileName, String.format("[(%s)byte[]] %s", UnitUtils.byteTo(bytes.length), fileName));
    }

    @NonNull
    public InputStream getInputStream() throws IOException {
        return this.inputStreamSource.getInputStream();
    }

    public String getFileName() {
        return this.fileNameSupp.get();
    }

    public String getDescriptor() {
        return descriptor;
    }
}
