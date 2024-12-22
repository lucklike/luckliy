package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.convert.AbstractConditionalSelectionResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$LISTENER_VAR$__;


/**
 * SSE结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
public class SseResponseConvert extends AbstractConditionalSelectionResponseConvert {

    /**
     * 数据分隔符
     */
    private static final String SEPARATOR = ":";

    @Override
    public <T> T doConvert(Response response, ConvertContext context) throws Throwable {
        EventListener listener = getSseEventListener(context);
        listener.onOpen(new Event<>(context.getContext(), response));
        try (
                InputStream in = response.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, response.getContentType().getCharset()))
        ) {
            Properties properties = new Properties();
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (!StringUtils.hasText(line)) {
                        listener.onMessage(new Event<>(context.getContext(), new Message(properties)));
                        properties = new Properties();
                    } else {
                        int index = line.indexOf(SEPARATOR);
                        if (index != -1) {
                            properties.put(line.substring(0, index), line.substring(index + 1));
                        }
                    }
                } catch (Throwable e) {
                    listener.onError(new Event<>(context.getContext(), e));
                }
            }
            listener.onClose(new Event<>(context.getContext(), null));
        }
        return null;
    }

    @NonNull
    private EventListener getSseEventListener(ConvertContext context) {
        // 尝试从方法参数中获取EventListener
        for (Object argument : context.getContext().getArguments()) {
            if (argument instanceof EventListener) {
                return (EventListener) argument;
            }
        }

        // 尝试从SpEL环境变量中获取EventListener
        Object listenerVar = context.getVar(__$LISTENER_VAR$__);
        if (listenerVar instanceof EventListener) {
            return (EventListener) listenerVar;
        }

        // 尝试从方法注解中获取EventListener
        SseListener sseListenerAnn = context.getMergedAnnotationCheckParent(SseListener.class);
        if (sseListenerAnn == null) {
            throw new SseException("Can not find SSE EventListener in the method arguments or method annotation.");
        }

        if (StringUtils.hasText(sseListenerAnn.expression())) {
            return context.parseExpression(sseListenerAnn.expression());
        }
        return context.getContext().generateObject(sseListenerAnn.listener(), sseListenerAnn.listenerClass(), EventListener.class);
    }

}
