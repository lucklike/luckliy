package com.luckyframework.httpclient.generalapi.download;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.FileTypeConvertFunction;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AliasFor;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.generalapi.download.RangeDownloadApi.DEFAULT_MAX_CONCURRENT_COUNT;
import static com.luckyframework.httpclient.generalapi.download.RangeDownloadApi.DEFAULT_RANGE_SIZE;
import static com.luckyframework.httpclient.generalapi.download.RangeDownloadApi.DEFAULT_RETRY_BACKOFF_MILLIS;

/**
 * 分片文件下载
 * 负责将文件下载到本地的注解<br/>
 * <pre>
 *     使用该注解的方法的返回值必须是以下类型：
 *     1.{@link Boolean}或{@code boolean}(是否下载成功)
 *     2.{@link String }(下载到磁盘上的文件路径)
 *     3.{@link Long }或{@code long}(下载到磁盘上的文件的大小)
 *     4.{@link File}(下载到磁盘上的文件)
 *     5.{@link InputStream}(下载到磁盘上的文件流)
 *     6.{@link MultipartFile}(下载到磁盘上的文件对应的MultipartFile对象)
 *     7.<b>void</b>
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/23 02:09
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpELImport(RangeDownload.RangeDownloadFunction.class)
@Wrapper(fun = "__range_download__", waitReqCreatComplete = true)
public @interface RangeDownload {

    /**
     * 保存下载文件的位置，支持SpEL表达式
     *
     * @see SpELVariableNote
     */
    @AliasFor("saveDir")
    String value() default "";

    /**
     * 保存下载文件的位置，支持SpEL表达式
     * @see SpELVariableNote
     */
    @AliasFor("value") String saveDir() default "";

    /**
     * 文件名，支持SpEL表达式
     * <pre>
     * 支持占位符：
     *  {@code {_name_}}  : 表示原始文件名
     *  {@code {.ext}}    : 表示原始文件的后缀名
     *  例如：
     *      file_name: lucky_httpclient_test.json
     *      {@code {_name_}} -> lucky_httpclient_test
     *      {@code {.ext}}   -> .json
     *  </pre>
     *
     * @see SpELVariableNote
     */
    String filename() default "";

    /**
     * 用于实现分片下载的实现类Class
     */
    Class<? extends RangeDownloadApi> implClass() default RangeDownloadApi.class;

    /**
     * 分片大小
     */
    long rangeSize() default DEFAULT_RANGE_SIZE;

    /**
     * 分片下载的最大并发数量
     */
    int maxConcurrentCount() default DEFAULT_MAX_CONCURRENT_COUNT;

    /**
     * 分片下载失败后重试前的等待时间（毫秒）
     */
    long retryBackoffMillis() default DEFAULT_RETRY_BACKOFF_MILLIS;

    /**
     * 最大重试次数，小于0时表示无限重试直到成功
     */
    int maxRetryCount() default -1;

    /**
     * 分片下载函数
     */
    class RangeDownloadFunction {

        private static final Logger log = LoggerFactory.getLogger(RangeDownloadFunction.class);

        /**
         * 文件下载，下载前会去检测当前的下载资源是否支持分片下载功能，如果支持
         * 则使用分片下载，否则使用普通下载
         *
         * @param context 方法上下文对象
         * @param request 当前请求体
         * @return 符合方法返回值类型的结果
         */
        @FunctionAlias("__range_download__")
        public static Object download(MethodContext context, Request request) {

            // 类型检验
            FileTypeConvertFunction.convertTypeCheck(context, "@RangeDownload annotation unsupported method return value type: {}");

            RangeDownload rangeDownloadAnn = context.getMergedAnnotation(RangeDownload.class);
            String saveDir = context.parseExpression(rangeDownloadAnn.saveDir(), String.class);
            if (!StringUtils.hasText(saveDir)) {
                saveDir = FileUtils.getLuckyTempDir("@RangeDownload");
            }

            HttpClientProxyObjectFactory proxyFactory = context.getHttpProxyFactory();
            RangeDownloadApi downloadApi = proxyFactory.getProxyObject(rangeDownloadAnn.implClass());
            File downloadFile;

            String filename = context.parseExpression(rangeDownloadAnn.filename(), String.class);

            // 获取 Http 执行器
            HttpExecutor httpExecutor = context.getHttpExecutor();

            // 检测资源是否支持分片下载（探测失败时降级为普通下载）
            Range range;
            try {
                range = downloadApi.rangeInfo(httpExecutor, request.change(RequestMethod.HEAD));
            } catch (Exception e) {
                log.warn("[⚠️] Failed to detect whether the resource supports range download, fallback to normal download. Error: {}", e.getMessage());
                range = Range.notSupport();
            }
            long rangeSize = rangeDownloadAnn.rangeSize();
            if (range.isSupport() && range.getLength() > rangeSize) {
                downloadFile = downloadApi.downloadRetryIfFail(
                        context.getAsyncTaskExecutor(),
                        httpExecutor,
                        request,
                        range,
                        saveDir,
                        filename,
                        rangeDownloadAnn.rangeSize(),
                        rangeDownloadAnn.maxConcurrentCount(),
                        rangeDownloadAnn.retryBackoffMillis(),
                        rangeDownloadAnn.maxRetryCount()
                );
            }
            // 不支持分片下载
            else {
                downloadFile = downloadApi.download(httpExecutor, request, saveDir, filename);
            }

            // 文件类型转方法返回值类型
            return new FileTypeConvertFunction(context).apply(downloadFile);
        }

    }
}

