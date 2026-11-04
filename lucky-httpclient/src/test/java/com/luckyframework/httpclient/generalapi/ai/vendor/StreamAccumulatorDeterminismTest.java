package com.luckyframework.httpclient.generalapi.ai.vendor;

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
 * 5.4 拼接的两条契约：确定性（同一帧序列重复执行结果一致，不依赖网络/时间/随机数）
 * 与异常终止的确定性（未给出结束信号即断开、参数JSON不完整、单帧结构非法，
 * 三种情况都抛出含工具名与已拼接参数前缀的协议异常，绝不返回截断的调用）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StreamAccumulatorDeterminismTest {

    private static List<String> describe(List<StreamEvent> events) {
        List<String> descriptions = new ArrayList<>(events.size());
        for (StreamEvent event : events) {
            descriptions.add(event.toString());
        }
        return descriptions;
    }

    private static List<String> run(StreamAccumulator accumulator, List<Map<String, Object>> frames) {
        List<StreamEvent> events = new ArrayList<>();
        for (Map<String, Object> frame : frames) {
            events.addAll(accumulator.feed(frame));
        }
        events.addAll(accumulator.finish());
        return describe(events);
    }

    private static Map<String, Object> openAIFrame(String arguments, String finishReason) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("arguments", arguments);
        Map<String, Object> fragment = new LinkedHashMap<>();
        fragment.put("index", 0);
        fragment.put("id", "call_001");
        fragment.put("function", function);
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("tool_calls", Collections.singletonList(fragment));
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("choices", Collections.singletonList(choice));
        return frame;
    }

    private static Map<String, Object> openAIFirstFrame() {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "get_weather");
        function.put("arguments", "");
        Map<String, Object> fragment = new LinkedHashMap<>();
        fragment.put("index", 0);
        fragment.put("id", "call_001");
        fragment.put("function", function);
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("tool_calls", Collections.singletonList(fragment));
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", null);
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("choices", Collections.singletonList(choice));
        return frame;
    }

    private static Map<String, Object> anthropicFrame(Object... keyValues) {
        Map<String, Object> frame = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            frame.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return frame;
    }

    private static Map<String, Object> anthropicBlockStart(int index, String name) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_use");
        block.put("id", "toolu_001");
        block.put("name", name);
        return anthropicFrame("type", "content_block_start", "index", index, "content_block", block);
    }

    private static Map<String, Object> anthropicJsonDelta(int index, String partialJson) {
        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("type", "input_json_delta");
        delta.put("partial_json", partialJson);
        return anthropicFrame("type", "content_block_delta", "index", index, "delta", delta);
    }

    // ------------------------------------------------------------------------------------
    // 确定性：同一帧序列重复执行（每次新建实例）结果逐字段一致
    // ------------------------------------------------------------------------------------

    @Test
    public void openAIAccumulationIsRepeatable() {
        List<Map<String, Object>> frames = AiFixtures.sseFrames("ai/openai/stream-tool-call.txt");
        List<String> first = run(new OpenAIStreamAccumulator(), frames);
        List<String> second = run(new OpenAIStreamAccumulator(), frames);
        assertEquals(first, second);
        assertEquals(1, first.size());
    }

    @Test
    public void anthropicAccumulationIsRepeatable() {
        List<Map<String, Object>> frames = AiFixtures.sseFrames("ai/anthropic/stream-tool-call.txt");
        List<String> first = run(new AnthropicStreamAccumulator(), frames);
        List<String> second = run(new AnthropicStreamAccumulator(), frames);
        assertEquals(first, second);
        assertEquals(2, first.size());
    }

    @Test
    public void geminiAccumulationIsRepeatable() {
        List<Map<String, Object>> frames = AiFixtures.sseFrames("ai/gemini/stream-tool-call.txt");
        List<String> first = run(new GeminiStreamAccumulator(), frames);
        List<String> second = run(new GeminiStreamAccumulator(), frames);
        assertEquals(first, second);
        assertEquals(3, first.size());
    }

    // ------------------------------------------------------------------------------------
    // 异常终止
    // ------------------------------------------------------------------------------------

    @Test
    public void openAITruncatedStreamThrowsWithToolNameAndPrefix() {
        // 只喂前两段增量（未给出结束信号），随后流被关闭
        StreamAccumulator accumulator = new OpenAIStreamAccumulator();
        accumulator.feed(openAIFirstFrame());
        accumulator.feed(openAIFrame("{\"city\"", null));
        try {
            accumulator.finish();
            fail("未收到结束信号即断开时应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("{\"city\""));
        }
    }

    @Test
    public void openAIIncompleteArgumentsAtEndSignalThrows() {
        StreamAccumulator accumulator = new OpenAIStreamAccumulator();
        accumulator.feed(openAIFirstFrame());
        try {
            accumulator.feed(openAIFrame("{\"city\":", "tool_calls"));
            fail("结束信号到来时参数JSON仍不完整应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("incomplete"));
        }
    }

    @Test
    public void anthropicTruncatedStreamThrowsWithToolNameAndPrefix() {
        StreamAccumulator accumulator = new AnthropicStreamAccumulator();
        accumulator.feed(anthropicBlockStart(1, "get_weather"));
        accumulator.feed(anthropicJsonDelta(1, "{\"city\": \"北"));
        try {
            accumulator.finish();
            fail("未收到 content_block_stop/message_stop 时应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("{\"city\": \"北"));
        }
    }

    @Test
    public void anthropicIncompleteArgumentsAtBlockStopThrows() {
        StreamAccumulator accumulator = new AnthropicStreamAccumulator();
        accumulator.feed(anthropicBlockStart(1, "get_weather"));
        accumulator.feed(anthropicJsonDelta(1, "{\"city\":"));
        try {
            accumulator.feed(anthropicFrame("type", "content_block_stop", "index", 1));
            fail("content_block_stop 时参数JSON仍不完整应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("incomplete"));
        }
    }

    @Test
    public void illFormedFrameThrowsForEveryVendor() {
        assertIllFormed(new OpenAIStreamAccumulator(), Collections.singletonList("not-a-json-object"), "choices");
        assertIllFormed(new AnthropicStreamAccumulator(), Collections.singletonList("not-a-json-object"), "type");
        assertIllFormed(new GeminiStreamAccumulator(), Collections.singletonList("not-a-json-object"), "candidates");

        try {
            new OpenAIStreamAccumulator().feed(null);
            fail("空帧应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("null"));
        }
    }

    private void assertIllFormed(StreamAccumulator accumulator, List<Object> nodes, String expectedKeyword) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("choices", nodes);
        frame.put("candidates", nodes);
        frame.put("type", null);
        try {
            accumulator.feed(frame);
            fail("单帧结构非法时应抛协议异常");
        } catch (StreamProtocolException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Ill-formed"));
            assertTrue(e.getMessage(), e.getMessage().contains(expectedKeyword));
        }
    }
}
