package com.luckyframework.httpclient.generalapi.file;

import com.luckyframework.async.EnhanceFuture;
import com.luckyframework.async.EnhanceFutureFactory;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
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
public abstract class RangeDownloadApi implements FileApi {

    private static final Logger log = LoggerFactory.getLogger(RangeDownloadApi.class);

    /**
     * 默认的分片大小
     */
    public static final long DEFAULT_RANGE_SIZE = 1024 * 1024 * 5;

    //-----------------------------------------------------------------------------------------------------------------------
    //                                              Http Method
    //-----------------------------------------------------------------------------------------------------------------------


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
    public abstract Range rangeInfo(Request request);

    /**
     * 异步下载分片文件并将文件内容写入到目标文件的指定位置，并返回写入结果
     *
     * @param request    请求对象
     * @param targetFile 本地目标文件
     * @param index      分片索引信息
     * @return 分片文件下载写入结果的Future对象
     */
    @HttpRequest
    @RespConvert("#{$this$.writeDataToFile(targetFile, $streamBody$, index)}")
    @StaticHeader("[SET]Range: bytes=#{index.begin}-#{index.end}")
    public abstract Future<Range.WriterResult> asyncDownloadRangeFile(Request request, @Param("targetFile") File targetFile, @Param("index") Range.Index index);

    /**
     * 下载分片文件并将文件内容写入到目标文件的指定位置，并返回写入结果
     *
     * @param request    请求对象
     * @param targetFile 本地目标文件
     * @param index      分片索引信息
     * @return 分片文件下载写入结果
     */
    @HttpRequest
    @RespConvert("#{$this$.writeDataToFile(targetFile, $streamBody$, index)}")
    @StaticHeader("[SET]Range: bytes=#{index.begin}-#{index.end}")
    public abstract Range.WriterResult downloadRangeFile(Request request, @Param("targetFile") File targetFile, @Param("index") Range.Index index);


