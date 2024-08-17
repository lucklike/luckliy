package com.luckyframework.httpclient.core.meta;

import com.luckyframework.serializable.SerializationException;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Response默认实现
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/3 2:59 下午
 */
public class DefaultResponse implements Response {

    private final ResponseMetaData responseMetaData;
    private byte[] result;

    public DefaultResponse(ResponseMetaData metaData) {
        this.responseMetaData = metaData;
    }

    @Override
    public synchronized byte[] getResult() {
        if (result == null) {
            try {
                result = FileCopyUtils.copyToByteArray(responseMetaData.getInputStream());
            } catch (IOException e) {
               throw new SerializationException(e);
            }
        }
        return result;
    }

    @Override
    public InputStream getInputStream() {
        if (result == null) {
            return responseMetaData.getInputStream();
        }
        return new ByteArrayInputStream(result);
    }

    @Override
    public ResponseMetaData getResponseMetaData() {
        return this.responseMetaData;
    }

    @Override
    public void closeResource() {
        try {
            if (result == null) {
                responseMetaData.getInputStream().close();
            }
        } catch (IOException ex) {
            // ignore
        }
    }
}
