package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.RepeatableReadStream;
import com.luckyframework.httpclient.proxy.annotations.StreamType;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.RepeatableReadStreamUtil;
import com.luckyframework.reflect.AnnotationUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;

public class RepeatableReadStreamConvertUnpack implements ContextValueUnpack {

    @Override
    public Object getRealValue(ValueUnpackContext unpackContext, Object wrapperValue) throws ContextValueUnpackException {
        if (wrapperValue instanceof InputStream) {
            RepeatableReadStream convertAnn = unpackContext.toAnnotation(RepeatableReadStream.class);
            StreamType streamType = convertAnn.value();
            try {
                if (streamType == StreamType.BYTE_ARRAY) {
                    return RepeatableReadStreamUtil.useByteStore((InputStream) wrapperValue);
                }
                String storeDir = unpackContext.parseExpression(convertAnn.storeDir());
                if (!StringUtils.hasText(storeDir)) {
                    return RepeatableReadStreamUtil.useFileStore((InputStream) wrapperValue);
                }
                File storeFile = new File(storeDir, NanoIdUtils.randomNanoId());
                FileUtils.createSaveFolder(storeFile.getParentFile());
                return RepeatableReadStreamUtil.useFileStore(storeFile, (InputStream) wrapperValue);
            }catch (IOException e) {
                throw new ContextValueUnpackException(e);
            }


        }
        return wrapperValue;
    }
}
