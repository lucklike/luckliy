package com.luckyframework.httpclient.proxy.sse.standard;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.sse.ReconnectionEventListener;
import com.luckyframework.httpclient.proxy.sse.SseException;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.sse.standard.SseConstant.COMMENT;
import static com.luckyframework.httpclient.proxy.sse.standard.SseConstant.DATA;

/**
 * SSE标准数据格式的事件监听器（text/event-stream）
 * <p>
 * 行解析规则对齐HTML规范的Server-sent events事件流解析：字段名取首个冒号之前的内容，
 * 值取冒号之后的内容并去掉值前的一个空格；不含冒号的行整行为字段名且值为空串；
 * {@code data}与注释的多行值按行拼接，其余字段后值覆盖前值。
 */
public abstract class StandardEventListener extends ReconnectionEventListener {

    /**
     * 消息集合
     */
    private final Map<MethodContext, Properties> props = new ConcurrentHashMap<>();

    /**
     * 上下文缺失时的兜底缓冲区
     * <p>
     * 监听器可能绕过{@link com.luckyframework.httpclient.proxy.sse.SseResponseConvert}被直接调用
     * {@link #onText(String)}（如回环监听器向业务监听器透传增量数据），此时尚未注入方法上下文，
     * 用本缓冲区兜底避免以null作为{@link ConcurrentHashMap}的key。
     */
    private Properties fallbackProps = new Properties();

    @Override
    public final void onOpen(Response response) throws Exception {
        resetProperties();
        onOpening(response);
    }

    @Override
    public final void onClose() {
        MethodContext context = getContext();
        if (context != null) {
            props.remove(context);
        }
        fallbackProps = new Properties();
        onClosed();
    }

    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息事件
     */
    @Override
    public final void onText(String message) throws Exception {
        Properties properties = currentProperties();
        // 消息处理，遇到空行之前收集消息，遇到空行时处理消息
        if (!StringUtils.hasText(message)) {
            onMessage(new Message(properties));
            resetProperties();
            return;
        }
        int index = message.indexOf(":");
        if (index == -1) {
            // 规范中不含冒号的行整行即字段名，值为空串（如单独一行的data代表一个空数据行）
            putProperty(properties, message, "");
            return;
        }
        String value = message.substring(index + 1);
        // 规范中值前的第一个空格属于分隔符不算作值本身（多个空格只去第一个）
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        putProperty(properties, message.substring(0, index), value);
    }

    /**
     * 写入一条消息属性
     * <p>
     * SSE规范允许一个事件中出现多行同名字段，但不同字段的合并规则不一样：
     * <ul>
     *     <li>{@code data}：多行的值按出现顺序用换行符拼接成一个整体（服务端推送多行数据时
     *     会拆成多个{@code data:}/{@code data}行输出），所以此处做累加而不是覆盖，避免前面的数据丢失；</li>
     *     <li>注释行（字段名为空串）：规范中直接丢弃，但本监听器通过{@link Message#getComment()}
     *     对外暴露，所以同样做累加保留全部注释；</li>
     *     <li>{@code event}/{@code id}/{@code retry}等：规范中仍是后值覆盖前值，保持原有覆盖语义。</li>
     * </ul>
     *
     * @param properties 当前事件的属性集合
     * @param key        字段名
     * @param value      字段值
     */
    private void putProperty(Properties properties, String key, String value) {
        if (DATA.equals(key) || COMMENT.equals(key)) {
            String exists = properties.getProperty(key);
            if (exists != null) {
                value = exists + "\n" + value;
            }
        }
        properties.put(key, value);
    }

    /**
     * 获取当前上下文的消息属性缓冲区，不存在时新建
     *
     * @return 消息属性缓冲区
     */
    private Properties currentProperties() {
        MethodContext context = getContext();
        return context == null ? fallbackProps : props.computeIfAbsent(context, key -> new Properties());
    }

    /**
     * 丢弃当前缓冲区并重建一个新的
     * <p>
     * 必须换对象而不能原地清空：已交付给业务的{@link Message}与缓冲区共享同一个
     * {@link Properties}实例，异步读取时不能被后续事件影响。
     */
    private void resetProperties() {
        MethodContext context = getContext();
        if (context == null) {
            fallbackProps = new Properties();
        } else {
            props.put(context, new Properties());
        }
    }

    /**
     * 正常结束时触发
     * <p>
     * SSE规范中一个事件以空行作为结束标志，但服务端可能在推送完最后一帧后
     * 未补发空行就直接关闭流（EOF）。此时{@link #onText(String)}不会派发缓冲区中
     * 残留的最后一帧，导致消息丢失，因此在流正常结束时主动flush一次。
     * <p>
     * 注意：网络异常中断会走{@link #onError(Throwable)}而不会到达此处，
     * 因此这里flush的一定是正常结束时的完整尾帧。
     */
    @Override
    public final void onCompleted() {
        Properties properties = currentProperties();
        // 缓冲区仍有残留数据，说明最后一帧没有以空行结束，需要在此补发
        if (!properties.isEmpty()) {
            try {
                onMessage(new Message(properties));
            } catch (Exception e) {
                throw new SseException(e);
            } finally {
                resetProperties();
            }
        }
        onCompleting();
    }


    /**
     * 接收到服务器的消息时触发
     *
     * @param message 消息
     * @throws Exception 消息处理过程中可能出现的异常
     */
    protected abstract void onMessage(Message message) throws Exception;

    /**
     * 当连接建立时触发
     *
     * @param response 响应对象
     */
    protected void onOpening(Response response) throws Exception {

    }

    /**
     * 当流正常结束时触发（在残留尾帧flush之后）
     */
    protected void onCompleting() {

    }

    /**
     * 当连接关闭时触发
     */
    protected void onClosed() {

    }

}
