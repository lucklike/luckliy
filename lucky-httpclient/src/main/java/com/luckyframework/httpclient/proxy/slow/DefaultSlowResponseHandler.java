package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 默认的慢响应处理器
 */
public class DefaultSlowResponseHandler implements SlowResponseHandler {

    @Override
    public void handleSlowResponse(MethodContext context, Response response, ResponseTimeSpent responseTimeSpent) {
        HandlerSlowResponse handlerSlowResponseAnn = context.getSameAnnotationCombined(HandlerSlowResponse.class);
        SlowResponseHandlerFunction handlerFunction = context.generateObject(handlerSlowResponseAnn.handler(), handlerSlowResponseAnn.handlerClass(), SlowResponseHandlerFunction.class);
        handlerFunction.handleSlowResponse(context, response, responseTimeSpent, context.parseExpression(handlerSlowResponseAnn.slowTime(), long.class));
    }

    @Override
    public long getSlowTime(MethodContext context) {
        HandlerSlowResponse handlerSlowResponseAnn = context.getSameAnnotationCombined(HandlerSlowResponse.class);
        return context.parseExpression(handlerSlowResponseAnn.slowTime(), long.class);
    }
}
