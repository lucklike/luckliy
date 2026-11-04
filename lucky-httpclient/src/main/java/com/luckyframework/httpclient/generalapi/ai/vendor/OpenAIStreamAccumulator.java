package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI兼容簇的增量拼接器：工具调用片段位于 {@code choices[0].delta.tool_calls[]}，
 * 按 {@code index} 分桶，{@code id} 与 {@code function.name} 只在该index的首个片段出现，
 * {@code function.arguments} 的增量按到达顺序做字符串连接；文本增量与工具调用增量互不影响。
 * 工具调用只在 {@code finish_reason} 到来时一次性产出，半成品参数不对外可见。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
class OpenAIStreamAccumulator implements StreamAccumulator {

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
        List<Object> choices = AiMaps.getList(frame, "choices");
        if (choices.isEmpty()) {
            return events;
        }
        if (!(choices.get(0) instanceof Map)) {
            throw new StreamProtocolException("Ill-formed streamed frame, choices[0] is not a JSON object: " + frame);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);

        Map<String, Object> delta = readDelta(firstChoice);
        String content = AiMaps.getString(delta, "content");
        if (content != null && !content.isEmpty()) {
            events.add(StreamEvent.textDelta(content));
        }
        collectToolCallDeltas(delta);

        if (!endSignalSeen && AiMaps.contains(firstChoice, "finish_reason") && firstChoice.get("finish_reason") != null) {
            endSignalSeen = true;
            events.addAll(flushCompletedCalls());
        }
        return events;
    }

    @Override
    public List<StreamEvent> finish() {
        if (!buckets.isEmpty()) {
            throw new StreamProtocolException(describePending());
        }
        return new ArrayList<>();
    }

    private Map<String, Object> readDelta(Map<String, Object> firstChoice) {
        if (AiMaps.contains(firstChoice, "delta") && !(firstChoice.get("delta") instanceof Map)) {
            throw new StreamProtocolException("Ill-formed streamed frame, delta is not a JSON object: " + firstChoice);
        }
        return AiMaps.getMap(firstChoice, "delta");
    }

    private void collectToolCallDeltas(Map<String, Object> delta) {
        if (AiMaps.contains(delta, "tool_calls") && !(delta.get("tool_calls") instanceof List)) {
            throw new StreamProtocolException("Ill-formed streamed frame, delta.tool_calls is not an array: " + delta);
        }
        for (Object node : AiMaps.getList(delta, "tool_calls")) {
            if (!(node instanceof Map)) {
                throw new StreamProtocolException("Ill-formed streamed frame, a tool_call fragment is not a JSON object: " + node);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> fragment = (Map<String, Object>) node;
            Integer index = AiMaps.getInteger(fragment, "index");
            Bucket bucket = buckets.computeIfAbsent(index == null ? 0 : index, key -> new Bucket());
            if (fragment.get("id") != null) {
                bucket.id = String.valueOf(fragment.get("id"));
            }
            if (AiMaps.contains(fragment, "function") && !(fragment.get("function") instanceof Map)) {
                throw new StreamProtocolException("Ill-formed streamed frame, tool_call.function is not a JSON object: " + fragment);
            }
            Map<String, Object> function = AiMaps.getMap(fragment, "function");
            if (function.get("name") != null) {
                bucket.name = String.valueOf(function.get("name"));
            }
            Object arguments = function.get("arguments");
            if (arguments != null) {
                bucket.arguments.append(arguments);
            }
        }
    }

    private List<StreamEvent> flushCompletedCalls() {
        List<StreamEvent> events = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets.values()) {
            if (bucket.name == null && bucket.id == null) {
                continue;
            }
            String arguments = bucket.arguments.length() == 0 ? "{}" : bucket.arguments.toString();
            if (!isJsonObject(arguments)) {
                buckets.clear();
                throw new StreamProtocolException("The arguments JSON of tool call [" + bucket.name + "] is incomplete: " + arguments);
            }
            events.add(StreamEvent.toolCall(ToolCall.of(bucket.id, bucket.name, arguments)));
        }
        buckets.clear();
        return events;
    }

    private String describePending() {
        List<String> descriptions = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets.values()) {
            descriptions.add("tool=" + bucket.name + ", accumulated arguments prefix=" + bucket.arguments);
        }
        return "The stream ended before the tool call was complete; " + String.join("; ", descriptions);
    }

    private static boolean isJsonObject(String text) {
        try {
            return AiJsonUtils.fromJson(text, Map.class) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
