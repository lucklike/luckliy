package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * 负责文件下载到本地的注解<br/>
 * <pre>
 *     使用改注解的方法的返回值必须是以下类型：
 *     1.{@link Boolean}或{@link boolean}
 *     2.{@link String 文件路径}
 *     3.{@link File}
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(ResultConvert.class)
@ResultConvert(convert = @ObjectGenerate(DownloadToLocal.FileDownloadConvert.class))
public @interface DownloadToLocal {

    /**
     * 保存下载文件的位置，支持SpEL表达式
     */
    @AliasFor("saveDir")
    String value() default "";

    /**
     * 保存下载文件的位置，支持SpEL表达式
     */
    @AliasFor("value")
    String saveDir() default "";

    /**
     * 文件名，支持SpEL表达式
     */
    String filename() default "";

    /**
     * 文件下载转换器
     */
    class FileDownloadConvert implements ResponseConvert {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Response response, ConvertContext context) throws Throwable {
            int status = response.getStatus();
            if (status != 200) {
                throw new FileDownloadException("File download failed, the interface response code is {}", status);
            }
            try {
                MultipartFile file = response.getMultipartFile();
                DownloadToLocal ann = context.toAnnotation(DownloadToLocal.class);
                String saveDir = context.parseExpression(ann.saveDir());
                if (!StringUtils.hasText(saveDir)) {
                    throw new FileDownloadException("File download failed, {}({}) attribute must be set using @DownloadToLocal annotation", ann.saveDir(), saveDir);
                }
                String filename = context.parseExpression(ann.filename());
                if (StringUtils.hasText(filename)) {
                    file.setFileName(filename);
                }
                file.copyToFolder(saveDir);
                Type returnType = context.getContext().getRealMethodReturnType();
                if (returnType == Boolean.class || returnType == boolean.class) {
                    return (T) Boolean.TRUE;
                }
                File saveFile = new File(new File(saveDir), file.getFileName());
                if (returnType == String.class) {
                    return (T) saveFile.getAbsolutePath();
                }
                if (returnType == File.class){
                    return (T) saveFile;
                }
                throw new FileDownloadException("Unsupported method return value type: {}", returnType);
            } catch (Exception e) {
                throw new FileDownloadException(e, "File download failed with nested exception is {}", e.toString());
            }
        }
    }
}

class FileDownloadException extends LuckyRuntimeException {

    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(Throwable ex) {
        super(ex);
    }

    public FileDownloadException(String message, Throwable ex) {
        super(message, ex);
    }

    public FileDownloadException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FileDownloadException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
