package com.luckyframework.httpclient.generalapi.file;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;
import com.luckyframework.httpclient.proxy.annotations.DownloadToLocal;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.httpclient.proxy.annotations.SSL;
import com.luckyframework.httpclient.proxy.annotations.Timeout;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.Param;

import java.io.File;
import java.io.InputStream;

/**
 * 通用型文件API接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/7 09:08
 */
@SSL
@AutoRedirect
@Timeout(readTimeout = 60000)
@Retryable(retryCount = 5)
public interface FileApi {



    //-----------------------------------------------------------------------------------------
    //                              文件下载到本地
    //-----------------------------------------------------------------------------------------

    /**
     * 通用型文件下载方法，此方法会将下载的文件保存到指定的位置
     *
     * @param request  请求对象
     * @param saveDir  保存文件的目录
     * @param filename 保存文件的名称
     * @return 下载到本地后的文件对象
     */
    @HttpRequest
    @DownloadToLocal(saveDir = "#{saveDir}", filename = "#{filename}", useOriginalFileName = true)
    File download(Request request, @Param("saveDir") String saveDir, @Param("filename") String filename);

    /**
     * 使用GET的请求方式从网络上获取文件并下载到本地
     *
     * @param url      下载文件的URL
     * @param saveDir  保存文件的目录
     * @param filename 保存文件的名称
     * @return 下载到本地后的文件对象
     */
    default File download(String url, String saveDir, String filename) {
        return download(Request.get(url), saveDir, filename);
    }

    /**
     * 使用GET的请求方式从网络上获取文件并下载到本地
     *
     * @param url     下载文件的URL
     * @param saveDir 保存文件的目录
     * @return 下载到本地后的文件对象
     */
    default File download(String url, String saveDir) {
        return download(url, saveDir, "");
    }

    /**
     * 使用GET的请求方式从网络上获取文件并下载到系统临时文件夹
     *
     * @param url 下载文件的URL
     * @return 下载到本地后的文件对象
     */
    default File download(String url) {
        return download(url, FileUtils.getLuckyTempDir("FileApi"));
    }

    //-----------------------------------------------------------------------------------------
    //                              获取文件流
    //-----------------------------------------------------------------------------------------

    /**
     * 通用型文件下载方法，此方法会将下载的文件保存到内存中并返回一个{@link MultipartFile}实例
     *
     * @param request 请求对象
     * @return 文件对应的MultipartFile对象
     */
    @HttpRequest
    MultipartFile getFile(Request request);

    /**
     * 使用GET的请求方式下载的文件保存到内存中并返回一个{@link MultipartFile}实例
     *
     * @param url 下载文件的URL
     * @return 文件对应的MultipartFile对象
     */
    default MultipartFile getFile(String url) {
        return getFile(Request.get(url));
    }

    /**
     * 通用型文件下载方法，此方法会将下载的文件保存到内存中并返回一个{@link InputStream}实例
     *
     * @param request 请求对象
     * @return 文件对应的InputStream对象
     */
    @HttpRequest
    InputStream getInputStream(Request request);

    /**
     * 使用GET的请求方式下载的文件保存到内存中并返回一个{@link InputStream}实例
     *
     * @param url 下载文件的URL
     * @return 文件对应的InputStream对象
     */
    default InputStream getInputStream(String url) {
        return getInputStream(Request.get(url));
    }

    /**
     * 通用型文件下载方法，此方法会将下载的文件保存到内存中并返回一个{@link byte[]}实例
     *
     * @param request 请求对象
     * @return 文件对应的byte[]对象
     */
    @HttpRequest
    byte[] getByteArray(Request request);

    /**
     * 使用GET的请求方式下载的文件保存到内存中并返回一个{@link byte[]}实例
     *
     * @param url 下载文件的URL
     * @return 文件对应的byte[]对象
     */
    default byte[] getByteArray(String url) {
        return getByteArray(Request.get(url));
    }

}
