package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.DownloadToLocal;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.io.MultipartFile;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;

import static org.springframework.util.StreamUtils.BUFFER_SIZE;

/**
 * 流式文件下载处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/31 00:00
 */
public class FileDownloadResultConvert implements ResponseConvert {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        int status = response.getStatus();
        DownloadToLocal ann = context.toAnnotation(DownloadToLocal.class);
        if (ContainerUtils.notInArrays(ConversionUtils.conversion(ann.normalStatus(), Integer[].class), status)) {
            throw new FileDownloadException("File download failed, the interface response code is {}", status);
        }
        String saveDir = context.parseExpression(ann.saveDir());
        if (!StringUtils.hasText(saveDir)) {
            throw new FileDownloadException("File download failed, {}({}) attribute must be set using @DownloadToLocal annotation", ann.saveDir(), saveDir);
        }

        // 重响应体中获取文件
        MultipartFile file = response.getMultipartFile();

        // 获取文件名称
        String configName = context.parseExpression(ann.filename());
        if (StringUtils.hasText(configName)) {
            file.setFileName(configName);
        }

        // 获取进度监控器
        ProgressMonitor progressMonitor = findProgressMonitor(context.getContext(), ann);

        File saveFile = new File(saveDir, file.getFileName());

        // 下载文件，如果过程中出现异常，则删除不完整的文件
        try {
            // 直接下载
            if (progressMonitor == null) {
                file.copyToFolder(saveDir);
            }
            // 监控模式下载
            else {
                progressMonitorCopy(response, saveFile, progressMonitor, ann.frequency());
            }
        } catch (Exception e) {
            saveFile.delete();
            throw new FileDownloadException(e, "File download failed, an exception occurred while writing a file to a local disk: {}", saveFile.getAbsolutePath());
        }


        // 封装返回值
        MethodContext methodContext = context.getContext();
        if (methodContext.isVoidMethod()) {
            return null;
        }
        Type returnType = methodContext.getRealMethodReturnType();

        // Boolean类型返回值时返回true
        if (returnType == Boolean.class || returnType == boolean.class) {
            return (T) Boolean.TRUE;
        }
        // File类型返回值时返回文件对象
        if (returnType == File.class) {
            return (T) saveFile;
        }
        // Long类返回值时返回文件大小
        if (returnType == Long.class || returnType == long.class) {
            return (T) Long.valueOf(saveFile.length());
        }
        // String类型返回值文件的绝对路径
        if (returnType == String.class) {
            return (T) saveFile.getAbsolutePath();
        }
        // InputStream类型返回值时返回对应的文件输入流
        if (returnType == InputStream.class) {
            return (T) Files.newInputStream(saveFile.toPath());
        }
        throw new FileDownloadException("@DownloadToLocal annotation unsupported method return value type: {}", returnType);
    }

    private ProgressMonitor findProgressMonitor(MethodContext context, DownloadToLocal ann) {
        // 优先使用参数列表中的进度监控器
        Object[] arguments = context.getArguments();
        for (Object arg : arguments) {
            if (arg instanceof ProgressMonitor) {
                return (ProgressMonitor) arg;
            }
        }
        // 参数列表中没有则尝试从注解中获取
        ObjectGenerate generate = ann.monitor();
        if (generate.clazz() != ProgressMonitor.class) {
            return context.generateObject(generate);
        }

        return null;
    }

    private void progressMonitorCopy(Response response, File file, ProgressMonitor monitor, int frequency) throws Exception {
        InputStream in = response.getInputStream();
        OutputStream out = Files.newOutputStream(file.toPath());

        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");

        Progress progress = new Progress(response.getResponseMetaData(), file.getAbsolutePath());
        monitor.beforeBeginning(progress);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            progress.start();
            int i = 1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                progress.complete(bytesRead);
                if (i % frequency == 0) {
                    monitor.sniffing(progress);
                }
                i++;
            }
            monitor.sniffing(progress);
            out.flush();
            progress.end();
            monitor.afterCompleted(progress);
        } catch (Exception e) {
            monitor.afterFailed(progress, e);
        } finally {
            close(in);
            close(out);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            // ignore
        }
    }

}
