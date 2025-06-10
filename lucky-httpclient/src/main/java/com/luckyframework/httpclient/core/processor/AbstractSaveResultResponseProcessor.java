package com.luckyframework.httpclient.core.processor;

import com.luckyframework.httpclient.core.encoder.ContentEncodingConvertor;
import com.luckyframework.httpclient.core.encoder.GzipContentEncodingConvertor;
import com.luckyframework.httpclient.core.encoder.InflaterContentEncodingConvertor;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
import com.luckyframework.httpclient.proxy.exeception.ResponseProcessException;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Collection;
import java.util.Map;

/**
 * 保存响应结果的响应处理器
 *
 * @author fukang
 * @version 2.1.1
 * @date 2024/05/24 11:24
 */
public abstract class AbstractSaveResultResponseProcessor<R> implements ResponseProcessor {

    private static final Map<String, ContentEncodingConvertor> CEC_MAP = new LinkedCaseInsensitiveMap<>();

    static {
        addContentEncodingConvertor(new GzipContentEncodingConvertor());
        addContentEncodingConvertor(new InflaterContentEncodingConvertor());
    }

    public static void addContentEncodingConvertor(ContentEncodingConvertor convertor) {
        CEC_MAP.put(convertor.contentEncoding(), convertor);
    }

    public static ContentEncodingConvertor getContentEncodingConvertor(String name) {
        return CEC_MAP.get(name);
    }

    public static Collection<ContentEncodingConvertor> getContentEncodingConvertors() {
        return CEC_MAP.values();
    }

    private R result;

    public R getResult() {
        return this.result;
    }

    @Override
    public void process(ResponseMetaData responseMetaData) throws Exception {
        try {
            this.result = convert(getFinallyResponseMetaData(responseMetaData));
        } catch (Exception e) {
            throw new ResponseProcessException("An exception occurred while processing the response result of the HTTP request:" + responseMetaData.getRequest().getUrl(), e);
        }
    }

    /**
     * 获取最终的{@link ResponseMetaData}
     *
     * @param responseMetaData 原始响应元数据
     * @return 最终响应元数据
     */
    private ResponseMetaData getFinallyResponseMetaData(ResponseMetaData responseMetaData) {
        Header contentEncodingHeader = responseMetaData.getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_ENCODING);
        if (contentEncodingHeader == null) {
            return responseMetaData;
        }

        ContentEncodingConvertor encodingConvertor = getContentEncodingConvertor(String.valueOf(contentEncodingHeader.getValue()));
        if (encodingConvertor == null) {
            return responseMetaData;
        }

        return new ResponseMetaData(
                responseMetaData.getRequest(),
                responseMetaData.getStatus(),
                responseMetaData.getHeaderManager(),
                () -> encodingConvertor.inputStreamConvert(responseMetaData.getInputStream())
        );
    }


    /**
     * 将响应元数据转换为目标类型
     *
     * @param responseMetaData 响应元数据
     * @return 目标类型实例
     */
    protected abstract R convert(ResponseMetaData responseMetaData) throws Exception;
}
