package com.luckyframework.httpclient.generalapi.ai.vendor;

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
import static org.junit.Assert.fail;

/**
 * Anthropic增量拼接器：{@code content_block_start} 给出该块的标识与工具名，
 * {@code content_block_delta} 的 {@code input_json_delta.partial_json} 按index连接，
 * 只有 {@code content_block_stop} 才产出完整工具调用，文本块与工具块分桶处理、互不污染。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class AnthropicStreamAccumulatorTest {

    private static final String COMPLETE_ARGUMENTS = "{\"city\": \"北京\"}";

    private final AnthropicStreamAccumulator accumulator = new AnthropicStreamAccumulator();

    // ------------------------------------------------------------------------------------
    // 帧构造辅助
    // ------------------------------------------------------------------------------------

    private static Map<String, Object> frame(Object... keyValues) {
        Map<String, Object> frame = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            frame.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return frame;
    }

    private static Map<String, Object> blockStart(int index, String id, String name) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_use");
        block.put("id", id);
        block.put("name", name);
        block.put("input", new LinkedHashMap<String, Object>());
        return frame("type", "content_block_start", "index", index, "content_block", block);
    }

    private static Map<String, Object> jsonDelta(int index, String partialJson) {
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("type", "input_json_delta");
        delta.put("partial_json", partialJson);
        return frame("type", "content_block_delta", "index", index, "delta", delta);
    }

    private static Map<String, Object> textDelta(int index, String text) {
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("type", "text_delta");
        delta.put("text", text);
        return frame("type", "content_block_delta", "index", index, "delta", delta);
    }

    private static Map<String, Object> blockStop(int index) {
        return frame("type", "content_block_stop", "index", index);
    }

    /**
     * spec 样例的四个事件
     */
    private static List<Map<String, Object>> specEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(blockStart(1, "toolu_001", "get_weather"));
        events.add(jsonDelta(1, "{\"city\": \"北"));
        events.add(jsonDelta(1, "京\"}"));
        events.add(blockStop(1));
        return events;
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
    // 5.2 拼接规则
    // ------------------------------------------------------------------------------------

    @Test
    public void twoJsonDeltasComposeOneCallAtBlockStop() {
        List<StreamEvent> events = feedAll(accumulator, specEvents());
        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals("toolu_001", calls.get(0).getId());
        assertEquals("get_weather", calls.get(0).getName());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void idAndNameComeFromContentBlockStart() {
        // content_block_start 本身不产出任何事件，只把标识与工具名登记到该index的桶上
        assertTrue(accumulator.feed(blockStart(3, "toolu_003", "lookup_city")).isEmpty());
        accumulator.feed(jsonDelta(3, "{\"city\": \"北京\"}"));
        List<ToolCall> calls = toolCalls(accumulator.feed(blockStop(3)));
        assertEquals(1, calls.size());
        assertEquals("toolu_003", calls.get(0).getId());
        assertEquals("lookup_city", calls.get(0).getName());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void halfBuiltArgumentsAreInvisibleBeforeBlockStop() {
        List<Map<String, Object>> events = specEvents();
        List<StreamEvent> visible = new ArrayList<>();
        for (int i = 0; i < events.size() - 1; i++) {
            visible.addAll(accumulator.feed(events.get(i)));
        }
        assertTrue("拼接过程中不得对外暴露工具调用", toolCalls(visible).isEmpty());
        assertTrue(visible.isEmpty());

        List<StreamEvent> stopped = accumulator.feed(events.get(events.size() - 1));
        assertEquals("收到结束信号时产出且仅产出一次", 1, toolCalls(stopped).size());
    }

    @Test
    public void textBlockAndToolBlockCoexistWithoutPollution() {
        List<Map<String, Object>> frames = new ArrayList<>();
        frames.add(frame("type", "content_block_start", "index", 0,
                "content_block", frame("type", "text", "text", "")));
        frames.add(textDelta(0, "我来"));
        frames.add(blockStart(1, "toolu_001", "get_weather"));
        frames.add(jsonDelta(1, "{\"city\": "));
        frames.add(textDelta(0, "查一下。"));
        frames.add(jsonDelta(1, "\"北京\"}"));
        frames.add(blockStop(0));
        frames.add(blockStop(1));
        frames.add(frame("type", "message_stop"));

        List<StreamEvent> events = feedAll(accumulator, frames);
        assertEquals("文本增量实时输出", "我来查一下。", String.join("", texts(events)));
        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void blockStopWithoutToolUseStartThrows() {
        // 没有 content_block_start 就直接收到参数增量，不能静默产出一个无名调用
        accumulator.feed(jsonDelta(1, "{\"city\": \"北京\"}"));
        try {
            accumulator.feed(blockStop(1));
            fail("单帧结构非法时应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Ill-formed"));
            assertTrue(e.getMessage(), e.getMessage().contains("{\"city\": \"北京\"}"));
        }
    }

    @Test
    public void recordedEventStreamMatchesSpecSample() {
        List<StreamEvent> events = feedAll(accumulator, AiFixtures.sseFrames("ai/anthropic/stream-tool-call.txt"));
        assertEquals(Collections.singletonList("我来查一下。"), texts(events));
        List<ToolCall> calls = toolCalls(events);
        assertEquals(1, calls.size());
        assertEquals("toolu_001", calls.get(0).getId());
        assertEquals("get_weather", calls.get(0).getName());
        assertEquals(COMPLETE_ARGUMENTS, calls.get(0).getArguments());
    }

    @Test
    public void emptyInputYieldsNoEvents() {
        assertTrue(feedAll(new AnthropicStreamAccumulator(), Collections.<Map<String, Object>>emptyList()).isEmpty());
    }
}
