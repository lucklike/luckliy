package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.HeaderMataData;
import com.luckyframework.httpclient.core.ResponseMetaData;

/**
 * 保存{@link HeaderMataData}为结果的响应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/05/28 17:58
 */
public class SaveHeaderMataDataProcessor extends AbstractSaveResultResponseProcessor<HeaderMataData> {

    @Override
    protected HeaderMataData convert(ResponseMetaData responseMetaData) throws Exception {
        return responseMetaData;
    }

}
