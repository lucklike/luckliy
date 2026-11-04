package com.luckyframework.httpclient.proxy.sse.standard;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link StandardEventListener}的事件流行解析测试
 */
public class StandardEventListenerTest {

    /**
     * 一个事件中的多行data应按顺序用换行符拼接，不能只保留最后一行
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void multipleDataLinesShouldBeJoinedByNewline() throws Exception {
        Message message = singleEvent("data: first", "data:second", "data", "data: fourth");
        assertTrue(message.hasData());
        assertEquals("first\nsecond\n\nfourth", message.getData());
    }

    /**
     * 值前的第一个空格是字段分隔符不算作值本身，多个空格时只去掉一个
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void onlyTheFirstLeadingSpaceOfValueShouldBeRemoved() throws Exception {
        Message message = singleEvent("data:  x", "id: 7", "event: myEvent", "retry: 1000");
        assertEquals(" x", message.getData());
        assertEquals("7", message.getId());
        assertEquals("myEvent", message.getEvent());
        assertEquals("1000", message.getRetry());
    }

    /**
     * 除data之外的字段重复出现时仍是后值覆盖前值
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void repeatedNonDataFieldShouldBeOverwritten() throws Exception {
        Message message = singleEvent("event: a", "event: b", "id: 1", "id: 2");
        assertEquals("b", message.getEvent());
        assertEquals("2", message.getId());
    }

    /**
     * 多行注释需全部保留，且注释行不会产生data
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void commentLinesShouldBeAccumulated() throws Exception {
        Message message = singleEvent(":ping", ": pong");
        assertEquals("ping\npong", message.getComment());
        assertFalse(message.hasData());
    }

    /**
     * 最后一帧没有以空行结束时，流正常结束需要补发且只发一次
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void trailingFrameWithoutBlankLineShouldBeFlushedOnCompleted() throws Exception {
        Recorder recorder = new Recorder();
        recorder.onOpen(null);
        recorder.onText("data: a");
        recorder.onText("data: b");
        recorder.onCompleted();
        recorder.onClose();
        assertEquals(1, recorder.messages.size());
        assertEquals("a\nb", recorder.messages.get(0).getData());
    }

    /**
     * 已按空行派发过事件后，流正常结束时不应再补发一次
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void dispatchedEventShouldNotBeFlushedAgain() throws Exception {
        Recorder recorder = new Recorder();
        recorder.onOpen(null);
        recorder.onText("data: only");
        recorder.onText("");
        recorder.onCompleted();
        recorder.onClose();
        assertEquals(1, recorder.messages.size());
        assertEquals("only", recorder.messages.get(0).getData());
    }

    /**
     * 已交付的消息不能因为后续事件重建缓冲区而丢失内容
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void deliveredMessageShouldNotBeClearedByNextEvent() throws Exception {
        Recorder recorder = new Recorder();
        recorder.onOpen(null);
        recorder.onText("data: first");
        recorder.onText("");
        recorder.onText("data: second");
        recorder.onText("");
        recorder.onClose();
        assertEquals("first", recorder.messages.get(0).getData());
        assertEquals("second", recorder.messages.get(1).getData());
    }

    /**
     * 未走响应转换器（方法上下文尚未注入）时直接调用onText不应抛出空指针异常
     *
     * @throws Exception 解析过程可能出现的异常
     */
    @Test
    public void textWithoutContextShouldNotThrow() throws Exception {
        Recorder recorder = new Recorder();
        recorder.onText("data: lazy");
        recorder.onCompleted();
        recorder.onClose();
        assertEquals(1, recorder.messages.size());
        assertEquals("lazy", recorder.messages.get(0).getData());
    }

    /**
     * 以空行结尾推送一个完整事件并返回该事件
     *
     * @param lines 事件流中的各行（不含结尾空行）
     * @return 解析出的消息
     * @throws Exception 解析过程可能出现的异常
     */
    private static Message singleEvent(String... lines) throws Exception {
        List<Message> messages = parse(lines);
        assertEquals(1, messages.size());
        return messages.get(0);
    }

    /**
     * 按行驱动监听器解析并返回收集到的所有消息
     *
     * @param lines 事件流中的各行
     * @return 收集到的消息
     * @throws Exception 解析过程可能出现的异常
     */
    private static List<Message> parse(String... lines) throws Exception {
        Recorder recorder = new Recorder();
        recorder.onOpen(null);
        for (String line : lines) {
            recorder.onText(line);
        }
        // 各行中未包含结尾空行时，由流正常结束补发最后一帧
        recorder.onCompleted();
        recorder.onClose();
        return recorder.messages;
    }

    /**
     * 只负责记录收到消息的监听器
     */
    private static class Recorder extends StandardEventListener {

        private final List<Message> messages = new ArrayList<>();

        @Override
        protected void onMessage(Message message) {
            messages.add(message);
        }
    }

}
