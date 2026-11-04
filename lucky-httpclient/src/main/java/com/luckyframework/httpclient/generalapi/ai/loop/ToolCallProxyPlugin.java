package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.vendor.VendorAdapter;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;

import java.util.Arrays;

/**
 * 工具调用回环插件，由 {@link EnableToolCall} 组合的插件注解挂载。
 * <pre>
 *     命中本插件时不再执行 {@code decorator.proceed()}：方法上下文在一次调用结束后会被销毁，
 *     重复proceed()会撞上已回收的资源。回环交给 {@link FunctionCallingRunner}，
 *     由它每轮重新调用代理方法来拿到全新的上下文。
 *
 *     防重入依赖会话请求对象上的轮次标记：回环内部发出的每一轮请求，其ChatRequest的
 *     loopRound都大于0，插件读到标记后直接proceed()走正常单次请求。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，由 {@link EnableToolCall} 的组装 {@code @Plugin} 挂载，业务方不需要直接引用
 */
public class ToolCallProxyPlugin implements ProxyPlugin {

    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {

        ExecuteMeta meta = decorator.getMeta();
        MethodContext mc = meta.getMethodContext();
        EnableToolCall enableAnn = mc.getMergedAnnotationCheckParent(EnableToolCall.class);
        if (enableAnn == null) {
            return decorator.proceed();
        }

        ChatRequest request = meta.getArg(ChatRequest.class);
        if (request == null) {
            throw new ToolCallException("The @EnableToolCall method ['" + meta.getMethod().getName()
                    + "'] must have a ChatRequest parameter to carry the conversation history.");
        }

        // 回环内部发出的请求：直接走正常的单次请求流程，不再进入第二层回环
        if (request.getLoopRound() > 0) {
            return decorator.proceed();
        }

        ToolRegistry registry = StringUtils.hasText(enableAnn.registry())
                ? mc.parseExpression(enableAnn.registry(), ToolRegistry.class)
                : ToolRegistry.getDefault();
        int maxRounds = mc.parseExpression(enableAnn.maxRounds(), int.class);
        // 方言只在流式回环里需要（用来创建增量拼接器），阻塞式回环可以不填
        VendorAdapter adapter = StringUtils.hasText(enableAnn.dialect())
                ? mc.parseExpression(enableAnn.dialect(), VendorAdapter.class)
                : null;

        // 子集生效：报文里的工具列表与可执行的工具都以这份快照为准，未选中的工具按「未知工具」处理
        ToolRegistry scope = enableAnn.tools().length == 0 ? registry : registry.select(Arrays.asList(enableAnn.tools()));

        return new FunctionCallingRunner(meta, scope, scope.listSpecs(), maxRounds, enableAnn.failurePolicy(), adapter)
                .run(request);
    }

    @Override
    public boolean match(ExecuteMeta meta) {
        return meta.getMethodContext().getMergedAnnotationCheckParent(EnableToolCall.class) != null;
    }
}
