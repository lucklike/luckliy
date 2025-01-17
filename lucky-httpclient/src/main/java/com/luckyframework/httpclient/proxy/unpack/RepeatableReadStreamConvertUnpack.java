package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.io.StorageMediumStream;

/**
 *
 */
@SuppressWarnings("all")
public class RepeatableReadStreamConvertUnpack implements ContextValueUnpack {

    @Override
    public Object getRealValue(ValueUnpackContext unpackContext, Object wrapperValue) throws ContextValueUnpackException {
        if (wrapperValue == null || (wrapperValue instanceof StorageMediumStream)) {
            return wrapperValue;
        }

        // 获取注解配置
        RepeatableReadStream convertAnn = unpackContext.toAnnotation(RepeatableReadStream.class);
        StreamType streamType = convertAnn.value();
        String storeDir = unpackContext.parseExpression(convertAnn.storeDir());

        // 执行转换
        return RepeatableReadStreamFunction.handlingObject(unpackContext, wrapperValue, streamType, storeDir);
    }


}