    //-----------------------------------------------------------------------------------------------------------------------
    //                                       asyncGetRangeFileContent
    //-----------------------------------------------------------------------------------------------------------------------

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
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param url     资源URL
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir) {
        return downloadRetryIfFail(url, saveDir, -1);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param request 请求信息
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir) {
        return downloadRetryIfFail(request, saveDir, -1);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(Request.get(url), saveDir, maxRetryCount);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(request, saveDir, null, maxRetryCount);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param url      资源URL
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, String filename) {
        return downloadRetryIfFail(Request.get(url), saveDir, filename);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, String filename) {
        return downloadRetryIfFail(request, saveDir, filename, -1);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(Request.get(url), saveDir, filename, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(request, saveDir, filename, DEFAULT_RANGE_SIZE, maxRetryCount);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(Request.get(url), saveDir, filename, rangeSize);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(request, saveDir, filename, rangeSize, -1);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(Request.get(url), saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        File targetFile = rangeFileDownload(request, saveDir, filename, rangeSize);
        int r = 1;
        while (hasFail(targetFile)) {
            if (maxRetryCount > 0 && r >= maxRetryCount) {
                throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit!").printException(log);
            }
            if (log.isDebugEnabled()) {
                log.debug("The presence of retry file '{}' is detected, and the {} retry is started.", getFailFile(targetFile).getAbsolutePath(), r);
            }
            rangeFileDownloadByFailFile(request, targetFile);
            r++;
        }
        return targetFile;
    }

    /**
     * 【GET】分片文件下载，使用默认的文件名和分片大小（5M）
     *
     * @param url     资源地址
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(String url, String saveDir) {
        return rangeFileDownload(Request.get(url), saveDir);
    }

    /**
     * 分片文件下载，使用默认的文件名和分片大小（5M）
     *
     * @param request 请求信息
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(Request request, String saveDir) {
        return rangeFileDownload(request, saveDir, null);
    }

    /**
     * 分片文件下载，使用默认的分片大小（5M）
     *
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(Request request, String saveDir, String filename) {
        return rangeFileDownload(request, saveDir, filename, DEFAULT_RANGE_SIZE);
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
            throw new RangeDownloadException("not support range download: {}", request).printException(log);
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
        doRangeFileDownload(request, targetFile, indexes);
        return targetFile;
    }

    /**
     * 从失败文件中获取分片信息进行文件下载
     *
     * @param request    请求信息
     * @param targetFile 本地写入数据的文件
     */
    public void rangeFileDownloadByFailFile(Request request, File targetFile) {
        doRangeFileDownload(request, targetFile, readDataFromFailFile(getFailFile(targetFile)));
    }

    /**
     * 分片文件下载
     *
     * @param request    请求实例
     * @param targetFile 本地写入数据的文件
     * @param indexes    索引信息
     */
    public void doRangeFileDownload(Request request, File targetFile, List<Range.Index> indexes) {
        List<Future<Range.WriterResult>> futureList = new ArrayList<>(indexes.size());
        for (Range.Index index : indexes) {
            futureList.add(asyncDownloadRangeFile(request.copy(), targetFile, index));
        }
        List<Range.WriterResult> writerResultList = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
            Future<Range.WriterResult> future = futureList.get(i);
            Range.Index index = indexes.get(i);
            try {
                Range.WriterResult writerResult = future.get();
                if (writerResult.fail()) {
                    writerResultList.add(writerResult);
                }
            } catch (Exception e) {
                log.info("Failed to obtain the download result of the fragmented file (Range: bytes={}-{}) . Nested exception is: [{}]-{}", index.getBegin(), index.getEnd(), e, e.getMessage());
                writerResultList.add(Range.WriterResult.forException(index, e));
            }
        }

        // 生成失败文件，删除前需要删除之前生成的
        File failFile = getFailFile(targetFile);
        deleteFailFileIfExists(failFile);
        if (ContainerUtils.isNotEmptyCollection(writerResultList)) {
            writerDataToFailFile(writerResultList, failFile);
        }
    }


    //-----------------------------------------------------------------------------------------------------------------------
    //                                      EnhanceFutureFactory + getRangeFileContent
    //-----------------------------------------------------------------------------------------------------------------------


    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir) {
        return downloadRetryIfFail(enhanceFutureFactory, url, saveDir, -1);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir) {
        return downloadRetryIfFail(enhanceFutureFactory, request, saveDir, -1);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(enhanceFutureFactory, Request.get(url), saveDir, maxRetryCount);
    }


    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(enhanceFutureFactory, request, saveDir, null, maxRetryCount);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, String filename) {
        return downloadRetryIfFail(enhanceFutureFactory, Request.get(url), saveDir, filename);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(enhanceFutureFactory, request, saveDir, filename, -1);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(enhanceFutureFactory, Request.get(url), saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(enhanceFutureFactory, request, saveDir, filename, DEFAULT_RANGE_SIZE, maxRetryCount);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param rangeSize            分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(enhanceFutureFactory, Request.get(url), saveDir, filename, rangeSize);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param rangeSize            分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(enhanceFutureFactory, request, saveDir, filename, rangeSize, -1);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源URL
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param rangeSize            分片大小
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(enhanceFutureFactory, Request.get(url), saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param rangeSize            分片大小
     * @param maxRetryCount        最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        File targetFile = rangeFileDownload(enhanceFutureFactory, request, saveDir, filename, rangeSize);
        int r = 1;
        while (hasFail(targetFile)) {
            if (maxRetryCount > 0 && r >= maxRetryCount) {
                throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit!").printException(log);
            }
            if (log.isDebugEnabled()) {
                log.debug("The presence of retry file '{}' is detected, and the {} retry is started.", getFailFile(targetFile).getAbsolutePath(), r);
            }
            rangeFileDownloadByFailFile(enhanceFutureFactory, request, targetFile);
            r++;
        }
        return targetFile;
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】分片文件下载，使用默认的文件名和分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  资源地址
     * @param saveDir              保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir) {
        return rangeFileDownload(enhanceFutureFactory, Request.get(url), saveDir);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，使用默认的文件名和分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir) {
        return rangeFileDownload(enhanceFutureFactory, request, saveDir, null);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 分片文件下载，使用默认的分片大小（5M）
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename) {
        return rangeFileDownload(enhanceFutureFactory, request, saveDir, filename, DEFAULT_RANGE_SIZE);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param saveDir              保存下载文件的目录
     * @param filename             下载文件的文件名
     * @param rangeSize            分片大小
     * @return 下载完成后的文件实例
     */
    public File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, String filename, long rangeSize) {
        Range range = rangeInfo(request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).printException(log);
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
        doRangeFileDownload(enhanceFutureFactory, request, targetFile, indexes);
        return targetFile;
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 从失败文件中获取分片信息进行文件下载
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求信息
     * @param targetFile           本地写入数据的文件
     */
    public void rangeFileDownloadByFailFile(EnhanceFutureFactory enhanceFutureFactory, Request request, File targetFile) {
        doRangeFileDownload(enhanceFutureFactory, request, targetFile, readDataFromFailFile(getFailFile(targetFile)));
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/>
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求实例
     * @param targetFile           本地写入数据的文件
     * @param indexes              索引信息
     */
    public void doRangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, Request request, File targetFile, List<Range.Index> indexes) {
        EnhanceFuture<Range.WriterResult> enhanceFuture = enhanceFutureFactory.create();
        for (Range.Index index : indexes) {
            enhanceFuture.addAsyncTask(() -> downloadRangeFile(request.copy(), targetFile, index));
        }
        List<Range.WriterResult> writerResultList = new ArrayList<>();
        for (int i = 0; i < indexes.size(); i++) {
            Range.Index index = indexes.get(i);
            try {
                Range.WriterResult writerResult = enhanceFuture.getTaskResult(i);
                if (writerResult.fail()) {
                    writerResultList.add(writerResult);
                }
            } catch (Exception e) {
                log.info("Failed to obtain the download result of the fragmented file (Range: bytes={}-{}) . Nested exception is: [{}]-{}", index.getBegin(), index.getEnd(), e, e.getMessage());
                writerResultList.add(Range.WriterResult.forException(index, e));
            }
        }

        // 生成失败文件，删除前需要删除之前生成的
        File failFile = getFailFile(targetFile);
        deleteFailFileIfExists(failFile);
        if (ContainerUtils.isNotEmptyCollection(writerResultList)) {
            writerDataToFailFile(writerResultList, failFile);
        }
    }

    /**
     * 获取分片文件内容
     *
     * @param targetFile 目标文件
     * @param dataStream 要写入的数据流
     * @param index      分片位置信息
     */
    public Range.WriterResult writeDataToFile(File targetFile, InputStream dataStream, Range.Index index) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw")) {
            randomAccessFile.seek(index.getBegin());
            randomAccessFile.write(FileCopyUtils.copyToByteArray(dataStream));
            return Range.WriterResult.SUCCESS;
        } catch (Exception e) {
            log.debug("When a fragment file (Range: bytes={}-{}) fails to be downloaded, the fragment information and exception information will be recorded in the failed file. Nested exception is: [{}]-{}", index.getBegin(), index.getEnd(), e, e.getMessage());
            return Range.WriterResult.forException(index, e);
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
     * @param writerResultList 失败原因列表
     * @param failFile         失败文件
     */
    private void writerDataToFailFile(List<Range.WriterResult> writerResultList, File failFile) {
        FileUtils.createSaveFolder(failFile.getParentFile());
        try {
            FileCopyUtils.copy(JSON_SCHEME.serialization(writerResultList), new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(failFile.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RangeDownloadException(e, "Failed to generate the failed file '{}'", failFile).printException(log);
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
            return JSON_SCHEME.deserialization(new BufferedReader(new InputStreamReader(Files.newInputStream(failFile.toPath()), StandardCharsets.UTF_8)), new SerializationTypeToken<List<Range.WriterResult>>() {
            }).stream().map(Range.WriterResult::getIndex).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RangeDownloadException(e, "Failed to read the failed file '{}'", failFile).printException(log);
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
            throw new RangeDownloadException(e, "Failed to delete failed file '{}'", failFile).printException(log);
        }
    }
}
