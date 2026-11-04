package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic的增量拼接器：按 content block 的 {@code index} 分桶。
 * <pre>
 *     {@code content_block_start} 给出该块的类型、工具调用标识与工具名；
 *     {@code content_block_delta} 里 {@code input_json_delta.partial_json} 按index连接为参数文本，
 *     {@code text_delta} 属于文本块、与工具块互不干扰，实时产出文本增量事件；
 *     {@code content_block_stop} 表示该index的工具调用已完整，此时才产出完整调用；
 *     {@code message_stop} 是流结束信号。
 *
 *     拼接过程中的半成品参数不会成为对外可见的事件；未收到结束信号即断开、
 *     参数文本不是合法JSON、单帧结构非法，三种情况都抛可诊断的协议异常。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
class AnthropicStreamAccumulator implements StreamAccumulator {

    private static final class Bucket {

        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }

    private final Map<Integer, Bucket> buckets = new LinkedHashMap<>();

    private boolean endSignalSeen;

    @Override
    public List<StreamEvent> feed(Map<String, Object> frame) {
        if (frame == null) {
            throw new StreamProtocolException("The streamed frame is null, cannot continue the accumulation.");
        }
        List<StreamEvent> events = new ArrayList<>();
        String type = AiMaps.getString(frame, "type");
        if (type == null) {
            throw new StreamProtocolException("Ill-formed streamed frame, the 'type' field is missing: " + frame);
        }
        if ("message_stop".equals(type)) {
            endSignalSeen = true;
            return events;
        }
        if ("content_block_start".equals(type)) {
            startBlock(frame);
            return events;
        }
        if ("content_block_delta".equals(type)) {
            return deltaEvents(frame);
        }
        if ("content_block_stop".equals(type)) {
            return completeBlock(AiMaps.getInteger(frame, "index"));
        }
        // content_block_stop之外的未知事件（ping、message_start、message_delta等）不影响拼接语义
        return events;
    }

    @Override
    public List<StreamEvent> finish() {
        if (!buckets.isEmpty()) {
            throw new StreamProtocolException(describePending());
        }
        return new ArrayList<>();
    }

    private void startBlock(Map<String, Object> frame) {
        Map<String, Object> block = AiMaps.getMap(frame, "content_block");
        if (!"tool_use".equals(AiMaps.getString(block, "type"))) {
            return;
        }
        Bucket bucket = bucket(indexOf(frame), true);
        bucket.id = AiMaps.getString(block, "id");
        bucket.name = AiMaps.getString(block, "name");
    }

    private List<StreamEvent> deltaEvents(Map<String, Object> frame) {
        Map<String, Object> delta = AiMaps.getMap(frame, "delta");
        String deltaType = AiMaps.getString(delta, "type");
        if ("text_delta".equals(deltaType)) {
            String text = AiMaps.getString(delta, "text");
            List<StreamEvent> events = new ArrayList<>(1);
            if (text != null && !text.isEmpty()) {
                events.add(StreamEvent.textDelta(text));
            }
            return events;
        }
        if ("input_json_delta".equals(deltaType)) {
            Object partial = delta.get("partial_json");
            if (partial != null) {
                bucket(indexOf(frame), true).arguments.append(partial);
            }
        }
        return new ArrayList<>();
    }

    private List<StreamEvent> completeBlock(Integer index) {
        Bucket bucket = buckets.remove(index == null ? 0 : index);
        List<StreamEvent> events = new ArrayList<>();
        if (bucket == null) {
            // 文本块（或未识别块）的结束，本身不产出工具调用
            return events;
        }
        if (bucket.name == null) {
            // 没有content_block_start就直接收到参数增量，属于结构非法，不能静默产出一个无名调用
            throw new StreamProtocolException("Ill-formed streamed frame, content_block_stop for index " + index
                    + " has no tool_use block started; accumulated arguments prefix=" + bucket.arguments);
        }
        String arguments = bucket.arguments.length() == 0 ? "{}" : bucket.arguments.toString();
        if (!isJsonObject(arguments)) {
            throw new StreamProtocolException("The arguments JSON of tool call [" + bucket.name
                    + "] is incomplete: " + arguments);
        }
        events.add(StreamEvent.toolCall(ToolCall.of(bucket.id, bucket.name, arguments)));
        return events;
    }

    private Bucket bucket(int index, boolean create) {
        Bucket bucket = buckets.get(index);
        if (bucket == null && create) {
            bucket = new Bucket();
            buckets.put(index, bucket);
        }
        return bucket;
    }

    private static int indexOf(Map<String, Object> frame) {
        Integer index = AiMaps.getInteger(frame, "index");
        return index == null ? 0 : index;
    }

    private String describePending() {
        List<String> descriptions = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets.values()) {
            descriptions.add("tool=" + bucket.name + ", accumulated arguments prefix=" + bucket.arguments);
        }
        String suffix = endSignalSeen ? "" : "; the stream ended before message_stop";
        return "The stream ended before the tool call was complete; " + String.join("; ", descriptions) + suffix;
    }

    private static boolean isJsonObject(String text) {
        try {
            return AiJsonUtils.fromJson(text, Map.class) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
