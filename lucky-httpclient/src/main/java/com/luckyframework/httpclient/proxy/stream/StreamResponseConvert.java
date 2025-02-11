package com.luckyframework.httpclient.proxy.stream;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.convert.AbstractConditionalSelectionResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$LISTENER_VAR$__;

/**
 * 流式响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/11 22:46
 */
public class StreamResponseConvert extends AbstractConditionalSelectionResponseConvert {


    @Override
    protected <T> T doConvert(Response response, ConvertContext context) throws Throwable {
        StreamEventListener listener = getStreamEventListener(context);
        listener.onOpen(new Event<>(context.getContext(), response));
        try (
                InputStream in = response.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, response.getContentType().getCharset()))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    listener.onText(new Event<>(context.getContext(), line));
                } catch (Throwable e) {
                    listener.onError(new Event<>(context.getContext(), e));
                }
            }
            listener.onClose(new Event<>(context.getContext(), null));
        }
        return null;
    }


    @NonNull
    private StreamEventListener getStreamEventListener(ConvertContext context) {
        // 尝试从方法参数中获取StreamListener
        for (Object argument : context.getContext().getArguments()) {
            if (argument instanceof StreamEventListener) {
                return (StreamEventListener) argument;
            }
        }

        // 尝试从SpEL环境变量中获取StreamListener
        Object listenerVar = context.getVar(__$LISTENER_VAR$__);
        if (listenerVar instanceof StreamEventListener) {
            return (StreamEventListener) listenerVar;
        }

        // 尝试从方法注解中获取StreamListener
        StreamListener streamListenerAnn = context.getMergedAnnotationCheckParent(StreamListener.class);
        if (streamListenerAnn == null) {
            throw new StreamException("Can not find StreamListener in the method arguments or method annotation.");
        }

        if (StringUtils.hasText(streamListenerAnn.expression())) {
            return context.parseExpression(streamListenerAnn.expression());
        }
        return context.getContext().generateObject(streamListenerAnn.listener(), streamListenerAnn.listenerClass(), StreamEventListener.class);
    }
}
