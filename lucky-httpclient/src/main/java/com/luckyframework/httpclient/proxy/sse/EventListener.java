package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;

import java.util.ArrayList;
import java.util.List;


/**
 * SSE事件监听器
 */
public interface EventListener {

    /**
     * 当连接建立时触发
     *
     * @param event 连接建立事件
     */
    default void onOpen(Event<Response> event) throws Throwable {
        MethodContext methodContext = event.getContext();
        List<Condition> conditions = new ArrayList<>();

        // 获取方法和类上的@Condition注解
        conditions.addAll(methodContext.getParentContext().findNestCombinationAnnotations(Condition.class));
        conditions.addAll(methodContext.findNestCombinationAnnotations(Condition.class));

        for (Condition condition : conditions) {
            boolean assertion = methodContext.parseExpression(condition.assertion(), boolean.class);
            if (assertion) {
                String exception = condition.exception();
                if (StringUtils.hasText(exception)) {
                    Object exObj = methodContext.parseExpression(exception);
                    if (exObj instanceof Throwable) {
                        throw (Throwable) exObj;
                    }
                    throw new ActivelyThrownException(String.valueOf(exObj));
                }
            }
        }
    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param event 消息事件
     */
    default void onMessage(Event<Message> event) throws Exception {

    }

    /**
     * 当发生错误时触发
     *
     * @param event 异常事件
     */
    default void onError(Event<Throwable> event) {
        throw new SseException(event.getMessage());
    }

    /**
     * 当连接关闭时触发
     *
     * @param event 连接关闭事件
     */
    default void onClose(Event<Void> event) {

    }
}
