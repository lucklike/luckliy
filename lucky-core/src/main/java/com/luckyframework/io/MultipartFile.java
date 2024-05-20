package com.luckyframework.io;

import com.luckyframework.common.DateUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TimeUtils;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Date;

public class MultipartFile implements InputStreamSource {


    /**
     * 文件类型
     */
    private final String fileType;

    /**
     * 原始的文件名
     */
    private final String originalFileName;

    /**
     * ContentType
     */
    private final String contentType;

    /**
     * 原始输入流源头
     */
    private final InputStreamSource originalFileInputStreamSource;

    /**
     * 原始输入流
     */
    private InputStream originalFileInputStream;

    /**
     * 最终文件名
     */
    private String finalFileName;

    public MultipartFile(InputStreamSource originalFileInputStreamSource, String fileName) {
        this.originalFileInputStreamSource = originalFileInputStreamSource;
        this.originalFileName = fileName;
        this.fileType = "." + StringUtils.getFilenameExtension(this.originalFileName);
        this.contentType = ContentTypeUtils.getMimeType(this.originalFileName);
        this.finalFileName = StringUtils.format("{}_{}_{}{}",
                TimeUtils.formatYyyyMMdd(),
                StringUtils.stripFilenameExtension(this.originalFileName),
                NanoIdUtils.randomNanoId(5),
                fileType);
    }


    public MultipartFile(InputStream originalFileInputStream, String fileName) {
        this(() -> originalFileInputStream, fileName);
    }

    /**
     * 获得文件的类型
     *
     * @return 文件类型
     */
    public String getFileType() {
        return this.fileType;
    }

    public String getContentType() {
        return this.contentType;
    }

    /**
     * 获取最终文件名
     *
     * @return 最终文件名
     */
    public String getFileName() {
        return this.finalFileName;
    }

    /**
     * 获得文件的原始文件名
     *
     * @return 原始文件名
     */
    public String getOriginalFileName() {
        return originalFileName;
    }

    /**
     * 设置上传后的文件名<br/>
     * 该方法会检测传入的文件名是否符合当前的文件类型，如果符合则直接采用，否则会自动加上文件后缀
     *
     * @param fileName 上传后文件在服务器中的文件名
     */
    public void setFileName(String fileName) {
        String fileType = getFileType();
        finalFileName = fileName.endsWith(fileType) ? fileName : fileName + fileType;
    }

    /**
     * 将文件复制到系统的任意位置上文件夹中
     *
     * @param folderPath 绝对路径
     * @throws IOException
     */
    public void copyToFolder(String folderPath) throws IOException {
        File file = new File(folderPath);
        copyToFolder(file);
    }

    private void copyToFolder(File folder) throws IOException {
        if (folder.isFile()) {
            throw new IllegalArgumentException("The destination of the copy must be a folder with the wrong path: " + folder.getAbsolutePath());
        }
        if (!folder.exists()) {
            folder.mkdirs();
        }
        FileOutputStream outfile = new FileOutputStream(folder.getAbsoluteFile() + File.separator + finalFileName);
        FileCopyUtils.copy(getInputStream(), outfile);
    }

    /**
     * 获得上传文件的大小
     *
     * @return 文件的大小
     */
    public int getSize() throws IOException {
        return getInputStream().available();
    }

    /**
     * 获得原始输入流
     *
     * @return 原始输入流
     */
    @NonNull
    public InputStream getInputStream() throws IOException {
        if (this.originalFileInputStream == null) {
            this.originalFileInputStream = this.originalFileInputStreamSource.getInputStream();
        }
        return this.originalFileInputStream;
    }

    /**
     * 获得文件对应的byte数组
     */
    public byte[] getByte() throws IOException {
        return FileCopyUtils.copyToByteArray(getInputStream());
    }

    @Override
    public String toString() {
        int fileSize = 0;
        try {
            fileSize = getSize();
        } catch (IOException e) {
            // 忽略异常
        }
        return StringUtils.format("[{0}k] {1}", fileSize, getOriginalFileName());
    }

}
