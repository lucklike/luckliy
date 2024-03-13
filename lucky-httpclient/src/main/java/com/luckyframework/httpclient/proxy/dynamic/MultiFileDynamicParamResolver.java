package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.annotations.MultiFileParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 资源参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:10
 */
public class MultiFileDynamicParamResolver extends AbstractDynamicParamResolver {

    private final String _index_ = "$index$";

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        Object value = valueContext.getValue();
        HttpFile[] httpFiles;
        if (valueContext.isResourceType()) {
            httpFiles = HttpExecutor.toHttpFiles(value);
        }
        // 字符串类型尝试转为Resource数组之后在做转换
        else if (value instanceof String) {
            httpFiles = HttpExecutor.toHttpFiles(ConversionUtils.conversion(value, Resource[].class));
        }
        // 其他情况
        else {
            String fileName = getFileName(context, context.toAnnotation(MultiFileParam.class).fileName());
            if (value instanceof InputStream) {
                httpFiles = new HttpFile[1];
                httpFiles[0] = new HttpFile(((InputStream) value), fileName);
            } else if (value instanceof byte[]) {
                httpFiles = new HttpFile[1];
                httpFiles[0] = new HttpFile(((byte[]) value), fileName);
            } else if (value instanceof Byte[]) {
                httpFiles = new HttpFile[1];
                httpFiles[0] = new HttpFile(toBytes((Byte[]) value), fileName);
            } else {
                httpFiles = null;
            }

        }
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), httpFiles));
    }

    private String getFileName(DynamicParamContext context, String fileNameConfig) {
        return fileNameConfig;
    }

    private byte[] toBytes(Byte[] bytes) {
        byte[] bytesArr = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            bytesArr[i] = bytes[i];
        }
        return bytesArr;
    }
}
