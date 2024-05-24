package com.luckyframework.httpclient.core.impl;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;

/**
 * 将响应结果以byte[]保存的响应处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:34
 */
public class SaveResponseInstanceProcessor extends AbstractSaveResultResponseProcessor<Response> {

    @Override
    protected Response convert(ResponseMetaData responseMetaData) throws Exception{
        return new DefaultResponse(responseMetaData);
    }

}