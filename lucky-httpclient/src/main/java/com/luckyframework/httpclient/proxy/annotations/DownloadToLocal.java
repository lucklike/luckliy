package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.convert.FileDownloadResultConvert;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.io.ProgressMonitor;
import org.springframework.core.annotation.AliasFor;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
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
 *
 *     可以使用的特殊参数
 *     {@link ProgressMonitor}：方法参数中可以使用该参数，用于获取下载进度
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 * @see FileDownloadResultConvert
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ResultConvertMeta(convert = @ObjectGenerate(FileDownloadResultConvert.class))
public @interface DownloadToLocal {

    /**
     * 保存下载文件的位置，支持SpEL表达式，不配置时默认保存到系统临时文件夹下: ${java.io.tmpdir}/Lucky/@DownloadToLocal/yyyyMMdd/
     *
     * @see SpELVariableNote
     */
    @AliasFor("saveDir")
    String value() default "";

    /**
     * 保存下载文件的位置，支持SpEL表达式，不配置时默认保存到系统临时文件夹下:  ${java.io.tmpdir}/Lucky/@DownloadToLocal/yyyyMMdd/
     *
     * @see SpELVariableNote
     */
    @AliasFor("value")
    String saveDir() default "";

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
     * </pre>
     *
     * @see SpELVariableNote
     */
    String filename() default "";

    /**
     * 是否使用原始文件名+随机值的命名方式
     */
    boolean useRandomFileName() default false;

    /**
     * 进度监控器{@link ProgressMonitor}生成器
     */
    ObjectGenerate monitor() default @ObjectGenerate(ProgressMonitor.class);

    /**
     * 进度监控器{@link ProgressMonitor}Class
     */
    Class<? extends ProgressMonitor> monitorClass() default ProgressMonitor.class;

    /**
     * 嗅探频率，每拷贝的字节数为4096b，拷贝n次之后进行一次嗅探，默认100kb嗅探一次
     */
    int frequency() default 25;
}
