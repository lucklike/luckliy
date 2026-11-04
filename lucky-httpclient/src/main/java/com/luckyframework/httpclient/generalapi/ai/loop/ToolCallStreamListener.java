package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.vendor.StreamAccumulator;
import com.luckyframework.httpclient.generalapi.ai.vendor.StreamEvent;
import com.luckyframework.httpclient.generalapi.ai.vendor.StreamProtocolException;
import com.luckyframework.httpclient.generalapi.ai.vendor.VendorAdapter;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.ContextAware;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.httpclient.proxy.sse.standard.Message;
import com.luckyframework.httpclient.proxy.sse.standard.StandardEventListener;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 流式工具调用的装饰监听器。
 * <pre>
 *     本监听器代替业务方的 {@link EventListener} 被挂到SSE调用上（它继承标准监听器，
 *     因此SSE的按空行组帧与尾帧flush能力直接复用，不需要改动SSE基建），职责有三件：
 *
 *     1. 把每一帧交给厂商方言的增量拼接器，文本增量原样转投业务方的onText，
 *        完整工具调用则截留下来交给 {@link FunctionCallingRunner} 执行；
 *     2. 每一轮流式响应结束（或出错）时唤醒等待中的回环线程，由回环决定是否再发一轮；
 *     3. 让业务方只看到一个连续的流：中间轮次的onOpen/onCompleted/onClose全部吞掉，
 *        onCompleted只在最后一轮（模型不再要求调用工具）转发一次。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，流式回环的监听器装饰器，由 {@link FunctionCallingRunner} 创建；业务方只实现自己的 EventListener
 * @see ToolCallProxyPlugin
 * @see FunctionCallingRunner
 */
public class ToolCallStreamListener extends StandardEventListener {

    /**
     * 兼容SSE协议的流结束标记，它不是JSON帧
     */
    private static final String DONE_MARKER = "[DONE]";

    private final EventListener delegate;
    private final VendorAdapter adapter;
    private final List<ToolCall> toolCalls = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();

    private StreamAccumulator accumulator;
    private CountDownLatch roundLatch = new CountDownLatch(0);
    private Throwable error;
    private boolean openForwarded;
    private boolean completedForwarded;
    private boolean closedForwarded;

    public ToolCallStreamListener(EventListener delegate, VendorAdapter adapter) {
        this.delegate = delegate;
        this.adapter = adapter;
    }

    /**
     * 发起一轮流式调用之前重置本轮状态（拼接器每轮一个实例，保证拼接互不污染）
     */
    public void startRound() {
        accumulator = adapter.newAccumulator();
        toolCalls.clear();
        text.setLength(0);
        error = null;
        roundLatch = new CountDownLatch(1);
    }

    /**
     * 等待本轮流式响应结束（正常结束与出错都会唤醒）
     */
    public void awaitRound() {
        try {
            roundLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolCallException("The streaming tool-call loop was interrupted while waiting for a round to finish.", e);
        }
    }

    /**
     * 本轮拼接出的完整工具调用（回环据此决定是否再发一轮）
     */
    public List<ToolCall> getToolCalls() {
        return Collections.unmodifiableList(toolCalls);
    }

    /**
     * 本轮拼接出的全部文本
     */
    public String getText() {
        return text.toString();
    }

    /**
     * 本轮流式响应中的异常，无异常时为null
     */
    public Throwable getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    /**
     * 回环正常收敛：把终止事件交给业务方，业务方全程只看到这一次
     */
    public void complete() {
        if (!completedForwarded) {
            completedForwarded = true;
            try {
                delegate.onCompleted();
            } catch (Throwable e) {
                throw new ToolCallException("The delegate EventListener failed on onCompleted: " + e, e);
            }
        }
        close();
    }

    /**
     * 回环异常结束（协议错误、轮次超限）时只补一个关闭事件
     */
    public void close() {
        if (!closedForwarded) {
            closedForwarded = true;
            delegate.onClose();
        }
    }

    // ------------------------------------------------------------------------------------
    // SSE回调
    // ------------------------------------------------------------------------------------

    @Override
    protected void onMessage(Message message) throws Exception {
        String data = message.getData();
        if (data == null) {
            return;
        }
        data = data.trim();
        if (data.isEmpty() || DONE_MARKER.equals(data)) {
            return;
        }
        handle(accumulator.feed(parseFrame(data)));
    }

    @Override
    protected void onOpening(Response response) throws Exception {
        injectDelegateContext();
        if (!openForwarded) {
            openForwarded = true;
            try {
                delegate.onOpen(response);
            } catch (Throwable e) {
                throw new ToolCallException("The delegate EventListener failed on onOpen: " + e, e);
            }
        }
    }

    @Override
    protected void onCompleting() {
        try {
            handle(accumulator.finish());
        } catch (Throwable e) {
            // 收尾阶段（尾帧flush与finish校验）出的错同样走异常终止路径
            onError(e);
            return;
        }
        roundLatch.countDown();
    }

    @Override
    protected void onClosed() {
        // 中间轮的关闭事件不能透传，否则业务方会看到多次开关流
    }

    @Override
    public void onError(Throwable event) {
        if (error == null) {
            error = event;
            delegate.onError(event);
        }
        roundLatch.countDown();
    }

    // ------------------------------------------------------------------------------------
    // 内部逻辑
    // ------------------------------------------------------------------------------------

    private void handle(List<StreamEvent> events) throws Exception {
        for (StreamEvent event : events) {
            if (event.isToolCall()) {
                toolCalls.add(event.getToolCall());
            } else if (event.getTextDelta() != null && !event.getTextDelta().isEmpty()) {
                text.append(event.getTextDelta());
                delegate.onText(event.getTextDelta());
            }
        }
    }

    private static Map<String, Object> parseFrame(String data) {
        try {
            Map<String, Object> frame = AiJsonUtils.fromJson(data, new SerializationTypeToken<Map<String, Object>>() {
            });
            if (frame == null) {
                throw new StreamProtocolException("Ill-formed streamed frame, the SSE data payload is not a JSON object: " + data);
            }
            return frame;
        } catch (StreamProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new StreamProtocolException("Ill-formed streamed frame, the SSE data payload is not a JSON object: " + data, e);
        }
    }

    /**
     * 业务方监听器若需要方法上下文（如实现了 {@link ContextAware}），把本轮上下文一并交给它
     */
    private void injectDelegateContext() {
        Context context = getContext();
        if (delegate instanceof ContextAware && context != null) {
            ((ContextAware) delegate).setContext(context);
        }
    }
}
