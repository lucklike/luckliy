package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gemini的增量拼接器：每一帧都是「generateContent响应的一部分」，
 * {@code candidates[].content.parts[]} 里的 {@code functionCall} 单帧即完整，收到就产出完整调用；
 * {@code text} 分片按到达顺序实时产出文本增量事件，文本与调用混排时顺序保持。
 * 调用标识同样由Gemini报文缺失，按 {@link GeminiDialect} 的规则合成。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
class GeminiStreamAccumulator implements StreamAccumulator {

    private static final String PART_FUNCTION_CALL = "functionCall";

    /**
     * 已合成的调用标识数量，保证同一轮流式响应内的标识互不相同
     */
    private int synthesizedCount;

    @Override
    public List<StreamEvent> feed(Map<String, Object> frame) {
        if (frame == null) {
            throw new StreamProtocolException("The streamed frame is null, cannot continue the accumulation.");
        }
        List<Object> candidates = AiMaps.getList(frame, "candidates");
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        if (!(candidates.get(0) instanceof Map)) {
            throw new StreamProtocolException("Ill-formed streamed frame, candidates[0] is not a JSON object: " + frame);
        }
        List<StreamEvent> events = new ArrayList<>();
        for (Map<String, Object> part : AiMaps.toMapList(AiMaps.getList(AiMaps.getMap(candidates.get(0), "content"), "parts"))) {
            String text = AiMaps.getString(part, "text");
            if (text != null && !text.isEmpty()) {
                events.add(StreamEvent.textDelta(text));
            }
            if (part.get(PART_FUNCTION_CALL) != null && !(part.get(PART_FUNCTION_CALL) instanceof Map)) {
                throw new StreamProtocolException("Ill-formed streamed frame, parts[].functionCall is not a JSON object: " + part);
            }
            Map<String, Object> functionCall = AiMaps.getMap(part, PART_FUNCTION_CALL);
            if (!functionCall.isEmpty()) {
                String name = AiMaps.getString(functionCall, "name");
                String arguments = AiJsonUtils.toJson(AiMaps.getMap(functionCall, "args"));
                events.add(StreamEvent.toolCall(ToolCall.of(
                        GeminiDialect.SYNTHESIZED_CALL_ID_PREFIX + (synthesizedCount++) + "-" + name, name, arguments)));
            }
        }
        return events;
    }

    @Override
    public List<StreamEvent> finish() {
        return new ArrayList<>();
    }
}
