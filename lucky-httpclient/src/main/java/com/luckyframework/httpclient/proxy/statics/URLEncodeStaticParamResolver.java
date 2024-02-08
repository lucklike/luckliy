package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.URLEncodeStaticParam;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 基于注解value属性、配置使用'='分隔的静态参数解析器
 * 加上是否需要URLEncode编码的逻辑
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 15:51
 */
public class URLEncodeStaticParamResolver extends SpELValueFieldEqualSeparationStaticParamResolver {


    @Override
    protected ParamInfo postProcess(StaticParamAnnContext context, ParamInfo originalParamInfo) {
        URLEncodeStaticParam urlEncodeAnn = context.toAnnotation(URLEncodeStaticParam.class);
        boolean urlEncode = urlEncodeAnn.urlEncode();
        if (urlEncode) {
            String charset = urlEncodeAnn.charset();
            try {
                String encodeValue = URLEncoder.encode(String.valueOf(originalParamInfo.getValue()), charset);
                return new ParamInfo(originalParamInfo.getName(), encodeValue);
            } catch (UnsupportedEncodingException e) {
                throw new HttpExecutorException("url encoding(" + charset + ") exception: value='" + originalParamInfo.getValue() + "'", e);
            }
        }
        return super.postProcess(context, originalParamInfo);
    }
}
