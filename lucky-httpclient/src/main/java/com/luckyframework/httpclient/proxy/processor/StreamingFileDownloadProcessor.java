package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.ResourceNameParser;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.DownloadToLocal;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.VoidResponseConvert;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 流式文件下载处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/31 00:00
 */
public class StreamingFileDownloadProcessor implements VoidResponseConvert, ResponseProcessor, ProcessorAnnContextAware {

    private ProcessorAnnContext context;


    @Override
    public void process(ResponseMetaData responseMetaData) throws Exception {

        int status = responseMetaData.getStatus();
        DownloadToLocal ann = context.toAnnotation(DownloadToLocal.class);
        if (ContainerUtils.notInArrays(ConversionUtils.conversion(ann.normalStatus(), Integer[].class), status)) {
            throw new FileDownloadException("File download failed, the interface response code is {}", status);
        }
        String saveDir = context.parseExpression(ann.saveDir());
        if (!StringUtils.hasText(saveDir)) {
            throw new FileDownloadException("File download failed, {}({}) attribute must be set using @DownloadToLocal annotation", ann.saveDir(), saveDir);
        }

        // 获取文件名称
        String configName = context.parseExpression(ann.filename());
        String resourceName = ResourceNameParser.getResourceName(responseMetaData);
        String filename;
        if (!StringUtils.hasText(configName)) {
            filename = resourceName;
        } else {
            String fileType = StringUtils.getFilenameExtension(resourceName);
            filename = StringUtils.getFilenameExtension(configName) == null ? configName + "." + fileType : configName;
        }

        // 保存文件到磁盘
        File saveFile = new File(saveDir, filename);
        try {
            File folder = saveFile.getParentFile();
            if (folder.isFile()) {
                throw new FileDownloadException("The destination of the copy must be a folder with the wrong path: " + folder.getAbsolutePath());
            }
            if (!folder.exists()) {
                folder.mkdirs();
            }
            FileCopyUtils.copy(responseMetaData.getInputStream(), Files.newOutputStream(saveFile.toPath()));
        }catch (Exception e) {
            throw new FileDownloadException(e, "File download failed, an exception occurred while writing a file to a local disk: {}", saveFile.getAbsolutePath());
        }

        // 非void方法时需要将文件信息写入到上下文中
        if (!context.getContext().isVoidMethod()) {
            MapRootParamWrapper contextVar = context.getContextVar();
            contextVar.addRootVariable(getValName(File.class.getName()), saveFile);
            contextVar.addRootVariable(getValName(InputStream.class.getName()), Files.newInputStream(saveFile.toPath()));
            contextVar.addRootVariable(getValName(String.class.getName()), saveFile.getAbsolutePath());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(VoidResponse voidResponse, ConvertContext context) throws Throwable {
        MethodContext methodContext = context.getContext();
        if (methodContext.isVoidMethod()) {
            return null;
        }
        Type returnType = methodContext.getRealMethodReturnType();
        if (returnType == Boolean.class || returnType == boolean.class) {
            return (T) Boolean.TRUE;
        }
        if (returnType == String.class || returnType == File.class || returnType == InputStream.class){
            return (T) context.getRootVar(getValName(returnType.getTypeName()));
        }
        throw new FileDownloadException("@DownloadToLocal annotation unsupported method return value type: {}", returnType);

    }

    @Override
    public void setProcessorAnnContext(ProcessorAnnContext processorAnnContext) {
        this.context = processorAnnContext;
    }

    private String getValName(String name) {
        return "$" + name.replaceAll("\\.", "_") + "$";
    }
}
