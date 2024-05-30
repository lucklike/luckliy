package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.ResourceNameParser;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.VoidResponseConvert;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.processor.ProcessorAnnContext;
import com.luckyframework.httpclient.proxy.processor.ProcessorAnnContextAware;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;

/**
 * 流式下载注解
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@VoidResultConvert(convert = @ObjectGenerate(clazz = StreamDownloadToLocal.StreamDownloadProcessor.class, scope = Scope.METHOD_CONTEXT))
@RespProcessorMeta(process = @ObjectGenerate(clazz = StreamDownloadToLocal.StreamDownloadProcessor.class, scope = Scope.METHOD_CONTEXT))
public @interface StreamDownloadToLocal {

    String saveDir();

    String filename() default "";

    class StreamDownloadProcessor implements VoidResponseConvert, ResponseProcessor, ProcessorAnnContextAware {

        private ProcessorAnnContext context;

        @Override
        public void process(ResponseMetaData responseMetaData) throws Exception {
            StreamDownloadToLocal downloadAnn = context.toAnnotation(StreamDownloadToLocal.class);
            String saveDir = context.parseExpression(downloadAnn.saveDir());
            String annFileName = downloadAnn.filename();
            String filename = context.parseExpression(annFileName);
            filename = StringUtils.hasText(filename)
                    ? filename
                    : ResourceNameParser.getResourceName(responseMetaData);

            File saveFile = new File(saveDir, filename);
            FileCopyUtils.copy(responseMetaData.getInputStream(), new FileOutputStream(saveFile));
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
            String typeName = methodContext.getRealMethodReturnType().getTypeName();
            return (T) context.getRootVar(getValName(typeName));
        }

        @Override
        public void setProcessorAnnContext(ProcessorAnnContext processorAnnContext) {
            this.context = processorAnnContext;
        }

        private String getValName(String name) {
            return "$" + name.replaceAll("\\.", "_") + "$";
        }
    }
}
