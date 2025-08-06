package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.DownloadToLocal;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.io.ProgressMonitor;
import org.springframework.lang.Nullable;

import java.io.File;

/**
 * 流式文件下载处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/31 00:00
 */
public class FileDownloadResultConvert extends AbstractConditionalSelectionResponseConvert {

    @Override
    @SuppressWarnings("all")
    public <T> T doConvert(Response response, ConvertContext context) throws Throwable {

        DownloadToLocal ann = context.toAnnotation(DownloadToLocal.class);
        String saveDir = context.parseExpression(ann.saveDir());
        if (!StringUtils.hasText(saveDir)) {
            saveDir = FileUtils.getLuckyTempDir("@DownloadToLocal");
        }

        // 类型检验
        FileTypeConvertFunction.convertTypeCheck(context.getContext(), "@DownloadToLocal annotation unsupported method return value type: {}");

        // 重响应体中获取文件
        MultipartFile file = response.getMultipartFile();

        // 获取文件名称
        String configName = context.parseExpression(ann.filename());
        if (StringUtils.hasText(configName)) {
            file.setFileName(configName);
        } else if (!ann.useRandomFileName()){
            file.setFileName(file.getOriginalFileName());
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
                file.progressMonitorCopy(saveDir, progressMonitor, ann.frequency());
            }
        } catch (Exception e) {
            saveFile.delete();
            throw new FileDownloadException(e, "File download failed, an exception occurred while writing a file to a local disk: {}", saveFile.getAbsolutePath());
        }

        // 文件类型转方法返回值类型
        return (T) new FileTypeConvertFunction(context.getContext()).apply(saveFile);
    }

    /**
     * 查找进度监控器实例
     *
     * @param context 上下文对象
     * @param ann     注解实例
     * @return 进度监控器
     */
    @Nullable
    private ProgressMonitor findProgressMonitor(MethodContext context, DownloadToLocal ann) {
        // 优先使用参数列表中的进度监控器
        Object[] arguments = context.getArguments();
        for (Object arg : arguments) {
            if (arg instanceof ProgressMonitor) {
                return (ProgressMonitor) arg;
            }
        }
        // 参数列表中没有则尝试从注解中获取
        try {
            return context.generateObject(ann.monitor(), ann.monitorClass(), ProgressMonitor.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
