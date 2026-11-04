package com.luckyframework.httpclient.generalapi.ai.loop;

/**
 * 工具执行失败时的处理策略。
 * <pre>
 *     {@link #CONTINUE} 默认策略：把失败作为工具结果回填给模型自纠，回环继续；
 *     {@link #ABORT}：首个工具异常直接上抛给业务方，不再发出后续请求。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public enum ToolCallFailurePolicy {

    /**
     * 失败结果回填后继续回环
     */
    CONTINUE,

    /**
     * 首个工具异常即终止回环并上抛
     */
    ABORT
}
