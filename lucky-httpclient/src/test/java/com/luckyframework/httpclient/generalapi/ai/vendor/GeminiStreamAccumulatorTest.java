package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Gemini的增量拼接器：{@code candidates[].content.parts[]} 里的 {@code functionCall} 单帧即完整，
 * 收到就产出完整调用；文本分片与调用分片混合时按到达顺序透传；报文缺少调用标识时合成唯一标识。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class GeminiStreamAccumulatorTest {

    private final GeminiStreamAccumulator accumulator = new GeminiStreamAccumulator();

    // ------------------------------------------------------------------------------------
    // 帧构造辅助
    // ------------------------------------------------------------------------------------

    private static Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", text);
        return part;
    }

    private static Map<String, Object> functionCallPart(String name, Map<String, Object> args) {
        Map<String, Object> functionCall = new LinkedHashMap<>();
        functionCall.put("name", name);
        functionCall.put("args", args);
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("functionCall", functionCall);
        return part;
    }

    private static Map<String, Object> cityArgs(String city) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("city", city);
        return args;
    }

    private static Map<String, Object> frame(List<Map<String, Object>> parts) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "model");
        content.put("parts", parts);
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("content", content);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("candidates", Collections.singletonList(candidate));
        return frame;
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

    private static List<String> texts(List<StreamEvent> events) {
        List<String> texts = new ArrayList<>();
        for (StreamEvent event : events) {
            if (event.isTextDelta()) {
                texts.add(event.getTextDelta());
            }
        }
        return texts;
    }

    // ------------------------------------------------------------------------------------
    // 5.3 拼接规则
    // ------------------------------------------------------------------------------------

    @Test
    public void recordedFramesProduceOneCallAndOrderedText() {
        List<StreamEvent> events = feedAll(new GeminiStreamAccumulator(), AiFixtures.sseFrames("ai/gemini/stream-tool-call.txt"));
        assertEquals(Arrays.asList("北京", "今天晴"), texts(events));
        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals("get_weather", calls.get(0).getName());
        assertEquals(cityArgs("北京"), castArguments(calls.get(0).getArguments()));
        assertTrue(calls.get(0).getId().startsWith(GeminiDialect.SYNTHESIZED_CALL_ID_PREFIX));
    }

    @Test
    public void singleFrameFunctionCallIsCompleteImmediately() {
        List<StreamEvent> events = accumulator.feed(frame(Collections.singletonList(
                functionCallPart("get_weather", cityArgs("北京")))));
        assertEquals("Gemini单帧即完整，无需等待结束信号", 1, toolCalls(events).size());
        assertEquals(cityArgs("北京"), castArguments(toolCalls(events).get(0).getArguments()));
    }

    @Test
    public void textAndCallPartsKeepArrivalOrder() {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(textPart("我来"));
        parts.add(functionCallPart("get_weather", cityArgs("北京")));
        parts.add(textPart("查一下。"));
        List<StreamEvent> events = accumulator.feed(frame(parts));

        assertEquals(3, events.size());
        assertTrue(events.get(0).isTextDelta());
        assertTrue(events.get(1).isToolCall());
        assertTrue(events.get(2).isTextDelta());
        assertEquals("我来查一下。", String.join("", texts(events)));
    }

    @Test
    public void synthesizedCallIdIsNonEmptyAndUniqueWithinResponse() {
        List<StreamEvent> events = new ArrayList<>();
        events.addAll(accumulator.feed(frame(Collections.singletonList(
                functionCallPart("get_weather", cityArgs("北京"))))));
        events.addAll(accumulator.feed(frame(Collections.singletonList(
                functionCallPart("get_weather", cityArgs("上海"))))));
        List<ToolCall> calls = toolCalls(events);
        assertEquals(2, calls.size());
        assertNotNull(calls.get(0).getId());
        assertTrue(calls.get(0).getId(), calls.get(0).getId().startsWith(GeminiDialect.SYNTHESIZED_CALL_ID_PREFIX));
        assertNotEquals(calls.get(0).getId(), calls.get(1).getId());
    }

    @Test
    public void frameWithoutCandidatesYieldsNothing() {
        assertTrue(accumulator.feed(new LinkedHashMap<String, Object>()).isEmpty());
        assertTrue(accumulator.finish().isEmpty());
    }

    @Test
    public void emptyInputYieldsNoEvents() {
        assertTrue(feedAll(new GeminiStreamAccumulator(), Collections.<Map<String, Object>>emptyList()).isEmpty());
    }

    @Test
    public void illFormedFrameThrowsProtocolException() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("candidates", Collections.singletonList("not-a-json-object"));
        try {
            accumulator.feed(frame);
            fail("candidates[0] 不是对象时应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Ill-formed"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castArguments(String arguments) {
        return (Map<String, Object>) AiJsonUtils.fromJson(arguments, Map.class);
    }
}
