package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.PropertiesJava;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.List;

/**
 * properties文件格式的JAVA对象请求体解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 17:30
 */
public class FlatBeanPropertiesJavaObjectResolver extends AbstractPropertiesJsonResolver {


    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        PropertiesJava javaAnn = context.toAnnotation(PropertiesJava.class);
        return parserExpression(context, javaAnn.value(), javaAnn.separator());
    }

}
