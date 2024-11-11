package com.luckyframework.httpclient.generalapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.annotations.Head;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.io.FileUtils;
import com.luckyframework.reflect.Param;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 分片文件下载API
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/11 11:11
 */
@SpELImport(fun = Range.class)
public abstract class RangeDownloadApi2 implements FileApi {

    private static final Logger log = LoggerFactory.getLogger(RangeDownloadApi2.class);

    /**
     * 默认的分片大小
     */
    public static final long DEFAULT_RANGE_SIZE = 1024 * 1024 * 5;


    /**
     * 分片下载测试，检测该Url是否支持分片下载
     * <pre>
     *     1.向请求地址发送一个Head请求
     *     2.如果响应头中的存在Accept-Ranges，且值为'bytes'时表示支持分片，否则表示不支持
     * </pre>
     *
     * @param request 请求对象
     * @return 分片信息
     */
    @Head
    @RespConvert("#{#notSupport()}")
    @Condition(assertion = "#{$respHeader$['accept-ranges'] eq 'bytes'}", result = "#{#create($resp$)}")
    abstract Range rangeInfo(Request request);

    /**
     * 异步获取分片文件的数据流
     *
     * @param request 请求对象
     * @param begin   开始位置
     * @param end     结束位置
     * @return 对应分片文件的数据流的Future对象
     */
    @HttpRequest
    @StaticHeader("[SET]Range: bytes=#{begin}-#{end}")
    abstract Future<InputStream> asyncGetRangeFileContent(Request request, @Param("begin") long begin, @Param("end") long end);

    /**
     * 获取分片文件的数据流
     *
     * @param request 请求对象
     * @param begin   开始位置
     * @param end     结束位置
     * @return 对应分片文件的数据流
     */
    @HttpRequest
    @StaticHeader("[SET]Range: bytes=#{begin}-#{end}")
    abstract InputStream getRangeFileStream(Request request, @Param("begin") long begin, @Param("end") long end);

    /**
     * 判断某个资源请求是否支持分片下载
     *
     * @param request 请求实例
     * @return 是否支持分片下载
     */
    public boolean isSupport(Request request) {
        return rangeInfo(request).isSupport();
    }

    /**
     * 是否已经存在失败文件
     *
     * @param targetFile 本地写入数据的文件
     * @return 是否已经存在失败文件
     */
    public boolean hasFail(File targetFile) {
        return getFailFile(targetFile).exists();
    }

    /**
     * 分片文件下载
     *
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(Request request, String saveDir, String filename, long rangeSize) {
        Range range = rangeInfo(request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new LuckyRuntimeException("not support range download: {}", request);
        }

        // 获取分片信息
        List<Range.Index> indexes = new ArrayList<>();
        final long length = range.getLength();
        long begin = 0;
        while (begin <= length) {
            final long end = begin + rangeSize;
            indexes.add(new Range.Index(begin, Math.min(end, length)));
            begin = end + 1;
        }

        // 获取下载后的文件名
        String targetFileName = range.getFilename();
        if (StringUtils.hasText(filename)) {
            targetFileName = filename.contains(".") ? filename : filename + "." + StringUtils.getFilenameExtension(targetFileName);
        }
        File targetFile = new File(saveDir, targetFileName);
        FileUtils.createSaveFolder(targetFile.getParentFile());
        rangeFileDownload(request, targetFile, indexes);
        return targetFile;
    }

    /**
     * 从失败文件中获取分片信息进行文件下载
     *
     * @param request    请求信息
     * @param targetFile 本地写入数据的文件
     */
    public void rangeFileDownloadByFailFile(Request request, File targetFile) {
        rangeFileDownload(request, targetFile, readDataFromFailFile(getFailFile(targetFile)));
    }

    /**
     * 分片文件下载
     *
     * @param request    请求实例
     * @param targetFile 本地写入数据的文件
     * @param indexes    索引信息
     */
    private void rangeFileDownload(Request request, File targetFile, List<Range.Index> indexes) {
        List<Future<InputStream>> futureList = new ArrayList<>(indexes.size());
        for (Range.Index index : indexes) {
            futureList.add(asyncGetRangeFileContent(request.copy(), index.getBegin(), index.getEnd()));
        }
        List<Range.FailCause> failCauseList = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
            Future<InputStream> future = futureList.get(i);
            Range.Index index = indexes.get(i);
            try {
                writeDataToFile(targetFile, future.get(), index.getBegin());
            } catch (Exception e) {
                log.debug("When a fragment file (Range: bytes={}-{}) fails to be downloaded, the fragment information and exception information will be recorded in the failed file. Nested exception is: [{}]-{}", index.getBegin(), index.getEnd(), e, e.getMessage());
                failCauseList.add(Range.FailCause.forException(index, e));
            }
        }

        // 生成失败文件，删除前需要删除之前生成的
        File failFile = getFailFile(targetFile);
        deleteFailFileIfExists(failFile);
        if (ContainerUtils.isNotEmptyCollection(failCauseList)) {
            writerDataToFailFile(failCauseList, failFile);
        }
    }

    /**
     * 获取分片文件内容
     *
     * @param targetFile 目标文件
     * @param dataStream 要写入的数据流
     * @param startIndex 写数据的起始位置
     * @throws IOException 写入失败会抛出该异常
     */
    private void writeDataToFile(File targetFile, InputStream dataStream, long startIndex) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw")) {
            randomAccessFile.seek(startIndex);
            randomAccessFile.write(FileCopyUtils.copyToByteArray(dataStream));
        }
    }

    /**
     * 获取失败文件名称
     *
     * @param targetFile 下载的文件
     * @return 失败文件名称
     */
    private File getFailFile(File targetFile) {
        String failFileName = String.format("__$%s$__.fail", StringUtils.stripFilenameExtension(targetFile.getName()));
        return new File(targetFile.getParent(), failFileName);
    }

    /**
     * 写入数据到失败文件
     *
     * @param failCauseList 失败原因列表
     * @param failFile      失败文件
     */
    private void writerDataToFailFile(List<Range.FailCause> failCauseList, File failFile) {
        FileUtils.createSaveFolder(failFile.getParentFile());
        try {
            FileCopyUtils.copy(JSON_SCHEME.serialization(failCauseList), new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(failFile.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new LuckyRuntimeException(e, "Failed to generate the failed file '{}'", failFile);
        }
    }

    /**
     * 从失败文件中读取索引文件
     *
     * @param failFile 失败文件
     * @return 失败文件中的索引数据
     */
    private List<Range.Index> readDataFromFailFile(File failFile) {
        try {
            return JSON_SCHEME.deserialization(new BufferedReader(new InputStreamReader(Files.newInputStream(failFile.toPath()), StandardCharsets.UTF_8)), new SerializationTypeToken<List<Range.FailCause>>() {
            }).stream().map(Range.FailCause::getIndex).collect(Collectors.toList());
        } catch (Exception e) {
            throw new LuckyRuntimeException(e, "Failed to read the failed file '{}'", failFile);
        }
    }

    /**
     * 删除失败文件
     *
     * @param failFile 失败文件
     */
    private void deleteFailFileIfExists(File failFile) {
        try {
            Files.deleteIfExists(failFile.toPath());
        } catch (IOException e) {
            throw new LuckyRuntimeException(e, "Failed to delete failed file '{}'", failFile);
        }
    }
}
