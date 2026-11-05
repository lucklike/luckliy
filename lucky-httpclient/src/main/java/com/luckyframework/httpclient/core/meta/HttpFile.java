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

    /**
     * 文件的Content-Type，为null时由执行器根据文件名自动推断
     */
    private String contentType;

    private final String descriptor;

    public HttpFile(InputStreamSource inputStreamSource, Supplier<String> fileNameSupp, String descriptor) {
        this(inputStreamSource, fileNameSupp, null, descriptor);
    }

    public HttpFile(InputStreamSource inputStreamSource, Supplier<String> fileNameSupp, String contentType, String descriptor) {
        this.inputStreamSource = inputStreamSource;
        this.fileNameSupp = fileNameSupp;
        this.contentType = contentType;
        this.descriptor = descriptor;
    }

    public HttpFile(File file) {
        this(() -> Files.newInputStream(file.toPath()), file::getName, file.getAbsolutePath());
    }

    public HttpFile(File file, String fileName) {
        this(() -> Files.newInputStream(file.toPath()), () -> fileName, file.getAbsolutePath());
    }

    public HttpFile(File file, String fileName, String contentType) {
        this(() -> Files.newInputStream(file.toPath()), () -> fileName, contentType, file.getAbsolutePath());
    }

    public HttpFile(MultipartFile multipartFile) {
        this(multipartFile, multipartFile::getOriginalFileName, multipartFile.getContentType(),
                "[MultipartFile] " + multipartFile.getOriginalFileName());
    }

    public HttpFile(MultipartFile multipartFile, String fileName) {
        this(multipartFile, () -> fileName, multipartFile.getContentType(),
                "[MultipartFile] " + multipartFile.getOriginalFileName());
    }

    public HttpFile(MultipartFile multipartFile, String fileName, String contentType) {
        this(multipartFile, () -> fileName, contentType, "[MultipartFile] " + multipartFile.getOriginalFileName());
    }

    public HttpFile(Resource resource) {
        this(resource, resource::getFilename, resource.getDescription());
    }

    public HttpFile(Resource resource, String fileName) {
        this(resource, () -> fileName, resource.getDescription());
    }

    public HttpFile(Resource resource, String fileName, String contentType) {
        this(resource, () -> fileName, contentType, resource.getDescription());
    }

    public HttpFile(InputStream inputStream, String fileName) {
        this(() -> inputStream, () -> fileName, "[InputStream] " + fileName);
    }

    public HttpFile(InputStream inputStream, String fileName, String contentType) {
        this(() -> inputStream, () -> fileName, contentType, "[InputStream] " + fileName);
    }

    public HttpFile(byte[] bytes, String fileName) {
        this(() -> new ByteArrayInputStream(bytes), () -> fileName, String.format("[(%s)byte[]] %s", UnitUtils.byteTo(bytes.length), fileName));
    }

    public HttpFile(byte[] bytes, String fileName, String contentType) {
        this(() -> new ByteArrayInputStream(bytes), () -> fileName, contentType,
                String.format("[(%s)byte[]] %s", UnitUtils.byteTo(bytes.length), fileName));
    }

    @NonNull
    public InputStream getInputStream() throws IOException {
        return this.inputStreamSource.getInputStream();
    }

    public String getFileName() {
        return this.fileNameSupp.get();
    }

    /**
     * 获取文件的Content-Type
     *
     * @return 显式指定的Content-Type，未指定时返回null表示由执行器根据文件名推断
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * 设置文件的Content-Type
     * <p>
     * 设置后不再根据文件名推断，传入null时恢复为自动推断
     *
     * @param contentType Content-Type
     * @return 当前对象
     */
    public HttpFile setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getDescriptor() {
        return descriptor;
    }
}
