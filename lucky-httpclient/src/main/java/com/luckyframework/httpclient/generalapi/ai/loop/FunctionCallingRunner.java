package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolArguments;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolArgumentsException;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolExecutor;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolResult;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.httpclient.generalapi.ai.vendor.VendorAdapter;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 阻塞式工具调用回环引擎。
 * <pre>
 *     主流程：发出会话请求 → 解析响应 → 若响应含工具调用则执行工具、把结果作为新消息追加并重放请求，
 *     直到某轮响应不再含工具调用，以该轮结果作为最终返回值。
 *
 *     每一轮都重新调用同一个代理方法（代理对象 + 同一Method + 新参数数组），从而拿到全新的方法上下文，
 *     注解配置（base-url、鉴权头、重试、日志、Mock、转换器）自动复用，本引擎不实现任何HTTP逻辑。
 *     防重入标记放在 {@link ChatRequest#getLoopRound()} 上，插件看到轮次大于0时直接走单次请求。
 *
 *     本引擎只在调用方请求对象的副本上追加消息，调用方对象不被就地修改。
 *
 *     流式（SSE）回环与阻塞回环共用同一份轮次上限、失败回填与副本语义，
 *     只是「发一轮」的实现换成「一次流式代理方法调用 + 等待本轮聚合结果」（见 {@link ToolCallStreamListener}）。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，回环引擎的实现细节；业务方只需在接口方法上标 {@link EnableToolCall}
 * @see ToolCallProxyPlugin
 * @see EnableToolCall
 */
public class FunctionCallingRunner {

    private final ExecuteMeta meta;
    private final ToolRegistry registry;
    private final List<ToolSpec> declarations;
    private final int maxRounds;
    private final ToolCallFailurePolicy failurePolicy;
    private final VendorAdapter adapter;

    public FunctionCallingRunner(ExecuteMeta meta,
                                 ToolRegistry registry,
                                 List<ToolSpec> declarations,
                                 int maxRounds,
                                 ToolCallFailurePolicy failurePolicy,
                                 VendorAdapter adapter) {
        this.meta = meta;
        this.registry = registry;
        this.declarations = declarations;
        this.maxRounds = maxRounds;
        this.failurePolicy = failurePolicy;
        this.adapter = adapter;
    }

    /**
     * 执行回环直到模型给出最终回答
     *
     * @param callerRequest 调用方传入的会话请求对象，本方法不会就地修改它
     * @return 最后一轮的响应结果，由代理方法按声明的返回类型转换
     * @throws Throwable 回环过程中按失败策略上抛的异常，以及轮次超限异常
     */
    public Object run(ChatRequest callerRequest) throws Throwable {
        if (isStreamingRound(callerRequest)) {
            return runStream(callerRequest);
        }
        ChatRequest current = callerRequest.copy();
        current.setToolSpecs(declarations);
        List<ToolCall> lastRoundCalls = Collections.emptyList();
        for (int round = 1; round <= maxRounds; round++) {
            current.setLoopRound(round);
            Object result = invokeProxyMethod(current);
            if (!(result instanceof ChatResponse)) {
                return result;
            }
            ChatResponse response = (ChatResponse) result;
            List<ToolCall> toolCalls = response.getToolCalls();
            if (toolCalls.isEmpty()) {
                return result;
            }
            lastRoundCalls = toolCalls;
            current.addAssistantToolCalls(new ArrayList<>(toolCalls));
            // 严格按响应中的调用顺序逐条回填，保证按id/name配对的厂商不断裂
            for (ToolCall toolCall : toolCalls) {
                ToolResult toolResult = executeTool(toolCall);
                current.addMessage(ChatMessage.toolResult(toolCall.getId(), toolResult.getContent(), !toolResult.isSuccess()));
            }
        }
        throw ToolCallException.roundLimit(maxRounds, lastRoundCalls);
    }

    /**
     * 以新的参数数组重新调用一次代理方法，本轮请求由此发出
     *
     * @param roundRequest 本轮的会话请求（副本）
     * @return 代理方法的返回值
     * @throws Throwable 本轮请求执行过程中的异常
     */
    private Object invokeProxyMethod(ChatRequest roundRequest) throws Throwable {
        return invokeProxyMethod(roundRequest, null);
    }

    /**
     * 以新的参数数组重新调用一次代理方法，本轮请求由此发出
     *
     * @param roundRequest 本轮的会话请求（副本）
     * @param listener     需要替换到方法参数里的流式监听器，为null时不替换（阻塞式回环）
     * @return 代理方法的返回值
     * @throws Throwable 本轮请求执行过程中的异常
     */
    private Object invokeProxyMethod(ChatRequest roundRequest, EventListener listener) throws Throwable {
        Object[] args = meta.getArgs().clone();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ChatRequest) {
                args[i] = roundRequest;
            } else if (listener != null && args[i] instanceof EventListener) {
                args[i] = listener;
            }
        }
        try {
            return MethodUtils.invoke(meta.getProxy(), meta.getMethod(), args);
        } catch (LuckyInvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * 是否为流式回环：业务把会话请求的stream设为true且方法参数里提供了{@link EventListener}时走流式分支
     */
    private boolean isStreamingRound(ChatRequest request) {
        return Boolean.TRUE.equals(request.getStream()) && findListenerArgument() != null;
    }

    private EventListener findListenerArgument() {
        for (Object arg : meta.getArgs()) {
            if (arg instanceof EventListener) {
                return (EventListener) arg;
            }
        }
        return null;
    }

    /**
     * 流式回环：每轮发一次流式代理方法调用并等待本轮聚合结果，拼接出的工具调用同样逐条回填后重放。
     * 轮次上限、失败回填与请求副本语义与阻塞式完全一致。
     *
     * @param callerRequest 调用方传入的会话请求对象，本方法不会就地修改它
     * @return 流式方法的返回值固定为null（结果通过监听器输出）
     * @throws Throwable 回环过程中按失败策略上抛的异常，以及轮次超限异常
     */
    private Object runStream(ChatRequest callerRequest) throws Throwable {
        if (adapter == null) {
            throw new ToolCallException("A streaming tool-call loop needs a vendor dialect to create the stream accumulator, "
                    + "please declare it on @EnableToolCall(dialect=\"#{...}\").");
        }
        EventListener businessListener = findListenerArgument();
        if (businessListener == null) {
            throw new ToolCallException("The streaming @EnableToolCall method ['" + meta.getMethod().getName()
                    + "'] must have an EventListener parameter to receive the text deltas.");
        }
        ToolCallStreamListener listener = new ToolCallStreamListener(businessListener, adapter);
        ChatRequest current = callerRequest.copy();
        current.setToolSpecs(declarations);
        List<ToolCall> lastRoundCalls = Collections.emptyList();
        boolean converged = false;
        try {
            for (int round = 1; round <= maxRounds; round++) {
                current.setLoopRound(round);
                listener.startRound();
                invokeProxyMethod(current, listener);
                listener.awaitRound();
                if (listener.hasError()) {
                    // 异常已经通过onError交给业务方，本方法不再重复上抛（与框架SSE语义一致）
                    return null;
                }
                List<ToolCall> toolCalls = listener.getToolCalls();
                if (toolCalls.isEmpty()) {
                    converged = true;
                    listener.complete();
                    return null;
                }
                lastRoundCalls = toolCalls;
                current.addAssistantToolCalls(new ArrayList<>(toolCalls));
                // 严格按拼接出的调用顺序逐条回填，与非流式回环共用同一套执行逻辑
                for (ToolCall toolCall : toolCalls) {
                    ToolResult toolResult = executeTool(toolCall);
                    current.addMessage(ChatMessage.toolResult(toolCall.getId(), toolResult.getContent(), !toolResult.isSuccess()));
                }
            }
            throw ToolCallException.roundLimit(maxRounds, lastRoundCalls);
        } finally {
            if (!converged) {
                listener.close();
            }
        }
    }

    /**
     * 执行一次工具调用，全部失败都收敛为失败的工具结果，只有ABORT策略才把执行异常上抛
     *
     * @param toolCall 模型发起的工具调用
     * @return 工具执行结果
     * @throws Throwable ABORT策略下的工具执行异常
     */
    private ToolResult executeTool(ToolCall toolCall) throws Throwable {
        String toolName = toolCall.getName();
        Optional<ToolSpec> specOptional = registry.findSpec(toolName);
        if (!specOptional.isPresent()) {
            return ToolResult.failure("未知工具 [" + toolName + "]，它不在本次可用的工具集合内。");
        }

        Map<String, Object> arguments;
        try {
            arguments = parseArguments(toolCall.getArguments());
        } catch (Exception e) {
            return ToolResult.failure("工具 [" + toolName + "] 的参数JSON解析失败：" + e.getMessage());
        }
        try {
            // 校验顺序：JSON合法性 → 按schema取值/转换 → 必填检查，三类失败都在执行器之前判定
            arguments = ToolArguments.bind(toolName, specOptional.get().getParameters(), arguments);
        } catch (ToolArgumentsException e) {
            return ToolResult.failure(e.getMessage());
        }

        Optional<ToolExecutor> executorOptional = registry.findExecutor(toolName);
        if (!executorOptional.isPresent()) {
            return ToolResult.noExecutor(toolName);
        }

        try {
            return ToolResult.success(executorOptional.get().execute(arguments));
        } catch (Throwable e) {
            if (failurePolicy == ToolCallFailurePolicy.ABORT) {
                throw e;
            }
            return ToolResult.failure("工具 [" + toolName + "] 执行失败：" + e);
        }
    }

    /**
     * 把参数JSON文本解析为参数集合，空文本视为无参数
     */
    private static Map<String, Object> parseArguments(String argumentsText) {
        if (argumentsText == null || argumentsText.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        return AiJsonUtils.fromJson(argumentsText, new SerializationTypeToken<Map<String, Object>>() {
        });
    }
}
