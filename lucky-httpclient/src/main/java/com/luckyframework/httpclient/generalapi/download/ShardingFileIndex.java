package com.luckyframework.httpclient.generalapi.download;

import com.luckyframework.common.ContainerUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

/**
 * 分片文件信息
 */
public class ShardingFileIndex {

    private static final Logger logger = LoggerFactory.getLogger(ShardingFileIndex.class);

    private static final String INDEX_CREATE_COMPLETED_FILE_NAME = "index_create_completed";
    private static final Pattern PATTERN = Pattern.compile("\\d+-\\d+");

    /**
     * 保存下载数据的目标文件
     */
    private final File targetFile;

    /**
     * 保存索引文件和完成文件的目录
     */
    private final File indexDir;

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
     * 索引文件是否还未创建
     *
     * @return true/false
     */
    public boolean indexNotCreatedCompleted() {
        return !(indexCreateCompletedFile.exists() && indexCreateCompletedFile.isFile());
    }

    /**
     * 获取未处理的索引信息
     *
     * @return 未处理的索引信息
     */
    public List<Range.Index> getUnprocessedIndexes() {
        // 还未处理的索引文件
        List<Range.Index> unprocessedIndexes = new ArrayList<>();
        File[] matchFiles = indexDir.listFiles((f) -> {
            // 过滤文件夹
            if (f.isDirectory()) {
                return false;
            }

            // 文件名格式过滤
            return PATTERN.matcher(f.getName()).matches();
        });

        // 将未处理完的文件转成索引对象
        if (ContainerUtils.isNotEmptyArray(matchFiles)) {
            for (File indexFile : matchFiles) {
                String[] indexArray = indexFile.getName().split("-");
                unprocessedIndexes.add(new Range.Index(Long.parseLong(indexArray[0]), Long.parseLong(indexArray[1])));
            }
        }
        return unprocessedIndexes;
    }


    /**
     * 创建所有索引文件,以及标志文件
     *
     * @param range     分片对象
     * @param rangeSize 分片大小
     */
    public void createIndexFiles(Range range, long rangeSize) {
        if (rangeSize <= 0) {
            throw new RangeDownloadException("The rangeSize must be greater than 0, but it is {}", rangeSize).error(logger);
        }

        // 创建索引文件夹
        createDirs(indexDir);

        // 生成索引数据
        createAllIndexFile(range, rangeSize);

        // 所有索引文件都创建完成后创建标志文件
        createFile(indexCreateCompletedFile);

        // 将资源总长度记录到标志文件中，用于续传时校验资源是否已发生变化
        recordFileLength(range.getLength());
    }

    /**
     * 创建所有索引文件
     *
     * @param range     范围对象
     * @param rangeSize 分片大小
     */
    private void createAllIndexFile(Range range, long rangeSize) {
        final long length = range.getLength();
        long totalParts = (length + rangeSize - 1) / rangeSize;
        LongStream.range(0, totalParts).forEach(i -> {
            long start = i * rangeSize;
            long end = Math.min((i + 1) * rangeSize - 1, length - 1);
            createFile(new File(indexDir, getIndexFileName(start, end)));
        });
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
     * 获取索引文件的文件名
     *
     * @param beginIndex 开始位置
     * @param endIndex   结束位置
     * @return 索引文件名
     */
    public String getIndexFileName(long beginIndex, long endIndex) {
        return String.format("%s-%s", beginIndex, endIndex);
    }

    /**
     * 删除索引文件
     *
     * @param index 索引数据
     */
    public void deleteIndexFile(Range.Index index) {
        File indexFile = new File(indexDir, getIndexFileName(index.getBegin(), index.getEnd()));
        try {
            Files.deleteIfExists(indexFile.toPath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to delete file '{}'", indexFile).error(logger);
        }
    }

    /**
     * 校验续传状态与本次下载任务是否一致
     * <pre>
     *     1.目标文件被删除或者为空文件时视为不一致
     *     2.服务器资源长度发生变化时视为不一致
     * </pre>
     *
     * @param range 本次下载任务的分片对象
     * @return 续传状态是否与本次下载任务不一致
     */
    public boolean downloadInfoInconsistent(Range range) {
        // 目标文件被删除或者为空时，此前下载的分片数据已经丢失，已有索引不可信
        if (!targetFile.exists() || targetFile.length() == 0) {
            return true;
        }

        // 服务器资源长度发生变化时，此前生成的索引不可用
        long recordedLength = getRecordedLength();
        return recordedLength > 0 && recordedLength != range.getLength();
    }

    /**
     * 获取记录在标志文件中的资源总长度
     *
     * @return 资源总长度，无法获取时返回-1
     */
    public long getRecordedLength() {
        if (indexNotCreatedCompleted()) {
            return -1;
        }
        try {
            String content = new String(Files.readAllBytes(indexCreateCompletedFile.toPath()), StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? -1 : Long.parseLong(content);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 将资源总长度记录到标志文件中
     *
     * @param length 资源总长度
     */
    private void recordFileLength(long length) {
        try {
            Files.write(indexCreateCompletedFile.toPath(), String.valueOf(length).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to write file length to '{}'", indexCreateCompletedFile).error(logger);
        }
    }

    /**
     * 创建文件夹
     *
     * @param file 文件夹对象
     */
    public void createDirs(File file) {
        try {
            Files.createDirectories(file.toPath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to create dir '{}'", file).error(logger);
        }
    }

    /**
     * 创建文件（幂等操作，文件已存在时不进行任何处理）
     *
     * @param file 文件对象
     */
    public void createFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            Files.createFile(file.toPath());
        } catch (IOException e) {
            throw new RangeDownloadException(e, "Failed to create file '{}'", file).error(logger);
        }
    }

}
