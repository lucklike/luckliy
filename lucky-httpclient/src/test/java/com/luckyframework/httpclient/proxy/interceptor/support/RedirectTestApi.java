package com.luckyframework.httpclient.proxy.interceptor.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.AutoRedirect;
import com.luckyframework.httpclient.proxy.annotations.BinaryBody;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

/**
 * 重定向拦截器测试使用的API接口
 * <pre>
 *     1.方法都通过参数里的{@link Request}直接指定请求地址，
 *       测试因此可以构造出「完整URL落在path上、baseUrl为空」的请求（与URL写在@Get注解上的构造方式一致），
 *       这正是重定向时必须整体替换请求地址的场景；
 *     2.纯注解模式下重定向拦截器靠{@link AutoRedirect}挂载，所以每个方法都需要该注解。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-04
 */
public interface RedirectTestApi {

    /**
     * 使用默认的重定向配置（最大5次、不缓存地址）
     *
     * @param request 请求对象
     * @return 响应体文本
     */
    @Get
    @AutoRedirect
    String follow(Request request);

    /**
     * 开启重定向地址缓存，最终地址会被记在拦截器上供后续请求直接使用
     *
     * @param request 请求对象
     * @return 响应体文本
     */
    @Get
    @AutoRedirect(cacheLocation = true)
    String followWithCachedLocation(Request request);

    /**
     * 把最大重定向次数限制为2次，用于验证死循环重定向能被及时止损
     *
     * @param request 请求对象
     * @return 响应体文本
     */
    @Get
    @AutoRedirect(maxCount = 2)
    String followWithMaxCount(Request request);

    /**
     * 以POST发起、开启重定向，用于验证303后请求方法被改为GET
     *
     * @param request 请求对象
     * @param body    请求体
     * @return 响应体文本
     */
    @Post
    @AutoRedirect
    String postFollow(Request request, @BinaryBody byte[] body);

    /**
     * 把最大重定向次数设为0，用于验证maxCount<=0时重定向被完全关闭
     *
     * @param request 请求对象
     * @return 响应体文本
     */
    @Get
    @AutoRedirect(maxCount = 0)
    String followWithMaxCountZero(Request request);

}
