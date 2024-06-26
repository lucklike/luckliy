package com.luckyframework.httpclient.core.convert;

import com.luckyframework.exception.LuckyRuntimeException;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SpringMultipartFile implements MultipartFile {


    public final com.luckyframework.io.MultipartFile luckyMultipartFile;

    public SpringMultipartFile(com.luckyframework.io.MultipartFile luckyMultipartFile) {
        this.luckyMultipartFile = luckyMultipartFile;
    }


    @Override
    public String getName() {
        return luckyMultipartFile.getFileName();
    }

    @Override
    public String getOriginalFilename() {
        return luckyMultipartFile.getOriginalFileName();
    }

    @Override
    public String getContentType() {
        return luckyMultipartFile.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return getSize() == -1L;
    }

    @Override
    public long getSize() {
        try {
            return luckyMultipartFile.getSize();
        } catch (IOException e) {
            throw new LuckyRuntimeException(e);
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return FileCopyUtils.copyToByteArray(getInputStream());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return luckyMultipartFile.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        luckyMultipartFile.copyToFolder(dest);
    }
}
