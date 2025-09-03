package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.convert.AbstractConditionalSelectionResponseConvert;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$LISTENER_VAR$__;

/**
 * SSE响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/11 22:46
 */
public class SseResponseConvert extends AbstractConditionalSelectionResponseConvert {


    @Override
    protected <T> T doConvert(Response response, ConvertContext context) throws Throwable {
        // Void方法采用EventListener的方式来处理
        if (context.getContext().isVoidMethod()) {
            return eventListenerHandle(response, context);
        }

        if (Flux.class.isAssignableFrom(context.getContext().getReturnType())) {
            return convertToFlux(response, context);
        }

        throw new SseException("SSE方法定义异常");
    }


    @SuppressWarnings("all")
    private <T> T convertToFlux(Response response, ConvertContext context) throws Throwable {
        return (T) Flux.create(sink -> {

            AtomicBoolean isCancelled = new AtomicBoolean(false);
            context.getContext().getAsyncTaskExecutor().execute(() -> {
                try (
                        InputStream in = response.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, response.getContentType().getCharset()))
                ) {
                    String line;
                    while (!isCancelled.get() && (line = reader.readLine()) != null) {
                        if (isCancelled.get()) {
                            break;
                        }
                        sink.next(line);
                    }
                    if (!isCancelled.get()) {
                        sink.complete();
                    }
                } catch (Throwable e) {
                    sink.error(e);
                }
            });

            // 注册取消订阅时的回调
            sink.onCancel(() -> {
                isCancelled.set(true); // 设置取消标志，让读取循环退出
                // 注意：这里无法直接关闭HttpURLConnection的流，但标志位会让循环在下次检查时退出
            });

            // 注册dispose时的回调（与onCancel类似）
            sink.onDispose(() -> {
                isCancelled.set(true);
            });

        },  FluxSink.OverflowStrategy.BUFFER);

    }

    private <T> T eventListenerHandle(Response response, ConvertContext context) throws Throwable {
        EventListener listener = getEventListener(context);
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
        } finally {
            listener.onClose(new Event<>(context.getContext(), null));
        }
        return null;
    }


    @NonNull
    private EventListener getEventListener(ConvertContext context) {
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
            throw new SseException("Can not find SSE Listener in the method arguments or method annotation.");
        }

        if (StringUtils.hasText(sseListenerAnn.expression())) {
            return context.parseExpression(sseListenerAnn.expression());
        }
        return context.getContext().generateObject(sseListenerAnn.listener(), sseListenerAnn.listenerClass(), EventListener.class);
    }
}
