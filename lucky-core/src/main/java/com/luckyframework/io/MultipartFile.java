package com.luckyframework.io;

import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TimeUtils;
import com.luckyframework.common.UnitUtils;
import com.luckyframework.web.ContentTypeUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.springframework.util.StreamUtils.BUFFER_SIZE;

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
     * 最终文件名
     */
    private String finalFileName;

    /**
     * 文件大小
     */
    private long size;

    public MultipartFile(InputStreamSource originalFileInputStreamSource, String fileName, long size) {
        this.size = size;
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

    public MultipartFile(InputStreamSource originalFileInputStreamSource, String fileName) {
        this(originalFileInputStreamSource, fileName, -1L);
    }

    public MultipartFile(InputStream originalFileInputStream, String fileName, long size) {
        this(() -> originalFileInputStream, fileName, size);
    }

    public MultipartFile(InputStream originalFileInputStream, String fileName) {
        this(originalFileInputStream, fileName, -1L);
    }

    public MultipartFile(File file) {
        this(() -> Files.newInputStream(file.toPath()), file.getName(), file.length());
    }

    public MultipartFile(String filePath) {
        this(new File(filePath));
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
        finalFileName = FileUtils.getFileName(fileName, this.originalFileName);
    }

    /**
     * 将文件复制到系统的任意位置上文件夹中
     *
     * @param saveFolderPath 保存文件的文件夹的绝对路径
     * @throws IOException 复制过程中可能出现IO异常
     */
    public void copyToFolder(String saveFolderPath) throws IOException {
        File file = new File(saveFolderPath);
        copyToFolder(file);
    }

    /**
     * 将文件复制到系统的任意位置上文件夹中
     *
     * @param saveFolder 保存文件的文件夹对象
     * @throws IOException 复制过程中可能出现IO异常
     */
    public void copyToFolder(File saveFolder) throws IOException {
        FileUtils.createSaveFolder(saveFolder);
        OutputStream outfile = Files.newOutputStream(new File(saveFolder, finalFileName).toPath());
        FileCopyUtils.copy(getInputStream(), outfile);
    }

    /**
     * 进度监控模式拷贝
     *
     * @param saveFolder 保存文件的文件夹对象
     * @param monitor    进度监控器
     * @param frequency  监控频率
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressMonitorCopy(File saveFolder, ProgressMonitor monitor, int frequency) throws Exception {
        FileUtils.createSaveFolder(saveFolder);
        InputStream in = getInputStream();
        File saveFile = new File(saveFolder, finalFileName);
        OutputStream out = Files.newOutputStream(saveFile.toPath());

        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");

        Progress progress = new Progress(saveFile, getSize());
        monitor.beforeBeginning(progress);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            progress.start();
            int i = 1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                progress.complete(bytesRead);
                if (i % frequency == 0) {
                    monitor.sniffing(progress);
                }
                i++;
            }
            monitor.sniffing(progress);
            out.flush();
            progress.end();
            monitor.afterCompleted(progress);
        } catch (Exception e) {
            monitor.afterFailed(progress, e);
        } finally {
            FileUtils.closeIgnoreException(in);
            FileUtils.closeIgnoreException(out);
        }
    }

    /**
     * 进度监控模式拷贝，默认100KB监控一次
     *
     * @param saveFolder 保存文件的文件夹对象
     * @param monitor    进度监控器
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressMonitorCopy(File saveFolder, ProgressMonitor monitor) throws Exception {
        progressMonitorCopy(saveFolder, monitor, 25);
    }


    /**
     * 进度监控模式拷贝
     *
     * @param saveFolder 保存文件的文件夹路径
     * @param monitor    进度监控器
     * @param frequency  监控频率
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressMonitorCopy(String saveFolder, ProgressMonitor monitor, int frequency) throws Exception {
        progressMonitorCopy(new File(saveFolder), monitor, frequency);
    }

    /**
     * 进度监控模式拷贝，默认100KB监控一次
     *
     * @param saveFolder 保存文件的文件夹路径
     * @param monitor    进度监控器
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressMonitorCopy(String saveFolder, ProgressMonitor monitor) throws Exception {
        progressMonitorCopy(saveFolder, monitor, 25);
    }

    /**
     * 文件拷贝，拷贝过程中会显示进度条
     *
     * @param saveFolder 保存文件的文件夹路径
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressBarCopy(String saveFolder) throws Exception {
        progressMonitorCopy(saveFolder, new ConsolePrintProgressMonitor());
    }

    /**
     * 文件拷贝，拷贝过程中会显示进度条
     *
     * @param saveFolder 保存文件的文件夹对象
     * @throws Exception 复制过程中可能出现异常
     */
    public void progressBarCopy(File saveFolder) throws Exception {
        progressMonitorCopy(saveFolder, new ConsolePrintProgressMonitor());
    }


    /**
     * 获得上传文件的大小
     *
     * @return 文件的大小
     */
    public long getSize() throws IOException {
        if (size == -1L) {
            size = getInputStream().available();
        }
        return size;
    }

    /**
     * 获得原始输入流
     *
     * @return 原始输入流
     */
    @NonNull
    public InputStream getInputStream() throws IOException {
        return this.originalFileInputStreamSource.getInputStream();
    }

    /**
     * 获得文件对应的byte数组
     */
    public byte[] getByte() throws IOException {
        return FileCopyUtils.copyToByteArray(getInputStream());
    }

    @Override
    public String toString() {
        long fileSize = 0;
        try {
            fileSize = getSize();
        } catch (IOException e) {
            // 忽略异常
        }
        return StringUtils.format("[{0}] {1}", UnitUtils.byteTo(fileSize), getFileName());
    }
}
