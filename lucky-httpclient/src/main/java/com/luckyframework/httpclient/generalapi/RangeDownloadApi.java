package com.luckyframework.httpclient.generalapi;

import com.luckyframework.async.EnhanceFuture;
import com.luckyframework.async.EnhanceFutureFactory;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Branch;
import com.luckyframework.httpclient.proxy.annotations.BrowserFeign;
import com.luckyframework.httpclient.proxy.annotations.ConditionalSelection;
import com.luckyframework.httpclient.proxy.annotations.DownloadToLocal;
import com.luckyframework.httpclient.proxy.annotations.Head;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.io.FileUtils;
import com.luckyframework.reflect.Param;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static org.springframework.util.StreamUtils.BUFFER_SIZE;

/**
 * 分片下载API
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 14:00
 */
@BrowserFeign
@SpELImport(fun = RangeInfo.class)
@Retryable(retryCount = 5, waitMillis = 2000L)
public interface RangeDownloadApi extends FileApi {

    /**
     * 默认的分片大小
     */
    long DEFAULT_RANGE_SIZE = 1024 * 1024 * 5;

    /**
     * 异步分片文件下载
     *
     * @param request  请求对象
     * @param begin    开始位置
     * @param end      结束位置
     * @param saveDir  保存下载文件的目录
     * @param filename 文件名
     * @return Future<File>对象
     */
    @HttpRequest
    @StaticHeader("[SET]Range: bytes=#{begin}-#{end}")
    @DownloadToLocal(saveDir = "#{saveDir}", filename = "#{filename}", normalStatus = 206)
    Future<File> asyncRangeFileDownload(Request request,
                                        @Param("begin") long begin,
                                        @Param("end") long end,
                                        @Param("saveDir") String saveDir,
                                        @Param("filename") String filename);


    /**
     * 文件分片下载
     *
     * @param request  请求对象
     * @param begin    开始位置
     * @param end      结束位置
     * @param saveDir  保存下载文件的目录
     * @param filename 文件名
     * @return Future<File>对象
     */
    @HttpRequest
    @StaticHeader("[SET]Range: bytes=#{begin}-#{end}")
    @DownloadToLocal(saveDir = "#{saveDir}", filename = "#{filename}", normalStatus = 206)
    File rangeFileDownload(Request request,
                           @Param("begin") long begin,
                           @Param("end") long end,
                           @Param("saveDir") String saveDir,
                           @Param("filename") String filename);


    /**
     * 分片下载测试，检测该Url是否支持分片下载
     * <pre>
     *     1.向请求地址发送一个带Range请求头的Head请求
     *     2.如果返回的状态码为206，则说明该地址支持分片下载，否则不支持
     * </pre>
     *
     * @param request 请求对象
     * @return 分片信息
     */
    @Head
    @StaticHeader("Range: bytes=0-1")
    @ConditionalSelection(
            branch = @Branch(assertion = "#{$status$ == 206}", result = "#{#create($resp$)}"),
            defaultValue = "#{#notSupport()}"
    )
    RangeInfo rangeInfo(Request request);


    //---------------------------------------------------------------------------
    //                          default methods
    //---------------------------------------------------------------------------

