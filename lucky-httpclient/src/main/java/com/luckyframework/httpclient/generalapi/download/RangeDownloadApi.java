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
import org.springframework.util.FileCopyUtils;

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
    @Condition(assertion = "#{$status$ != 200 and $status$ !=206}", exception = "Response to the shard file [<status=#{$status$}> #{index.begin}-#{index.end}] download is error. ")
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
    @Condition(assertion = "#{$status$ != 200 and $status$ !=206}", exception = "Response to the shard file [<status=#{$status$}> #{index.begin}-#{index.end}] download is error. ")
    public abstract Range.WriterResult downloadRangeFile(HttpExecutor httpExecutor, Request request, @Param("shardingFileInfo") ShardingFileIndex shardingFileIndex, @Param("index") Range.Index index);


    //---------------------------------------------------------------------------------------------------------
    //                                        Writer Data To File
    //---------------------------------------------------------------------------------------------------------

    /**
     * 将分片数据写入文件（零拷贝优化版本）
     */
    public Range.WriterResult writeDataToFile(ShardingFileIndex shardingFileIndex,
                                              InputStream dataStream, Range.Index index) {
        File targetFile = shardingFileIndex.getTargetFile();

        // 使用 try-with-resources 确保资源正确关闭
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw")) {
            randomAccessFile.seek(index.getBegin());
            randomAccessFile.write(FileCopyUtils.copyToByteArray(dataStream));

            // 记录成功索引
            shardingFileIndex.recordSuccessIndex(index);

            log.debug("[✅] Sharding file (Range: bytes={}-{}) downloaded and written to {}, Number of bytes written: {}",
                    index.getBegin(), index.getEnd(), targetFile.getAbsolutePath(), index.getEnd() - index.getBegin());
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
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(httpExecutor, request, saveDir, range, filename, rangeSize, maxRetryCount);
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

        // 创建分片文件信息类
        File targetFile = getTargetFile(saveDir, range.getFilename(), filename);
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(targetFile);

        // 索引文件还未创建时
        if (!shardingFileIndex.indexCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                if (maxRetryCount > 0 && i >= maxRetryCount) {
                    throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit {}!", maxRetryCount).error(log);
                }
                log.info("[🔄] There are unprocessed index files in the index directory [{}], and the {} retry will be initiated", shardingFileIndex.getIndexDir().getAbsolutePath(), i);
            }

            // 执行分片异步下载
            rangeFileDownload(httpExecutor, request, shardingFileIndex);
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
        doRangeFileDownload(httpExecutor, request, shardingFileIndex);
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
        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(asyncDownloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index));
        }

        // 处理写入结果
        writerResultHandler(shardingFileIndex, unprocessedIndexes, processedResult);
    }


    //---------------------------------------------------------------------------------------------------------
    //                               DownloadRangeFile + Executor
    //---------------------------------------------------------------------------------------------------------

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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

        // 创建分片文件信息类
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(getTargetFile(saveDir, range.getFilename(), filename));

        // 索引文件还未创建时
        if (!shardingFileIndex.indexCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                if (maxRetryCount > 0 && i >= maxRetryCount) {
                    throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit {}!", maxRetryCount).error(log);
                }
                log.info("[🔄] There are unprocessed index files in the index directory [{}], and the {} retry will be initiated", shardingFileIndex.getIndexDir().getAbsolutePath(), i);
            }

            // 执行分片异步下载
            rangeFileDownload(executor, httpExecutor, request, shardingFileIndex);
            i++;
        }
        return shardingFileIndex.getTargetFile();
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
     * 【正常流程】分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求信息
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        doRangeFileDownload(executor, httpExecutor, request, shardingFileIndex);
    }

    /**
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link Executor}执行异步分片下载任务<b/><br/>
     * 分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void doRangeFileDownload(Executor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(CompletableFuture.supplyAsync(() -> downloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index), executor));
        }

        // 处理写入结果
        writerResultHandler(shardingFileIndex, unprocessedIndexes, processedResult);
    }

    //---------------------------------------------------------------------------------------------------------
    //                            DownloadRangeFile + AsyncTaskExecutor
    //---------------------------------------------------------------------------------------------------------


    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
        // 检测是否支持分片信息
        Range range = rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
        if (!range.isSupport()) {
            throw new RangeDownloadException("not support range download: {}", request).error(log);
        }
        return downloadRetryIfFail(executor, httpExecutor, request, range, saveDir, filename, rangeSize, maxRetryCount);
    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
        // 创建分片文件信息类
        File targetFile = getTargetFile(saveDir, range.getFilename(), filename);
        ShardingFileIndex shardingFileIndex = new ShardingFileIndex(targetFile);

        // 索引文件还未创建时
        if (!shardingFileIndex.indexCreatedCompleted()) {
            shardingFileIndex.createIndexFiles(range, rangeSize);
        }

        int i = 0;
        while (shardingFileIndex.infoFileDirIsExists()) {
            if (i != 0) {
                if (maxRetryCount > 0 && i >= maxRetryCount) {
                    throw new RangeDownloadException("Failed to download fragmented files: The number of retries exceeds the upper limit {}!", maxRetryCount).error(log);
                }
                log.info("[🔄] There are unprocessed index files in the index directory [{}], and the {} retry will be initiated", shardingFileIndex.getIndexDir().getAbsolutePath(), i);
            }

            // 执行分片异步下载
            rangeFileDownload(executor, httpExecutor, request, shardingFileIndex);
            i++;
        }
        return targetFile;

    }

    /**
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
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
     * <b>使用自定义线程池{@link AsyncTaskExecutor}执行异步分片下载任务<b/><br/>
     * 分片文件下载
     *
     * @param executor          自定义线程池
     * @param httpExecutor      Http执行器
     * @param request           请求实例
     * @param shardingFileIndex 分片文件信息
     */
    public void rangeFileDownload(AsyncTaskExecutor executor, HttpExecutor httpExecutor, Request request, ShardingFileIndex shardingFileIndex) {
        // 获取未完成的索引文件信息
        List<Range.Index> unprocessedIndexes = shardingFileIndex.getUnprocessedIndexes();

        // 提交异步任务
        List<Future<Range.WriterResult>> processedResult = new ArrayList<>(unprocessedIndexes.size());
        for (Range.Index index : unprocessedIndexes) {
            processedResult.add(executor.supplyAsync(() -> downloadRangeFile(httpExecutor, request.copy(), shardingFileIndex, index)));
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


    private String getTempDir() {
        return FileUtils.getLuckyTempDir("RangeDownloadApi");
    }
}
