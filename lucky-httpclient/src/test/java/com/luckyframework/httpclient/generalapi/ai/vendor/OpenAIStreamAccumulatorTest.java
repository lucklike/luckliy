package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * OpenAI兼容簇增量拼接器：按 {@code delta.tool_calls[].index} 分桶、参数增量按到达顺序连接、
 * 文本增量与工具增量互不干扰，且半成品参数在结束信号到来前不对外可见。
 * 全部断言以录制的SSE帧列表驱动，不发起任何网络请求。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class OpenAIStreamAccumulatorTest {

    private static final String COMPLETE_ARGUMENTS = "{\"city\":\"北京\"}";

    private final OpenAIStreamAccumulator accumulator = new OpenAIStreamAccumulator();

    // ------------------------------------------------------------------------------------
    // 帧构造辅助（与spec样例的帧结构一一对应）
    // ------------------------------------------------------------------------------------

    private static Map<String, Object> toolCallFragment(int index, String id, String name, String arguments) {
        Map<String, Object> function = new LinkedHashMap<>();
        if (name != null) {
            function.put("name", name);
        }
        if (arguments != null) {
            function.put("arguments", arguments);
        }
        Map<String, Object> fragment = new LinkedHashMap<>();
        fragment.put("index", index);
        if (id != null) {
            fragment.put("id", id);
        }
        fragment.put("type", "function");
        if (!function.isEmpty()) {
            fragment.put("function", function);
        }
        return fragment;
    }

    private static Map<String, Object> deltaFrame(String content, List<Map<String, Object>> toolCalls, String finishReason) {
        Map<String, Object> delta = new LinkedHashMap<>();
        if (content != null) {
            delta.put("content", content);
        }
        if (toolCalls != null) {
            delta.put("tool_calls", toolCalls);
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("object", "chat.completion.chunk");
        frame.put("choices", Collections.singletonList(choice));
        return frame;
    }

    /**
     * spec 样例的三段增量：首帧给出id与name，后两帧只给参数文本增量，末帧带 finish_reason
     */
    private static List<Map<String, Object>> specDeltaFrames() {
        List<Map<String, Object>> frames = new ArrayList<>();
        frames.add(deltaFrame(null, Collections.singletonList(
                toolCallFragment(0, "call_001", "get_weather", "")), null));
        frames.add(deltaFrame(null, Collections.singletonList(
                toolCallFragment(0, null, null, "{\"city\"")), null));
        frames.add(deltaFrame(null, Collections.singletonList(
                toolCallFragment(0, null, null, ":\"北京\"}")), "tool_calls"));
        return frames;
    }

    private static List<StreamEvent> feedAll(StreamAccumulator accumulator, List<Map<String, Object>> frames) {
        List<StreamEvent> events = new ArrayList<>();
        for (Map<String, Object> frame : frames) {
            events.addAll(accumulator.feed(frame));
        }
        events.addAll(accumulator.finish());
        return events;
    }

    private static List<ToolCall> toolCalls(List<StreamEvent> events) {
        List<ToolCall> calls = new ArrayList<>();
        for (StreamEvent event : events) {
            if (event.isToolCall()) {
                calls.add(event.getToolCall());
            }
        }
        return calls;
    }

    private static String textOf(List<StreamEvent> events) {
        StringBuilder text = new StringBuilder();
        for (StreamEvent event : events) {
            if (event.isTextDelta()) {
                text.append(event.getTextDelta());
            }
        }
        return text.toString();
    }

    // ------------------------------------------------------------------------------------
    // 5.1 拼接规则
    // ------------------------------------------------------------------------------------

    @Test
    public void threeDeltasComposeOneCompleteCall() {
        List<StreamEvent> events = feedAll(accumulator, specDeltaFrames());

        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals("call_001", calls.get(0).getId());
        assertEquals("get_weather", calls.get(0).getName());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
        // 拼接完成时参数文本必须是合法JSON
        assertEquals("北京", AiJsonUtils.fromJson(calls.get(0).getArguments(), Map.class).get("city"));
        assertTrue(textOf(events).isEmpty());
    }

    @Test
    public void recordedSseSampleComposeOneCompleteCall() {
        List<ToolCall> calls = toolCalls(feedAll(accumulator, AiFixtures.sseFrames("ai/openai/stream-tool-call.txt")));
        assertEquals(1, calls.size());
        assertEquals("call_001", calls.get(0).getId());
        assertEquals("get_weather", calls.get(0).getName());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void textOnlyStreamYieldsNoToolCall() {
        List<StreamEvent> events = feedAll(accumulator, AiFixtures.sseFrames("ai/openai/stream-text.txt"));
        assertTrue(toolCalls(events).isEmpty());
        assertEquals("北京今天晴，12 度。", textOf(events));
    }

    @Test
    public void argumentsSplitAtArbitraryBoundariesGiveSameResult() {
        // 逐字符切分参数文本，拼接结果与切分方式无关
        List<Map<String, Object>> frames = new ArrayList<>();
        frames.add(deltaFrame(null, Collections.singletonList(
                toolCallFragment(0, "call_001", "get_weather", "")), null));
        for (int i = 0; i < COMPLETE_ARGUMENTS.length(); i++) {
            frames.add(deltaFrame(null, Collections.singletonList(toolCallFragment(
                    0, null, null, String.valueOf(COMPLETE_ARGUMENTS.charAt(i)))), i == COMPLETE_ARGUMENTS.length() - 1 ? "tool_calls" : null));
        }
        List<ToolCall> calls = toolCalls(feedAll(new OpenAIStreamAccumulator(), frames));
        assertEquals(1, calls.size());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void textAndToolCallDeltasDoNotInterfere() {
        List<StreamEvent> events = feedAll(accumulator, AiFixtures.sseFrames("ai/openai/stream-text-and-tool-call.txt"));
        assertEquals("我来查一下。", textOf(events));
        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void multipleCallsAreBucketedByIndex() {
        List<ToolCall> calls = toolCalls(feedAll(accumulator, AiFixtures.sseFrames("ai/openai/stream-two-tool-calls.txt")));
        assertEquals(2, calls.size());
        assertEquals("call_001", calls.get(0).getId());
        assertEquals("{\"city\":\"北京\"}", calls.get(0).getArguments());
        assertEquals("call_002", calls.get(1).getId());
        assertEquals("{\"city\":\"上海\"}", calls.get(1).getArguments());
    }

    // ------------------------------------------------------------------------------------
    // 对外可见性（完整性时机）
    // ------------------------------------------------------------------------------------

    @Test
    public void partialDeltasAreNotVisibleAsToolCalls() {
        List<Map<String, Object>> frames = specDeltaFrames();
        List<StreamEvent> events = new ArrayList<>();
        for (int i = 0; i < frames.size() - 1; i++) {
            events.addAll(accumulator.feed(frames.get(i)));
        }
        assertTrue("半成品参数不得作为工具调用暴露给业务", toolCalls(events).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test
    public void completionSignalProducesExactlyOneCallEvent() {
        List<StreamEvent> events = feedAll(accumulator, specDeltaFrames());
        assertEquals(1, toolCalls(events).size());
    }

    @Test
    public void emptyInputYieldsNoEvents() {
        assertTrue(feedAll(new OpenAIStreamAccumulator(), Collections.<Map<String, Object>>emptyList()).isEmpty());
    }
}
