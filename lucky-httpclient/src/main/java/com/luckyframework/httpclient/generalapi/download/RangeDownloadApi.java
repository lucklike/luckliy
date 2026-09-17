package com.luckyframework.httpclient.generalapi.download;

import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.annotations.Head;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.io.FileUtils;
import com.luckyframework.reflect.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static com.luckyframework.httpclient.generalapi.download.Range.WriterResult.FAIL;
import static com.luckyframework.httpclient.generalapi.download.Range.WriterResult.SUCCESS;

/**
 * 分片文件下载API
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/11 11:11
 */
@SpELImport(Range.class)
public abstract class RangeDownloadApi implements FileApi {

    private static final Logger log = LoggerFactory.getLogger(RangeDownloadApi.class);

    /**
     * 默认的分片大小
     */
    public static final long DEFAULT_RANGE_SIZE = 1024 * 1024 * 5;

    /**
     * 默认同时进行下载的最大分片数量，用于限制一次性提交到异步线程池的任务数量
     */
    public static final int DEFAULT_MAX_CONCURRENT_COUNT = 8;

    /**
     * 默认的分片下载失败后重试前的等待时间（毫秒）
     */
    public static final long DEFAULT_RETRY_BACKOFF_MILLIS = 500L;

    //---------------------------------------------------------------------------------------------------------
    //                                          Http Method
    //---------------------------------------------------------------------------------------------------------


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
    @RespConvert("#{notSupport()}")
    @Condition(assertion = "#{$respHeader$['accept-ranges'] eq 'bytes'}", result = "#{create($resp$)}")
    public abstract Range rangeInfo(HttpExecutor httpExecutor, Request request);

    /**
     * 异步下载分片文件并将文件内容写入到目标文件的指定位置，并返回写入结果
     *
     * @param httpExecutor      Http执行器
     * @param request           请求对象
     * @param shardingFileIndex 分片文件信息
     * @param index             分片索引信息
     * @return 分片文件下载写入结果的Future对象
     */
    @HttpRequest
    @RespConvert("#{$this$.writeDataToFile(shardingFileInfo, $streamBody$, index)}")
    @StaticHeader("[SET]Range: bytes=#{index.begin}-#{index.end}")
    @Condition(assertion = "#{$status$ != 206}", exception = "The shard download must return status 206, but the actual status is #{$status$}. Index: #{index.begin}-#{index.end}. ")
    public abstract Future<Range.WriterResult> asyncDownloadRangeFile(HttpExecutor httpExecutor, Request request, @Param("shardingFileInfo") ShardingFileIndex shardingFileIndex, @Param("index") Range.Index index);

    /**
     * 下载分片文件并将文件内容写入到目标文件的指定位置，并返回写入结果
     *
     * @param httpExecutor      Http执行器
     * @param request           请求对象
     * @param shardingFileIndex 分片文件信息
     * @param index             分片索引信息
     * @return 分片文件下载写入结果
     */
    @HttpRequest
    @RespConvert("#{$this$.writeDataToFile(shardingFileInfo, $streamBody$, index)}")
    @StaticHeader("[SET]Range: bytes=#{index.begin}-#{index.end}")
    @Condition(assertion = "#{$status$ != 206}", exception = "The shard download must return status 206, but the actual status is #{$status$}. Index: #{index.begin}-#{index.end}. ")
    public abstract Range.WriterResult downloadRangeFile(HttpExecutor httpExecutor, Request request, @Param("shardingFileInfo") ShardingFileIndex shardingFileIndex, @Param("index") Range.Index index);


    //---------------------------------------------------------------------------------------------------------
    //                                        Writer Data To File
    //---------------------------------------------------------------------------------------------------------

