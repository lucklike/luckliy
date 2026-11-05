package com.luckyframework.httpclient.generalapi.ai.vendor;

import java.util.List;
import java.util.Map;

/**
 * 流式增量拼接器：把「已解析出的单帧Map」序列确定性地拼接为文本增量与完整工具调用。
 * 契约是纯数据变换——不依赖网络、系统时间与随机数，因此可以直接喂固定帧列表做离线断言。
 * 每个厂商一个实现，由 {@link VendorAdapter#newAccumulator()} 按轮流式调用各建一个实例。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
public interface StreamAccumulator {

    /**
     * 喂入一帧已解析的单帧数据，返回该帧产生的事件（半成品参数不作为事件对外可见）
     *
     * @param frame 单帧报文解析出的Map
     * @return 该帧产生的事件列表，无事件时返回空列表
     * @throws StreamProtocolException 单帧结构非法
     */
    List<StreamEvent> feed(Map<String, Object> frame);

    /**
     * 事件流结束时调用，校验是否收到过结束信号；
     * 未给出结束信号或参数JSON仍不完整时抛出协议异常
     *
     * @return 收尾阶段产出的事件列表，通常为空列表
     * @throws StreamProtocolException 流在被截断的位置提前结束
     */
    List<StreamEvent> finish();
}