    /**
     * <b>使用{@link HttpClientProxyObjectFactory#getAsyncExecutor(MethodContext)}中的线程池执行分片下载任务<b/><br/>
     * 【GET】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param url       请求地址
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(String url, String saveDir, long rangeSize) throws Exception {
        return rangeFileDownload(Request.get(url), saveDir, rangeSize);
    }

    /**
     * <b>使用{@link HttpClientProxyObjectFactory#getAsyncExecutor(MethodContext)}中的线程池执行分片下载任务<b/><br/>
     * 【GET，默认分片大小为5M】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param url     请求地址
     * @param saveDir 保存下载文件的目录
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(String url, String saveDir) throws Exception {
        return rangeFileDownload(url, saveDir, DEFAULT_RANGE_SIZE);
    }


    /**
     * <b>使用{@link HttpClientProxyObjectFactory#getAsyncExecutor(MethodContext)}中的线程池执行分片下载任务<b/><br/>
     * 使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param request   请求对象
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(Request request, String saveDir, long rangeSize) throws Exception {
        RangeInfo rangeInfo = rangeInfo(request.change(RequestMethod.HEAD));
        if (!rangeInfo.isSupport()) {
            throw new LuckyRuntimeException("not support range download: {}", request);
        }
        final long length = rangeInfo.getLength();
        long begin = 0;
        String rangeFolder = StringUtils.format("{}/range-{}", saveDir, NanoIdUtils.randomNanoId(5));
        List<Future<File>> futureList = new ArrayList<>();
        while (begin < length) {
            long end = begin + rangeSize;
            String filename = StringUtils.format("({}-{})_{}", begin, end, NanoIdUtils.randomNanoId(5));
            futureList.add(this.asyncRangeFileDownload(request.copy(), begin, end, rangeFolder, filename));
            begin = end + 1;
        }
        return fileMerge(futureList, new File(saveDir, rangeInfo.getFilename()), rangeFolder);
    }


    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param url                  请求地址
     * @param saveDir              保存下载文件的目录
     * @param rangeSize            分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir, long rangeSize) throws Exception {
        return rangeFileDownload(enhanceFutureFactory, Request.get(url), saveDir, rangeSize);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 【GET，默认分片大小为5M】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param url     请求地址
     * @param saveDir 保存下载文件的目录
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, String url, String saveDir) throws Exception {
        return rangeFileDownload(enhanceFutureFactory, url, saveDir, DEFAULT_RANGE_SIZE);
    }

    /**
     * <b>使用{@link EnhanceFutureFactory}执行异步分片下载任务<b/><br/>
     * 使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param enhanceFutureFactory EnhanceFutureFactory
     * @param request              请求对象
     * @param saveDir              保存下载文件的目录
     * @param rangeSize            分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(EnhanceFutureFactory enhanceFutureFactory, Request request, String saveDir, long rangeSize) throws Exception {
        RangeInfo rangeInfo = rangeInfo(request.change(RequestMethod.HEAD));
        if (!rangeInfo.isSupport()) {
            throw new LuckyRuntimeException("not support range download: {}", request);
        }
        final long length = rangeInfo.getLength();
        long begin = 0;
        String rangeFolder = StringUtils.format("{}/range-{}", saveDir, NanoIdUtils.randomNanoId(5));
        EnhanceFuture<File> enhanceFuture = enhanceFutureFactory.create();
        while (begin < length) {
            final long start = begin;
            final long end = begin + rangeSize;
            String filename = StringUtils.format("({}-{})_{}", begin, end, NanoIdUtils.randomNanoId(5));
            enhanceFuture.addAsyncTask(() -> this.rangeFileDownload(request.copy(), start, end, rangeFolder, filename));
            begin = end + 1;
        }
        return fileMerge(enhanceFuture.getFutures(), new File(saveDir, rangeInfo.getFilename()), rangeFolder);
    }


    /**
     * <b>使用自定义{@link Executor}执行异步分片下载任务<b/><br/>
     * 【GET，默认分片大小为5M】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param url     请求地址
     * @param saveDir 保存下载文件的目录
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(Executor Executor, String url, String saveDir) throws Exception {
        return rangeFileDownload(Executor, url, saveDir, DEFAULT_RANGE_SIZE);
    }

    /**
     * <b>使用自定义{@link Executor}执行异步分片下载任务<b/><br/>
     * 【GET】使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param executor  Executor
     * @param url       请求地址
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(Executor executor, String url, String saveDir, long rangeSize) throws Exception {
        return rangeFileDownload(executor, Request.get(url), saveDir, rangeSize);
    }

    /**
     * <b>使用自定义{@link Executor}执行异步分片下载任务<b/><br/>
     * 使用分片模式下载文件
     * <pre>
     *     1.向请求地址发送一个Range请求，检测该地址是否支持分片下载
     *     2.如果支持，则会根据文件的总大小和传入的分片大小对文件进行分片
     *     3.多线程下载分片文件，并将分片文件保存到本地
     *     4.合并分片文件，完成文件下载
     *     5.清理分片文件
     * </pre>
     *
     * @param executor  Executor
     * @param request   请求对象
     * @param saveDir   保存下载文件的目录
     * @param rangeSize 分片大小
     * @return 下载的文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File rangeFileDownload(Executor executor, Request request, String saveDir, long rangeSize) throws Exception {
        return rangeFileDownload(new EnhanceFutureFactory(executor), request, saveDir, rangeSize);
    }

    /**
     * 分片文件合并，将所有的分片文件写入到一个大文件中
     *
     * @param futureList  文件集合
     * @param targetFile  要写入的大文件
     * @param rangeFolder 分片文件所在的文件夹
     * @return 合并后的大文件
     * @throws Exception 下载过程中可能会出现的异常
     */
    default File fileMerge(Collection<Future<File>> futureList, File targetFile, String rangeFolder) throws Exception {
        FileUtils.createSaveFolder(targetFile.getParentFile());
        FileOutputStream out = new FileOutputStream(targetFile, true);
        for (Future<File> future : futureList) {
            File rFile = future.get();
            FileInputStream in = new FileInputStream(rFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            FileUtils.closeIgnoreException(in);
            Files.deleteIfExists(rFile.toPath());
        }
        FileUtils.closeIgnoreException(out);
        Files.deleteIfExists(Paths.get(rangeFolder));
        return targetFile;
    }
}