    /**
     * 将分片数据写入文件（流式写入，校验写入的字节数，防止数据不完整或者超出范围时静默损坏文件）
     */
    public Range.WriterResult writeDataToFile(ShardingFileIndex shardingFileIndex,
                                              InputStream dataStream, Range.Index index) {
        File targetFile = shardingFileIndex.getTargetFile();
        long expectedLength = index.getEnd() - index.getBegin() + 1;

        // 使用 try-with-resources 确保资源正确关闭
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw")) {
            randomAccessFile.seek(index.getBegin());

            // 流式写入，最多只写入该分片范围内(expectedLength)的数据
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            long wroteLength = 0;
            int readLength;
            while ((readLength = dataStream.read(buffer)) != -1) {
                totalRead += readLength;
                if (wroteLength < expectedLength) {
                    int writeSize = (int) Math.min(readLength, expectedLength - wroteLength);
                    randomAccessFile.write(buffer, 0, writeSize);
                    wroteLength += writeSize;
                }
            }

            // 校验数据长度，防止数据不完整或者超出范围时静默损坏文件
            if (totalRead != expectedLength) {
                log.error("[❌] The length of the downloaded fragment data is incorrect ([{}]Range: bytes={}-{}). Expected: {}, Actual: {}, TargetFile: {}",
                        targetFile.getName(), index.getBegin(), index.getEnd(), expectedLength, totalRead, targetFile.getAbsolutePath());
                return FAIL;
            }

            // 删除索引文件（删除失败不影响本次写入成功的判定，交由外层重试或者清理逻辑兜底）
            try {
                shardingFileIndex.deleteIndexFile(index);
            } catch (Exception e) {
                log.warn("[⚠️] The fragment data has been written, but failed to delete the index file ([{}]Range: bytes={}-{}). Error: {}",
                        targetFile.getName(), index.getBegin(), index.getEnd(), e.getMessage());
            }

            // 打日志
            log.debug("[✅] Sharding file (Range: bytes={}-{}) downloaded and written to {}, Number of bytes written: {}",
                    index.getBegin(), index.getEnd(), targetFile.getAbsolutePath(), expectedLength);
            return SUCCESS;

        } catch (Exception e) {
            log.error("[❌] Failed to write fragment ([{}]Range: bytes={}-{}). Error: {}, TargetFile: {}",
                    targetFile.getName(), index.getBegin(), index.getEnd(), e.getMessage(), e);
            return FAIL;
        }
    }


    //---------------------------------------------------------------------------------------------------------
    //                                      Judgment Method
    //---------------------------------------------------------------------------------------------------------

    /**
     * 判断某个资源请求是否支持分片下载
     *
     * @param request 请求实例
     * @return 是否支持分片下载
     */
    public boolean isSupport(Request request) {
        return isSupport(null, request);
    }

    /**
     * 判断某个资源请求是否支持分片下载
     *
     * @param request 请求实例
     * @return 是否支持分片下载
     */
    public boolean isSupport(HttpExecutor httpExecutor, Request request) {
        return rangeInfo(httpExecutor, request.change(RequestMethod.HEAD)).isSupport();
    }

    //---------------------------------------------------------------------------------------------------------
    //                                  AsyncDownloadRangeFile
    //---------------------------------------------------------------------------------------------------------


    /**
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param url 资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url) {
        return downloadRetryIfFail((HttpExecutor) null, url);
    }

    /**
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url) {
        return downloadRetryIfFail(httpExecutor, url, getTempDir());
    }

    /**
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param request 请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request) {
        return downloadRetryIfFail((HttpExecutor) null, request);
    }

    /**
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request) {
        return downloadRetryIfFail(httpExecutor, request, getTempDir());
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param url     资源URL
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir) {
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir);
    }


    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir) {
        return downloadRetryIfFail(httpExecutor, url, saveDir, -1);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param request 请求信息
     * @param saveDir 保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir) {
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, -1);
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
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, maxRetryCount);
    }


    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, maxRetryCount);
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
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, maxRetryCount);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, null, maxRetryCount);
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
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, filename);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, String filename) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, filename);
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
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, filename);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, filename, -1);
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
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, filename, maxRetryCount);
    }


    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, filename, maxRetryCount);
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
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, filename, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, filename, DEFAULT_RANGE_SIZE, maxRetryCount);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param url       资源URL
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, long rangeSize) {
        return downloadRetryIfFail((HttpExecutor) null, url, rangeSize);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), rangeSize);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param request   请求信息
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, long rangeSize) {
        return downloadRetryIfFail((HttpExecutor) null, request, rangeSize);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, request, getTempDir(), rangeSize);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, rangeSize);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, rangeSize);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, rangeSize);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, null, rangeSize);
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
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, filename, rangeSize);
    }

    /**
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, filename, rangeSize);
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
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, filename, rangeSize);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, filename, rangeSize, -1);
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
        return downloadRetryIfFail((HttpExecutor) null, url, saveDir, filename, rangeSize, maxRetryCount);
    }


    /**
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, Request.get(url), saveDir, filename, rangeSize, maxRetryCount);
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
        return downloadRetryIfFail((HttpExecutor) null, request, saveDir, filename, rangeSize, maxRetryCount);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param saveDir            保存下载文件的目录
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(httpExecutor, request, saveDir, range, filename, rangeSize, maxConcurrentCount, retryBackoffMillis, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param range         分片信息
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Request request, String saveDir, Range range, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(null, request, saveDir, range, filename, rangeSize, maxRetryCount);
    }


    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param range         分片信息
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, Range range, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(httpExecutor, request, saveDir, range, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param saveDir            保存下载文件的目录
     * @param range              分片信息
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(HttpExecutor httpExecutor, Request request, String saveDir, Range range, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {

        // 创建分片文件信息类
        File targetFile = getTargetFile(saveDir, range.getFilename(), filename);
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(targetFile);

        // 索引文件还未创建时
        if (shardingFileIndex.indexNotCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        } else if (shardingFileIndex.downloadInfoInconsistent(range)) {
            // 续传信息与本次下载任务不一致时（目标文件被删除或者服务器资源已变化），重建索引
            rebuildIndexFiles(shardingFileIndex, range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                retryBackoff(shardingFileIndex, i, retryBackoffMillis, maxRetryCount);
            }

            // 执行分片异步下载
            rangeFileDownload(httpExecutor, request, shardingFileIndex, maxConcurrentCount);
            i++;
        }
        return targetFile;
    }

    /**
     * 【正常流程】分片文件下载
     *
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload((HttpExecutor) null, request, shardingFileIndex);
    }

    /**
     * 【正常流程】分片文件下载
     *
     * @param httpExecutor      Http执行器
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload(httpExecutor, request, shardingFileIndex, DEFAULT_MAX_CONCURRENT_COUNT);
    }

    /**
     * 【正常流程】分片文件下载
     *
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param shardingFileIndex  分片文件信息
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    public void rangeFileDownload(HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex, int maxConcurrentCount) {
        doRangeFileDownload(httpExecutor, request, shardingFileIndex, maxConcurrentCount);
    }

    /**
     * 分片文件下载
     *
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void doRangeFileDownload(Request request, ShardingFileIndex shardingFileIndex) {
        doRangeFileDownload((HttpExecutor) null, request, shardingFileIndex);
    }

    /**
     * 分片文件下载
     *
     * @param httpExecutor      Http执行器
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void doRangeFileDownload(HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        doRangeFileDownload(httpExecutor, request, shardingFileIndex, DEFAULT_MAX_CONCURRENT_COUNT);
    }

    /**
     * 分片文件下载
     *
     * @param httpExecutor       Http执行器
     * @param request            请求实例
     * @param shardingFileIndex  分片文件信息
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    public void doRangeFileDownload(HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex, int maxConcurrentCount) {
        if (maxConcurrentCount <= 0) {
            throw new RangeDownloadException("The maxConcurrentCount must be greater than 0, but it is {}", maxConcurrentCount).error(log);
        }

        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务（限制同时在途的任务数量，避免一次性提交过多的异步任务）
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(asyncDownloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index));
            awaitBatchCompletion(processedResult, maxConcurrentCount);
        }

        // 处理写入结果
        writerResultHandler(shardingFileIndex, unprocessedIndexes, processedResult);
    }


    //---------------------------------------------------------------------------------------------------------
    //                               DownloadRangeFile + Executor
    //---------------------------------------------------------------------------------------------------------

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param url      资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url) {
        return downloadRetryIfFail(executor, httpExecutor, url, getTempDir());
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param request  请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request) {
        return downloadRetryIfFail(executor, null, request);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request) {
        return downloadRetryIfFail(executor, httpExecutor, request, getTempDir());
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param url      资源URL
     * @param saveDir  保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir) {
        return downloadRetryIfFail(executor, httpExecutor, url, saveDir, -1);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir) {
        return downloadRetryIfFail(executor, null, request, saveDir);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, -1);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, maxRetryCount);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, null, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param url      资源URL
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, String filename) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor 自定义线程池
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, -1);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename, maxRetryCount);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, DEFAULT_RANGE_SIZE, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor  自定义线程池
     * @param url       资源URL
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, long rangeSize) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), rangeSize);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor  自定义线程池
     * @param request   请求信息
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, rangeSize);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, getTempDir(), rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor  自定义线程池
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, rangeSize);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor  自定义线程池
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, saveDir, rangeSize);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, null, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor  自定义线程池
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, filename, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor  自定义线程池
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, rangeSize);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor     自定义线程池
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, rangeSize, -1);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, filename, rangeSize, maxRetryCount);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor           自定义线程池
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param saveDir            保存下载文件的目录
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, maxConcurrentCount, retryBackoffMillis, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param request       请求信息
     * @param range         分片信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, range, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      自定义线程池
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param range         分片信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor           自定义线程池
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param range              分片信息
     * @param saveDir            保存下载文件的目录
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(Executor executor, HttpExecutor httpExecutor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {

        // 创建分片文件信息类
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(getTargetFile(saveDir, range.getFilename(), filename));

        // 索引文件还未创建时
        if (shardingFileIndex.indexNotCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        } else if (shardingFileIndex.downloadInfoInconsistent(range)) {
            // 续传信息与本次下载任务不一致时（目标文件被删除或者服务器资源已变化），重建索引
            rebuildIndexFiles(shardingFileIndex, range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                retryBackoff(shardingFileIndex, i, retryBackoffMillis, maxRetryCount);
            }

            // 执行分片异步下载
            rangeFileDownload(executor, httpExecutor, request, shardingFileIndex, maxConcurrentCount);
            i++;
        }
        return shardingFileIndex.getTargetFile();
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【正常流程】分片文件下载
     *
     * @param executor          自定义线程池
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(Executor executor, Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload(executor, null, request, shardingFileIndex);
    }


    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【正常流程】分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload(executor, httpExecutor, request, shardingFileIndex, DEFAULT_MAX_CONCURRENT_COUNT);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 【正常流程】分片文件下载
     *
     * @param executor           自定义线程池
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param shardingFileIndex  分片文件信息
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    public void rangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex, int maxConcurrentCount) {
        doRangeFileDownload(executor, httpExecutor, request, shardingFileIndex, maxConcurrentCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载
     *
     * @param executor          自定义线程池
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void doRangeFileDownload(Executor executor, Request request, ShardingFileIndex shardingFileIndex) {
        doRangeFileDownload(executor, (HttpExecutor) null, request, shardingFileIndex);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void doRangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        doRangeFileDownload(executor, httpExecutor, request, shardingFileIndex, DEFAULT_MAX_CONCURRENT_COUNT);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务</b><br/>
     * 分片文件下载
     *
     * @param executor           自定义线程池
     * @param httpExecutor       Http执行器
     * @param request            请求实例
     * @param shardingFileIndex  分片文件信息
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    public void doRangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex, int maxConcurrentCount) {
        if (maxConcurrentCount <= 0) {
            throw new RangeDownloadException("The maxConcurrentCount must be greater than 0, but it is {}", maxConcurrentCount).error(log);
        }

        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务（限制同时在途的任务数量，避免一次性提交过多的异步任务）
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(CompletableFuture.supplyAsync(() -> downloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index), executor));
            awaitBatchCompletion(processedResult, maxConcurrentCount);
        }

        // 处理写入结果
        writerResultHandler(shardingFileIndex, unprocessedIndexes, processedResult);
    }

    //---------------------------------------------------------------------------------------------------------
    //                            DownloadRangeFile + AsyncTaskExecutor
    //---------------------------------------------------------------------------------------------------------


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param url      资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url);
    }


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url) {
        return downloadRetryIfFail(executor, httpExecutor, url, getTempDir());
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param request  请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request) {
        return downloadRetryIfFail(executor, null, request);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【下载到系统临时文件】<br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request) {
        return downloadRetryIfFail(executor, httpExecutor, request, getTempDir());
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param url      资源URL
     * @param saveDir  保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir);
    }


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir) {
        return downloadRetryIfFail(executor, httpExecutor, url, saveDir, -1);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir) {
        return downloadRetryIfFail(executor, null, request, saveDir);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, -1);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, maxRetryCount);
    }


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名和分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, null, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param url      资源URL
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, String filename) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename);
    }


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor 用于执行异步任务的执行器
     * @param request  请求信息
     * @param saveDir  保存下载文件的目录
     * @param filename 下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M），不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, -1);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的分片大小（5M）
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, DEFAULT_RANGE_SIZE, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param url       资源URL
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, long rangeSize) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param request   请求信息
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、文件保存在系统临时文件、不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, getTempDir(), rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, (HttpExecutor) null, url, saveDir, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, saveDir, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，使用默认的文件名称、不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, null, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param url       资源URL
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param url          资源URL
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor  用于执行异步任务的执行器
     * @param request   请求信息
     * @param saveDir   保存下载文件的目录
     * @param filename  下载文件的文件名
     * @param rangeSize 分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, rangeSize);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试，不限重试次数
     *
     * @param executor     用于执行异步任务的执行器
     * @param httpExecutor Http执行器
     * @param request      请求信息
     * @param saveDir      保存下载文件的目录
     * @param filename     下载文件的文件名
     * @param rangeSize    分片大小
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, rangeSize, -1);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, url, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【GET】分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param url           资源URL
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, String url, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, Request.get(url), saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, saveDir, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor           用于执行异步任务的执行器
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param saveDir            保存下载文件的目录
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, String saveDir, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, maxConcurrentCount, retryBackoffMillis, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param request       请求信息
     * @param range         分片信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, null, request, range, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor      用于执行异步任务的执行器
     * @param httpExecutor  Http执行器
     * @param request       请求信息
     * @param range         分片信息
     * @param saveDir       保存下载文件的目录
     * @param filename      下载文件的文件名
     * @param rangeSize     分片大小
     * @param maxRetryCount 最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxRetryCount) {
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, DEFAULT_MAX_CONCURRENT_COUNT, DEFAULT_RETRY_BACKOFF_MILLIS, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载，如果失败则会尝试重试
     *
     * @param executor           用于执行异步任务的执行器
     * @param httpExecutor       Http执行器
     * @param request            请求信息
     * @param range              分片信息
     * @param saveDir            保存下载文件的目录
     * @param filename           下载文件的文件名
     * @param rangeSize          分片大小
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     * @return 下载完成后的文件实例
     */
    public File downloadRetryIfFail(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, Range range, String saveDir, String filename, long rangeSize, int maxConcurrentCount, long retryBackoffMillis, int maxRetryCount) {
        // 创建分片文件信息类
        File targetFile = getTargetFile(saveDir, range.getFilename(), filename);
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(targetFile);

        // 索引文件还未创建时
        if (shardingFileIndex.indexNotCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        } else if (shardingFileIndex.downloadInfoInconsistent(range)) {
            // 续传信息与本次下载任务不一致时（目标文件被删除或者服务器资源已变化），重建索引
            rebuildIndexFiles(shardingFileIndex, range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                retryBackoff(shardingFileIndex, i, retryBackoffMillis, maxRetryCount);
            }

            // 执行分片异步下载
            rangeFileDownload(executor, httpExecutor, request, shardingFileIndex, maxConcurrentCount);
            i++;
        }
        return targetFile;

    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 【正常流程】分片文件下载
     *
     * @param executor          自定义线程池
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(AsyncTaskExecutor executor, Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload(executor, null, request, shardingFileIndex);
    }


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        rangeFileDownload(executor, httpExecutor, request, shardingFileIndex, DEFAULT_MAX_CONCURRENT_COUNT);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务</b><br/>
     * 分片文件下载
     *
     * @param executor           自定义线程池
     * @param httpExecutor       Http执行器
     * @param request            请求实例
     * @param shardingFileIndex  分片文件信息
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    public void rangeFileDownload(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex, int maxConcurrentCount) {
        if (maxConcurrentCount <= 0) {
            throw new RangeDownloadException("The maxConcurrentCount must be greater than 0, but it is {}", maxConcurrentCount).error(log);
        }

        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务（限制同时在途的任务数量，避免一次性提交过多的异步任务）
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(executor.supplyAsync(() -> downloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index)));
            awaitBatchCompletion(processedResult, maxConcurrentCount);
        }

        // 处理写入结果
        writerResultHandler(shardingFileIndex, unprocessedIndexes, processedResult);
    }


    /**
     * 写入结果处理
     *
     * @param shardingFileIndex  分片文件信息
     * @param unprocessedIndexes 未处理的索引信息
     * @param processedResult    未处理的索引信息对应的处理结果
     */
    private void writerResultHandler(ShardingFileIndex shardingFileIndex, List<Range.Index> unprocessedIndexes, List<Future<Range.WriterResult>> processedResult) {
        // 分析异步任务的执行结果，写入成功后删除对应的索引文件
        boolean allSuccess = true;
        for (int i = 0; i < unprocessedIndexes.size(); i++) {
            Range.WriterResult finalWriterResult = getFinalWriterResult(processedResult.get(i), unprocessedIndexes.get(i));
            // 校验结果，是否存在失败
            if (finalWriterResult.fail()) {
                allSuccess = false;
            }
        }

        // 如果全部成功，则删除索引文件夹
        if (allSuccess) {
            shardingFileIndex.clearFile();
        }
    }


    //---------------------------------------------------------------------------------------------------------
    //                                         Private Method
    //---------------------------------------------------------------------------------------------------------

    /**
     * 获取目标文件
     *
     * @param saveDir    保存目标文件的文件夹路径
     * @param sourceName 文件的原始名称
     * @param configName 用户传入的文件名称
     * @return 目标文件的文件对象
     */
    private File getTargetFile(String saveDir, String sourceName, String configName) {
        return new File(saveDir, FileUtils.getFileName(configName, sourceName));
    }

    /**
     * 获取最终的写入结果
     *
     * @param writerResultFuture 包含写入结果的Future对象
     * @param index              索引信息
     * @return 最终的写入结果
     */
    private Range.WriterResult getFinalWriterResult(Future<Range.WriterResult> writerResultFuture, Range.Index index) {
        try {
            return writerResultFuture.get();
        } catch (Throwable e) {
            log.warn("[❌] Failed to obtain the download result of the fragmented file (Range: bytes={}-{}) . Nested exception is: [{}]-{}", index.getBegin(), index.getEnd(), e, e.getMessage());
            return FAIL;
        }
    }

    /**
     * 重试前的处理：校验重试次数是否达到上限、等待退避时间
     *
     * @param shardingFileIndex  分片文件信息
     * @param retryNum           当前是第几次重试
     * @param retryBackoffMillis 重试前的等待时间（毫秒）
     * @param maxRetryCount      最大重试次数，小于0时表示不限制重试次数
     */
    private void retryBackoff(ShardingFileIndex shardingFileIndex, int retryNum, long retryBackoffMillis, int maxRetryCount) {
        if (maxRetryCount > 0 && retryNum > maxRetryCount) {
            throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit {}!", maxRetryCount).error(log);
        }
        log.info("[🔄] There are unprocessed index files in the index directory [{}], and the {} retry will be initiated", shardingFileIndex.getIndexDir().getAbsolutePath(), retryNum);
        if (retryBackoffMillis > 0) {
            try {
                Thread.sleep(retryBackoffMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RangeDownloadException(e, "Interrupted while waiting to retry the fragmented file download").error(log);
            }
        }
    }

    /**
     * 达到并发上限时等待当前批次的任务执行完成，用于限制同时在途的异步任务数量
     *
     * @param processedResult    已提交任务的执行结果
     * @param maxConcurrentCount 同时进行下载的最大分片数量
     */
    private void awaitBatchCompletion(List<Future<Range.WriterResult>> processedResult, int maxConcurrentCount) {
        int size = processedResult.size();
        if (size == 0 || size % maxConcurrentCount != 0) {
            return;
        }
        for (int i = size - maxConcurrentCount; i < size; i++) {
            try {
                processedResult.get(i).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RangeDownloadException(e, "Interrupted while waiting for the shard download tasks to complete").error(log);
            } catch (Throwable ignored) {
                // 忽略异常，执行结果统一由 writerResultHandler 处理
            }
        }
    }

    /**
     * 重建索引文件：清空原有的索引信息，根据本次下载任务重新创建索引文件
     *
     * @param shardingFileIndex 分片文件信息
     * @param range             分片对象
     * @param rangeSize         分片大小
     */
    private void rebuildIndexFiles(ShardingFileIndex shardingFileIndex, Range range, long rangeSize) {
        log.warn("[⚠️] The download information is inconsistent with the current task, the index will be rebuilt. IndexDir: {}", shardingFileIndex.getIndexDir().getAbsolutePath());
        shardingFileIndex.clearFile();
        shardingFileIndex.createIndexFiles(range, rangeSize);
    }


    private String getTempDir() {
        return FileUtils.getLuckyTempDir("RangeDownloadApi");
    }
}
