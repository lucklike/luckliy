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
     * 是否保证不覆盖已有文件
     */
    private boolean ensureNoOverwrite = false;

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
        this.finalFileName = originalFileName;
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
        this.finalFileName = FileUtils.getFileName(fileName, this.originalFileName);
    }

    /**
     * 是否保证不覆盖已存在的文件
     *
     * @return 保证不覆盖已存在的文件
     */
    public boolean isEnsureNoOverwrite() {
        return ensureNoOverwrite;
    }

    /**
     * 保证不覆盖已存在的文件
     */
    public void ensureNoOverwrite(){
         setEnsureNoOverwrite(true);
    }

    /**
     * 设置是否保证不覆盖已存在的文件
     *
     * @param ensureNoOverwrite 是否保证不覆盖已存在的文件
     */
    public void setEnsureNoOverwrite(boolean ensureNoOverwrite) {
        this.ensureNoOverwrite = ensureNoOverwrite;
    }

    /**
     * 将文件复制到系统的任意位置上文件夹中
     *
     * @param saveFolderPath 保存文件的文件夹的绝对路径
     * @return 目标文件
     * @throws IOException 复制过程中可能出现IO异常
     */
    public File copyToFolder(String saveFolderPath) throws IOException {
        return copyToFolder(new File(saveFolderPath));
    }

    /**
     * 将文件复制到系统的任意位置上文件夹中
     *
     * @param saveFolder 保存文件的文件夹对象
     * @return 目标文件
     * @throws IOException 复制过程中可能出现IO异常
     */
    public File copyToFolder(File saveFolder) throws IOException {
        FileUtils.createSaveFolder(saveFolder);
        File targetFile = getTargetFile(saveFolder, this.finalFileName);
        OutputStream outfile = Files.newOutputStream(targetFile.toPath());
        FileCopyUtils.copy(getInputStream(), outfile);
        return targetFile;
    }

    /**
     * 进度监控模式拷贝
     *
     * @param saveFolder 保存文件的文件夹对象
     * @param monitor    进度监控器
     * @param frequency  监控频率
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressMonitorCopy(File saveFolder, ProgressMonitor monitor, int frequency) throws Exception {
        FileUtils.createSaveFolder(saveFolder);
        InputStream in = getInputStream();
        File saveFile = getTargetFile(saveFolder, this.finalFileName);
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
        return saveFile;
    }

    /**
     * 进度监控模式拷贝，默认100KB监控一次
     *
     * @param saveFolder 保存文件的文件夹对象
     * @param monitor    进度监控器
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressMonitorCopy(File saveFolder, ProgressMonitor monitor) throws Exception {
        return progressMonitorCopy(saveFolder, monitor, 25);
    }


    /**
     * 进度监控模式拷贝
     *
     * @param saveFolder 保存文件的文件夹路径
     * @param monitor    进度监控器
     * @param frequency  监控频率
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressMonitorCopy(String saveFolder, ProgressMonitor monitor, int frequency) throws Exception {
        return progressMonitorCopy(new File(saveFolder), monitor, frequency);
    }

    /**
     * 进度监控模式拷贝，默认100KB监控一次
     *
     * @param saveFolder 保存文件的文件夹路径
     * @param monitor    进度监控器
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressMonitorCopy(String saveFolder, ProgressMonitor monitor) throws Exception {
        return progressMonitorCopy(saveFolder, monitor, 25);
    }

    /**
     * 文件拷贝，拷贝过程中会显示进度条
     *
     * @param saveFolder 保存文件的文件夹路径
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressBarCopy(String saveFolder) throws Exception {
        return progressMonitorCopy(saveFolder, new ConsolePrintProgressMonitor());
    }

    /**
     * 文件拷贝，拷贝过程中会显示进度条
     *
     * @param saveFolder 保存文件的文件夹对象
     * @return 目标文件
     * @throws Exception 复制过程中可能出现异常
     */
    public File progressBarCopy(File saveFolder) throws Exception {
        return progressMonitorCopy(saveFolder, new ConsolePrintProgressMonitor());
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

    /**
     * 获取一个不冲突的目标文件对象（防止文件名覆盖）
     *
     * <p>
     * 此方法用于在指定目录下生成一个安全的文件保存路径。
     * 如果期望的文件名已存在，将自动在文件名后添加序号（如 "file(1).txt"、"file(2).txt"），
     * 直到找到一个不存在的文件名为止。
     * </p>
     *
     * <p><b>命名规则示例：</b></p>
     * <pre>
     * 原始文件名: report.pdf
     * 已存在时生成:
     *   - report(1).pdf
     *   - report(2).pdf
     *   - report(3).pdf
     *   ...
     * </pre>
     *
     * <p><b>边界情况处理：</b></p>
     * <ul>
     *   <li>无扩展名文件（如 "README"）: 生成 "README(1)"</li>
     *   <li>以点结尾的文件（如 "file."）: 生成 "file(1)."</li>
     *   <li>多扩展名文件（如 "archive.tar.gz"）: 只有最后一个 .gz 被视为扩展名，生成 "archive.tar(1).gz"</li>
     * </ul>
     *
     * @param saveDir  文件保存的目标目录，必须是已存在的目录
     * @param fileName 期望保存的文件名（可包含扩展名，如 "document.pdf"）
     * @return 一个确保在目标目录下不存在的 File 对象（不代表文件已创建）
     */
    private File getTargetFile(File saveDir, String fileName) {
        // 文件不存在时，直接返回
        File targetFile = new File(saveDir, fileName);
        if (!ensureNoOverwrite || !targetFile.exists()) {
            return targetFile;
        }

        String fileTemp = "%s(%d)%s";
        String name = StringUtils.stripFilenameExtension(fileName);
        String ext = StringUtils.getFilenameExtension(fileName);
        ext = ext == null ? "" : "." + ext.toLowerCase();

        int i = 1;
        while ((targetFile = new File(saveDir, String.format(fileTemp, name, i, ext))).exists()) {
            i++;
        }
        return targetFile;
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
