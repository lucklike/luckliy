package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.ObjectCreator;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.SpecialOperationFunction;
import com.luckyframework.httpclient.proxy.ValueContext;
import com.luckyframework.httpclient.proxy.annotations.Base64Encoder;
import com.luckyframework.httpclient.proxy.annotations.SpecialOperation;
import com.luckyframework.httpclient.proxy.annotations.URLEncoder;

import java.lang.annotation.Annotation;
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
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        SpecialOperation soAnn = context.getMergedAnnotationCheckParent(SpecialOperation.class);
        String originalParamName = getOriginalParamName(context);
        if (soAnn == null || !soAnn.enable() || soAnn.value() == SpecialOperationFunction.class) {
            return Collections.singletonList(new ParamInfo(originalParamName, context.getValue()));
        }
        ObjectCreator objectCreator = HttpClientProxyObjectFactory.getObjectCreator();
        SpecialOperationFunction soFun = objectCreator.newObject(soAnn.value(), soAnn.funMsg());
        return Collections.singletonList(new ParamInfo(
                originalParamName,
                soFun.change(originalParamName, context.getValue(), soAnn)
        ));
    }
}
