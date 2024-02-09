package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Base64Encoder;
import com.luckyframework.httpclient.proxy.annotations.SpecialOperation;
import com.luckyframework.httpclient.proxy.annotations.URLEncoder;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.special.SpecialOperationFunction;

import java.util.Collections;
import java.util.List;

/**
 * 查找到特殊注解之后进行对应处理的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:04
 * @see SpecialOperation
 * @see URLEncoder
 * @see Base64Encoder
 */
public class LookUpSpecialAnnotationDynamicParamResolver extends AbstractDynamicParamResolver {


    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        SpecialOperation soAnn = valueContext.getMergedAnnotationCheckParent(SpecialOperation.class);
        String originalParamName = getOriginalParamName(valueContext);
        if (soAnn == null || !soAnn.enable() || soAnn.operation().clazz() == SpecialOperationFunction.class) {
            return Collections.singletonList(new ParamInfo(originalParamName, valueContext.getValue()));
        }
        ObjectCreator objectCreator = HttpClientProxyObjectFactory.getObjectCreator();
        SpecialOperationFunction soFun = (SpecialOperationFunction) objectCreator.newObject(soAnn.operation(), valueContext);
        return Collections.singletonList(new ParamInfo(
                originalParamName,
                soFun.change(originalParamName, valueContext.getValue(), soAnn)
        ));
    }
}
