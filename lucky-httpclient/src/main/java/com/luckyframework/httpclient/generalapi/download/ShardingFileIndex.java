package com.luckyframework.httpclient.generalapi.download;

import com.luckyframework.common.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

/**
 * 分片文件信息
 */
public class ShardingFileIndex {

    private static final Logger logger = LoggerFactory.getLogger(ShardingFileIndex.class);

    private static final String INDEX_CREATE_COMPLETED_FILE_NAME = "index_create_completed";
    private static final String INDEX_FILE_NAME = "index";
    private static final String SUCCESSFUL_PROCESSING = "success";
    public static final String lineSeparator = java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

    /**
     * 保存下载数据的目标文件
     */
    private final File targetFile;

    /**
     * 保存索引文件和完成文件的目录
     */
    private final File indexDir;

    /**
     * 索引文件
     */
    private final File indexFile;

    /**
     * 记录处理成功的索引文件
     */
    private final File successFile;

    /**
     * 索引文件创建完成的标志文件
     */
    private final File indexCreateCompletedFile;

    /**
     * 分片文件信息类的构造方法
     *
     * @param targetFile 保存下载数据的目标文件
     */
    public ShardingFileIndex(File targetFile) {
        // target file
        this.targetFile = targetFile;

        // index dir
        String infoDirName = String.format("._$%s", targetFile.getName());
        this.indexDir = new File(targetFile.getParent(), infoDirName);

        // index file
        this.indexFile = new File(indexDir, INDEX_FILE_NAME);

        // success file
        this.successFile = new File(indexDir, SUCCESSFUL_PROCESSING);

        // index completed file
        this.indexCreateCompletedFile = new File(this.indexDir, INDEX_CREATE_COMPLETED_FILE_NAME);
    }

    /**
     * 获取保存索引文件和完成文件的目录
     *
     * @return 保存索引文件和完成文件的目录
     */
    public File getIndexDir() {
        return indexDir;
    }

    /**
     * 获取保存下载数据的目标文件
     *
     * @return 保存下载数据的目标文件
     */
    public File getTargetFile() {
        return targetFile;
    }

    /**
     * Info文件夹是否存在
     *
     * @return true/false
     */
    public boolean infoFileDirIsExists() {
        return indexDir.exists();
    }

    /**
     * 索引文件是否已经创建完成
     *
     * @return true/false
     */
    public boolean indexCreatedCompleted() {
        return indexCreateCompletedFile.exists() && indexCreateCompletedFile.isFile();
    }

    /**
     * 获取未处理的索引信息
     *
     * @return 未处理的索引信息
     */
    public List<Range.Index> getUnprocessedIndexes() {
        Set<String> successIndexSet = readIndexContent(successFile);
        Set<String> indexSet = readIndexContent(indexFile);

        // 取差集
        indexSet.removeAll(successIndexSet);

        // 还未处理的索引文件
        List<Range.Index> unprocessedIndexes = new ArrayList<>();
        for (String indexStr : indexSet) {
            String[] indexArray = indexStr.split("-");
            unprocessedIndexes.add(new Range.Index(Long.parseLong(indexArray[0]), Long.parseLong(indexArray[1])));
        }

        return unprocessedIndexes;
    }

    /**
     * 生成索引内容
     *
     * @param range     分片对象
     * @param rangeSize 分片大小
     * @return 索引内容
     */
    public String createInexContent(Range range, long rangeSize) {
        StringBuilder content = new StringBuilder();
        final long length = range.getLength();
        // 计算分片总数（向上取整）
        long totalParts = (length + rangeSize - 1) / rangeSize;

        // 使用流式API生成格式化的分片范围
        LongStream.range(0, totalParts)
                .forEach(i -> {
                    long start = i * rangeSize;
                    long end = Math.min((i + 1) * rangeSize - 1, length - 1);
                    content.append(getIndexFileName(start, end)).append("\n");
                });
        return content.toString();
    }

    /**
     * 创建所有索引文件,以及标志文件
     *
     * @param range     分片对象
     * @param rangeSize 分片大小
     */
    public void createIndexFiles(Range range, long rangeSize) {
        // 创建索引文件夹
        createDirs(indexDir);

        // 创建文件
        createFile(indexFile);
        createFile(successFile);

        // 生成索引数据
        try {
            FileCopyUtils.copy(createInexContent(range, rangeSize), new OutputStreamWriter(Files.newOutputStream(indexFile.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to write to the index data '{}'", indexFile).error(logger);
        }

        // 所有索引文件都创建完成后创建标志文件
        createFile(indexCreateCompletedFile);
    }


    /**
     * 记录写入成功的索引信息
     *
     * @param index 索引信息
     */
    public synchronized void recordSuccessIndex(Range.Index index) {
        // 第二个参数 true 表示追加模式
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(successFile, true))) {
            bw.write(getIndexFileName(index.getBegin(), index.getEnd()));
            bw.newLine(); // 跨平台换行
            bw.flush(); // 刷新缓冲区
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to write success record ['{}']", getIndexFileName(index.getBegin(), index.getEnd()));
        }
    }

    /**
     * 清理所有文件以及文件夹（使用 walkFileTree）
     */
    public void clearFile() {
        if (!Files.exists(indexDir.toPath())) {
            return;
        }
        try {
            // walkFileTree 自动管理资源，无需 try-with-resources
            Files.walkFileTree(indexDir.toPath(), new SimpleFileVisitor<Path>() {
                @NotNull
                @Override
                public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    logger.trace("[🗑️] Deleted file: {}", file);
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    logger.trace("[🗑️] Deleted directory: {}", dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            logger.debug("[🗑️] Successfully deleted directory: {}", indexDir.getAbsolutePath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to delete directory '{}'", indexDir).error(logger);
        }
    }

    /**
     * 读取索引文件内容
     *
     * @param indexFile 索引文件
     * @return 索引文件内容
     */
    private Set<String> readIndexContent(File indexFile) {
        try {
            String contentStr = FileCopyUtils.copyToString(new InputStreamReader(Files.newInputStream(indexFile.toPath()), StandardCharsets.UTF_8));
            if (!StringUtils.hasText(contentStr)) {
                return Collections.emptySet();
            }
            return new HashSet<>(Arrays.asList(contentStr.split(lineSeparator)));
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to read to the index data '{}'", indexFile).error(logger);
        }
    }

    /**
     * 获取索引文件的文件名
     *
     * @param beginIndex 开始位置
     * @param endIndex   结束位置
     * @return 索引文件名
     */
    private String getIndexFileName(long beginIndex, long endIndex) {
        return String.format("%s-%s", beginIndex, endIndex);
    }

    /**
     * 创建文件夹
     *
     * @param file 文件夹对象
     */
    private void createDirs(File file) {
        try {
            Files.createDirectories(file.toPath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to create dir '{}'", file).error(logger);
        }
    }

    /**
     * 创建文件
     *
     * @param file 文件对象
     */
    private void createFile(File file) {
        try {
            Files.createFile(file.toPath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to create file '{}'", file).error(logger);
        }
    }

}
