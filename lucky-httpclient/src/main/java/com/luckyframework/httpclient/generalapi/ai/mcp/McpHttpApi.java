package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.proxy.annotations.Header;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.logging.MaskType;
import com.luckyframework.httpclient.proxy.logging.Masker;

import java.util.Map;

/**
 * MCP Streamable HTTP传输的内部接口：把一条JSON-RPC消息POST到配置的地址。
 * <pre>
 *     走注解API而不是自写一层连接，是为了复用既有HTTP栈的重试、超时、日志脱敏与Mock能力。
 *     地址来自配置（{@code ai.mcp.url}，由 {@link StreamableHttpTransport} 为每个server单独装载），
 *     配置里的附加请求头（含凭据）与会话标识头合并成一个头Map由方法参数传入，
 *     注解里不出现任何常量地址与密钥；会话标识不存在时该键干脆不出现在Map里，而不是写入空值。
 *
 *     响应统一交给 {@link McpHttpResponseConvert} 摊平成 {@link McpHttpResponse}，
 *     因为传输层需要看到原始的状态码、Content-Type与响应体文本才能分支。
 *
 *     凭据保护复用框架既有的按名脱敏链路：注解里只声明「哪些头名属于凭据」（而非凭据值本身），
 *     调试日志输出这些头时整个值被替换为占位符，其余请求信息照常可读。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
@ServerAddress(url = "#{ai." + McpHttpApi.URL_CONFIG_PATH + "}")
public interface McpHttpApi {

    /**
     * 地址在AI配置里的键路径（完整表达式为 {@code ai.mcp.url}）
     */
    String URL_CONFIG_PATH = "mcp.url";

    /**
     * 发送一条JSON-RPC消息
     *
     * @param headers 本次请求的全部附加头：配置里的请求头（如Authorization）与会话标识头，键即头名
     * @param frame   JSON-RPC消息对象
     * @return 状态码、Content-Type、会话标识头与响应体文本的快照
     */
    @Post
    @StaticHeader("Accept: " + StreamableHttpTransport.ACCEPT_HEADER_VALUE)
    @Masker(keys = {"Authorization", "Proxy-Authorization", "Cookie"}, type = MaskType.FULL)
    @ResultConvertMeta(convert = @ObjectGenerate(McpHttpResponseConvert.class))
    McpHttpResponse post(@Header Map<String, Object> headers, @JsonBody Map<String, Object> frame);
}
