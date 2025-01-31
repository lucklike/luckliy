package com.luckyframework.httpclient.generalapi.download;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.FileTypeConvertFunction;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.annotation.AliasFor;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.generalapi.download.RangeDownloadApi.DEFAULT_RANGE_SIZE;

/**
 * 分片文件下载
 * 负责将文件下载到本地的注解<br/>
 * <pre>
 *     使用该注解的方法的返回值必须是以下类型：
 *     1.{@link Boolean}或{@link boolean}(是否下载成功)
 *     2.{@link String }(下载到磁盘上的文件路径)
 *     3.{@link Long }或{@link long}(下载到磁盘上的文件的大小)
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
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    @AliasFor("saveDir") String value() default "";

    /**
     * 保存下载文件的位置，支持SpEL表达式
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
     */
    @AliasFor("value") String saveDir() default "";

    /**
     * 文件名，支持SpEL表达式
     * <pre>
     * SpEL表达式内置参数有：
     * root: {
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *
     *      <b>Request : </b>
     *      {@value TAG#REQUEST}
     *      {@value TAG#REQUEST_URL}
     *      {@value TAG#REQUEST_METHOD}
     *      {@value TAG#REQUEST_QUERY}
     *      {@value TAG#REQUEST_PATH}
     *      {@value TAG#REQUEST_FORM}
     *      {@value TAG#REQUEST_HEADER}
     *      {@value TAG#REQUEST_COOKIE}
     *
     *      <b>Response : </b>
     *      {@value TAG#RESPONSE}
     *      {@value TAG#RESPONSE_STATUS}
     *      {@value TAG#CONTENT_LENGTH}
     *      {@value TAG#CONTENT_TYPE}
     *      {@value TAG#RESPONSE_HEADER}
     *      {@value TAG#RESPONSE_COOKIE}
     *      {@value TAG#RESPONSE_BODY}
     * }
     * </pre>
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
     * 最大重试次数，小于0时表示无限重试直到成功
     */
    int maxRetryCount() default -1;

    /**
     * 分片下载函数
     */
    class RangeDownloadFunction {

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
            String saveDir = rangeDownloadAnn.saveDir();
            if (!StringUtils.hasText(saveDir)) {
                saveDir = FileUtils.getLuckyTempDir("@RangeDownload");
            }

            HttpClientProxyObjectFactory proxyFactory = context.getHttpProxyFactory();
            RangeDownloadApi downloadApi = proxyFactory.getProxyObject(rangeDownloadAnn.implClass());
            File downloadFile;

            String filename = rangeDownloadAnn.filename();
            filename = context.parseExpression(filename, String.class);

            // 支持分片下载
            if (downloadApi.isSupport(request)) {
                downloadFile = downloadApi.downloadRetryIfFail(
                        context.getExecutor(),
                        request,
                        saveDir,
                        filename,
                        rangeDownloadAnn.rangeSize(),
                        rangeDownloadAnn.maxRetryCount()
                );
            }
            // 不支持分片下载
            else {
                downloadFile = downloadApi.download(request, saveDir, filename);
            }

            // 文件类型转方法返回值类型
            return new FileTypeConvertFunction(context).apply(downloadFile);
        }

    }
}

