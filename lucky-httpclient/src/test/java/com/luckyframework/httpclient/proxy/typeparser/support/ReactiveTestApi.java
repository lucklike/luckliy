package com.luckyframework.httpclient.proxy.typeparser.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.TextEventStream;
import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用于测试Mono/Flux包装类型解析器的代理接口
 * <pre>
 *     1.方法参数中的{@link Request}用于直接指定请求地址（跳过注解URL解析）
 *     2.方法参数中的{@link AsyncTaskExecutor}用于向框架注入测试内的记录型执行器，
 *       所有异步任务都会通过该执行器提交，便于观测与驱动
 * </pre>
 */
public interface ReactiveTestApi {

    /**
     * 普通文本响应的Mono
     */
    Mono<String> monoHello(Request request, AsyncTaskExecutor executor);

    /**
     * 空响应的Mono
     */
    Mono<Void> monoEmpty(Request request, AsyncTaskExecutor executor);

    /**
     * 慢响应的Mono（2秒）
     */
    Mono<String> monoSlow(Request request, AsyncTaskExecutor executor);

    /**
     * JSON数组响应的Flux（普通列表聚合模式）
     */
    Flux<User> fluxList(Request request, AsyncTaskExecutor executor);

    /**
     * 慢响应的Flux（2秒）
     */
    Flux<User> fluxSlow(Request request, AsyncTaskExecutor executor);

    /**
     * 返回非法JSON的Flux，用于验证转换异常传播
     */
    Flux<User> fluxBadJson(Request request, AsyncTaskExecutor executor);

    /**
     * SSE基础流：逐行发送3行后正常关闭
     */
    @TextEventStream
    Flux<String> sseBasic(Request request, AsyncTaskExecutor executor);

    /**
     * SSE挂起流：发送1行后保持连接不关闭
     */
    @TextEventStream(readTimeout = 3000)
    Flux<String> sseStall(Request request, AsyncTaskExecutor executor);

    /**
     * SSE截断响应：声明长度100只发送部分数据
     */
    @TextEventStream(readTimeout = 5000)
    Flux<String> sseBroken(Request request, AsyncTaskExecutor executor);
}
