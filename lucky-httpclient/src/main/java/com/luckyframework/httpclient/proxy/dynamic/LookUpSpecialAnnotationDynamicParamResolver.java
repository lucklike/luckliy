package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.SpecialOperationFunction;
import com.luckyframework.httpclient.proxy.annotations.Base64Encoder;
import com.luckyframework.httpclient.proxy.annotations.SpecialOperation;
import com.luckyframework.httpclient.proxy.annotations.URLEncoder;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.creator.ObjectCreator;

import java.util.Collections;
import java.util.List;

/**
 * 查找到特殊注解之后进行对应处理的动态参数解析器
 * @see SpecialOperation
 * @see URLEncoder
 * @see Base64Encoder
 *
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:04
 */
public class LookUpSpecialAnnotationDynamicParamResolver extends AbstractDynamicParamResolver {


    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        SpecialOperation soAnn = valueContext.getMergedAnnotationCheckParent(SpecialOperation.class);
        String originalParamName = getOriginalParamName(valueContext);
        if (soAnn == null || !soAnn.enable() || soAnn.value() == SpecialOperationFunction.class) {
            return Collections.singletonList(new ParamInfo(originalParamName, valueContext.getValue()));
        }
        ObjectCreator objectCreator = HttpClientProxyObjectFactory.getObjectCreator();
        SpecialOperationFunction soFun = objectCreator.newObject(soAnn.value(), soAnn.funMsg());
        return Collections.singletonList(new ParamInfo(
                originalParamName,
                soFun.change(originalParamName, valueContext.getValue(), soAnn)
        ));
    }
}
