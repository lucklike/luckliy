package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.interceptor.support.RedirectTestApi;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.httpclient.proxy.typeparser.support.TestUtils;
import com.sun.net.httpserver.HttpExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link RedirectInterceptor} 的端到端测试
 * <pre>
 *     重点是「完整URL写在@Get注解上」这一类请求：此时URL落在Request的path上、baseUrl为空，
 *     而{@code StringUtils.joinUrlPath}遇到带协议头的path时会直接返回path，
 *     所以只改baseUrl的重定向写法对这类请求完全是空操作，
 *     每次重定向仍在请求同一个地址，直到撞上最大次数才报错。
 *
 *     全部用例只打本地假服务端，不发起任何外部网络请求。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-04
 */
public class RedirectInterceptorTest {

    private static final String START_PATH = "/redirect/start";
    private static final String RELATIVE_START_PATH = "/redirect/relative-start";
    private static final String TWO_HOP_START_PATH = "/redirect/two-hop-start";
    private static final String MIDDLE_PATH = "/redirect/middle";
    private static final String FINAL_PATH = "/redirect/final";
    private static final String LOOP_PATH = "/redirect/loop";
    private static final String QUERY_START_PATH = "/redirect/query-start";
    private static final String QUERY_FINAL_PATH = "/redirect/query-final";
    private static final String NO_LOCATION_304_PATH = "/redirect/304-no-location";
    private static final String SEE_OTHER_START_PATH = "/redirect/303-start";
    private static final String SEE_OTHER_TARGET_PATH = "/redirect/303-target";
    private static final String HOST_START_PATH = "/redirect/host-start";
    private static final String HOST_TARGET_PATH = "/redirect/host-target";
    private static final String CACHE_FINAL_PATH = "/cache/final";

    private static final String FINAL_BODY = "final-body";

    private TestHttpServer server;
    private RedirectTestApi api;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        // 终点：正常响应
        registerRedirect(START_PATH, server.baseUrl() + FINAL_PATH);
        // 终点：相对地址形式的Location
        registerRedirect(RELATIVE_START_PATH, FINAL_PATH);
        // 两跳：起始 -> 中间 -> 终点，Location都是相对地址
        registerRedirect(TWO_HOP_START_PATH, MIDDLE_PATH);
        registerRedirect(MIDDLE_PATH, FINAL_PATH);
        // 死循环：Location始终指回自己
        registerRedirect(LOOP_PATH, LOOP_PATH);
        // 带查询串的Location，终点把收到的查询串原样回显，便于核对最终发出的参数
        registerRedirect(QUERY_START_PATH, QUERY_FINAL_PATH + "?token=abc");
        server.registerHandler(QUERY_FINAL_PATH, this::echoQuery);
        server.registerHandler(FINAL_PATH, exchange ->
                server.respond(exchange, 200, "text/plain;charset=UTF-8", Collections.emptyMap(), FINAL_BODY));
        // 304且不带Location，验证其不再被当作重定向
        server.registerHandler(NO_LOCATION_304_PATH, exchange ->
                server.respond(exchange, 304, "text/plain;charset=UTF-8", Collections.emptyMap(), ""));
        // 303：起始返回303，目标为正常2xx
        server.registerHandler(SEE_OTHER_START_PATH, exchange ->
                server.respond(exchange, 303, "text/plain;charset=UTF-8", Collections.singletonMap("Location", SEE_OTHER_TARGET_PATH), "see-other"));
        server.registerHandler(SEE_OTHER_TARGET_PATH, exchange ->
                server.respond(exchange, 200, "text/plain;charset=UTF-8", Collections.emptyMap(), "303-target"));
        // 跨主机：起始在baseUrl主机(0.0.0.0)，Location指向localhost，主机不同应敏感头被剥离
        server.registerHandler(HOST_START_PATH, exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8",
                        Collections.singletonMap("Location", "http://localhost:" + port() + HOST_TARGET_PATH), "moved"));
        server.registerHandler(HOST_TARGET_PATH, exchange ->
                server.respond(exchange, 200, "text/plain;charset=UTF-8", Collections.emptyMap(), "host-target"));
        // 缓存串号：/cache/1、/cache/2 各自302到带不同id的final
        server.registerHandler("/cache/1", exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8", Collections.singletonMap("Location", CACHE_FINAL_PATH + "?id=1"), "moved"));
        server.registerHandler("/cache/2", exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8", Collections.singletonMap("Location", CACHE_FINAL_PATH + "?id=2"), "moved"));
        server.registerHandler(CACHE_FINAL_PATH, this::echoQuery);
        // 缓存命中路径去重：/cache-q/start 302 到带 token=abc 的 final
        server.registerHandler("/cache-q/start", exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8", Collections.singletonMap("Location", "/cache-q/final?token=abc"), "moved"));
        server.registerHandler("/cache-q/final", this::echoQuery);
        // 缓存命中路径跨主机剥离：/cache-h/start 302 到 localhost 上的 target
        server.registerHandler("/cache-h/start", exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8",
                        Collections.singletonMap("Location", "http://localhost:" + port() + "/cache-h/target"), "moved"));
        server.registerHandler("/cache-h/target", exchange ->
                server.respond(exchange, 200, "text/plain;charset=UTF-8", Collections.emptyMap(), "cache-host-target"));

        api = new HttpClientProxyObjectFactory().getProxyObject(RedirectTestApi.class);
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * URL整个写在path上时，重定向必须真正换成Location指向的地址，而不是原地打转
     */
    @Test
    public void anAbsoluteRedirectLocationReplacesTheRequestUrl() {
        String body = api.follow(requestWithFullUrlOnPath(START_PATH));

        assertEquals(FINAL_BODY, body);
        assertEquals("起始地址只该被请求一次", 1, server.hit(START_PATH));
        assertEquals("重定向地址应当被请求到", 1, server.hit(FINAL_PATH));
    }

    /**
     * Location是相对地址时，要基于当前URL解析出完整地址，并且是整体替换而不是追加
     */
    @Test
    public void aRelativeRedirectLocationIsResolvedAgainstTheCurrentUrl() {
        String body = api.follow(requestWithFullUrlOnPath(RELATIVE_START_PATH));

        assertEquals(FINAL_BODY, body);
        assertEquals(1, server.hit(RELATIVE_START_PATH));
        assertEquals("最终请求的路径应当就是重定向指向的路径", FINAL_PATH, server.lastRequest(FINAL_PATH).path());
    }

    /**
     * 连续两次重定向时，每一跳都各自替换地址，不会把上一跳的路径拼到下一跳后面
     */
    @Test
    public void aTwoHopRedirectChainEndsAtTheLastLocation() {
        String body = api.follow(requestWithFullUrlOnPath(TWO_HOP_START_PATH));

        assertEquals(FINAL_BODY, body);
        assertEquals(1, server.hit(TWO_HOP_START_PATH));
        assertEquals(1, server.hit(MIDDLE_PATH));
        // 拼接出来的错误路径同样会命中终点的处理器（按前缀匹配），所以必须核对真实请求路径
        assertEquals(FINAL_PATH, server.lastRequest(FINAL_PATH).path());
    }

    /**
     * Location自带的查询参数要以Location为准，请求里的同名旧值不能重复发出去
     */
    @Test
    public void queryParamsCarriedByTheRedirectLocationWinTheDuplicateOnes() {
        DefaultRequest request = requestWithFullUrlOnPath(QUERY_START_PATH);
        request.addQueryParameter("token", "old");
        request.addQueryParameter("page", "2");

        String query = api.follow(request);

        assertTrue("同名参数应以Location为准: " + query, query.contains("token=abc"));
        assertTrue("不应再把请求里的旧值重复发出去: " + query, !query.contains("token=old"));
        assertTrue("无关参数仍要保留: " + query, query.contains("page=2"));
    }

    /**
     * 死循环重定向在用完最大次数时就报错，不会一直请求到默认上限
     */
    @Test
    public void theRedirectLoopIsStoppedByTheMaxRedirectCount() {
        try {
            api.followWithMaxCount(requestWithFullUrlOnPath(LOOP_PATH));
            fail("超过最大重定向次数应当抛出RedirectException");
        } catch (Exception e) {
            assertTrue("异常原因链中应有RedirectException，实际: " + e, TestUtils.hasCause(e, RedirectException.class));
            // 首次请求 + 2次重定向，第3次响应进来时发现已超出maxCount=2的限制
            assertEquals("最大次数为2时只该发出3次请求", 3, server.hit(LOOP_PATH));
        }
    }

    /**
     * 开启地址缓存后，后续请求直接使用已缓存的最终地址，不再经过第一跳
     */
    @Test
    public void theCachedRedirectLocationIsUsedDirectlyByLaterCalls() {
        assertEquals(FINAL_BODY, api.followWithCachedLocation(requestWithFullUrlOnPath(START_PATH)));
        assertEquals(1, server.hit(START_PATH));
        assertEquals(1, server.hit(FINAL_PATH));

        // 第二次调用换一个全新的请求对象，地址替换只能依赖拦截器里缓存的重定向地址
        assertEquals(FINAL_BODY, api.followWithCachedLocation(requestWithFullUrlOnPath(START_PATH)));
        assertEquals("缓存生效后不该再请求起始地址", 1, server.hit(START_PATH));
        assertEquals(2, server.hit(FINAL_PATH));
    }

    /**
     * 304不再被列入重定向状态码，即使不带Location也应原样返回，不再抛RedirectException
     */
    @Test
    public void aThreeOhFourResponseIsNotTreatedAsARedirect() {
        try {
            api.follow(requestWithFullUrlOnPath(NO_LOCATION_304_PATH));
        } catch (Exception e) {
            assertTrue("304不应再被当重定向处理，异常链里不该有RedirectException，实际: " + e,
                    !TestUtils.hasCause(e, RedirectException.class));
        }
        assertEquals("304只该被请求一次", 1, server.hit(NO_LOCATION_304_PATH));
    }

    /**
     * 303之后的请求必须改为GET并丢弃请求体
     */
    @Test
    public void aThreeOhThreeRedirectSwitchesTheFollowUpRequestToGet() {
        DefaultRequest request = new DefaultRequest("", server.baseUrl() + SEE_OTHER_START_PATH, RequestMethod.POST);
        String body = api.postFollow(request, "payload=1".getBytes());

        assertEquals("303-target", body);
        assertEquals("起始请求应为POST", "POST", server.lastRequest(SEE_OTHER_START_PATH).method());
        TestHttpServer.RecordedRequest target = server.lastRequest(SEE_OTHER_TARGET_PATH);
        assertEquals("303后重定向请求应为GET", "GET", target.method());
        assertEquals("303后应丢弃请求体", "", target.body());
    }

    /**
     * 重定向到不同主机时，Cookie与Authorization不应被带过去
     */
    @Test
    public void sensitiveHeadersAreStrippedWhenRedirectCrossesHosts() {
        DefaultRequest request = requestWithFullUrlOnPath(HOST_START_PATH);
        request.addHeader("Cookie", "session=SECRET");
        request.addHeader("Authorization", "Bearer TOKEN");

        assertEquals("host-target", api.follow(request));
        TestHttpServer.RecordedRequest target = server.lastRequest(HOST_TARGET_PATH);
        assertEquals("跨主机重定向不应携带Cookie", null, target.header("Cookie"));
        assertEquals("跨主机重定向不应携带Authorization", null, target.header("Authorization"));
    }

    /**
     * 同主机重定向时Cookie仍应保留（不被误剥）
     */
    @Test
    public void sensitiveHeadersAreKeptWhenRedirectStaysOnSameHost() {
        DefaultRequest request = requestWithFullUrlOnPath(START_PATH);
        request.addHeader("Cookie", "session=KEEP");

        assertEquals(FINAL_BODY, api.follow(request));
        assertEquals("同主机重定向应保留Cookie", "session=KEEP", server.lastRequest(FINAL_PATH).header("Cookie"));
    }

    /**
     * cacheLocation以原始请求地址为key，不同地址的请求不能串号
     */
    @Test
    public void theCachedLocationIsKeyedByTheOriginalRequestUrl() {
        assertEquals("id=1", api.followWithCachedLocation(cachedRequest("1")));
        // id=2 是另一个原始地址，不能复用 id=1 的缓存，否则返回错数据
        assertEquals("id=2", api.followWithCachedLocation(cachedRequest("2")));
        assertEquals("/cache/2 应当被真实请求过", 1, server.hit("/cache/2"));
    }

    /**
     * maxCount<=0时重定向被完全关闭，不该发出任何跟随请求
     */
    @Test
    public void maxCountNotPositiveCompletelyDisablesRedirect() {
        try {
            api.followWithMaxCountZero(requestWithFullUrlOnPath(START_PATH));
        } catch (Exception ignore) {
            // 3xx响应不做重定向时可能被下游状态码处理抛出，此处只关心重定向是否发生
        }
        assertEquals("maxCount=0时不应重定向，起始地址只请求一次", 1, server.hit(START_PATH));
        assertEquals("maxCount=0时不该发出任何重定向请求", 0, server.hit(FINAL_PATH));
    }

    /**
     * 缓存命中路径（doBeforeExecute）也必须像真实重定向那样清同名查询参数，不能重复拼接
     */
    @Test
    public void cachedPathReuseAlsoDedupesQueryParamsAgainstTheLocation() {
        // 第一次调用填充缓存：原始地址(.../start?token=old) -> 最终地址(.../final?token=abc)
        DefaultRequest first = requestWithFullUrlOnPath("/cache-q/start");
        first.addQueryParameter("token", "old");
        assertEquals("token=abc", api.followWithCachedLocation(first));

        // 第二次相同地址+参数，应复用缓存且仍要去掉同名的 token=old
        DefaultRequest second = requestWithFullUrlOnPath("/cache-q/start");
        second.addQueryParameter("token", "old");
        String query = api.followWithCachedLocation(second);
        assertTrue("缓存命中路径应以Location为准: " + query, query.contains("token=abc"));
        assertTrue("缓存命中路径不应重复拼上token=old: " + query, !query.contains("token=old"));
    }

    /**
     * 缓存命中路径跨主机时，同样必须剥离Cookie/Authorization，与真实重定向路径保持一致
     */
    @Test
    public void cachedPathReuseAlsoStripsSensitiveHeadersAcrossHosts() {
        DefaultRequest first = requestWithFullUrlOnPath("/cache-h/start");
        first.addHeader("Cookie", "session=SECRET");
        assertEquals("cache-host-target", api.followWithCachedLocation(first));

        DefaultRequest second = requestWithFullUrlOnPath("/cache-h/start");
        second.addHeader("Cookie", "session=SECRET");
        second.addHeader("Authorization", "Bearer TOKEN");
        assertEquals("cache-host-target", api.followWithCachedLocation(second));
        TestHttpServer.RecordedRequest target = server.lastRequest("/cache-h/target");
        assertEquals("缓存命中路径跨主机也应剥离Cookie", null, target.header("Cookie"));
        assertEquals("缓存命中路径跨主机也应剥离Authorization", null, target.header("Authorization"));
    }

    /**
     * 注册一个只回302+Location的端点
     *
     * @param path     路径
     * @param location 响应头Location的值
     */
    private void registerRedirect(String path, String location) {
        server.registerHandler(path, exchange ->
                server.respond(exchange, 302, "text/plain;charset=UTF-8", Collections.singletonMap("Location", location), "redirecting"));
    }

    /**
     * 把收到的查询串原样回显成响应体
     *
     * @param exchange 本次交换
     * @throws Exception 响应写出失败时抛出
     */
    private void echoQuery(HttpExchange exchange) throws Exception {
        String query = exchange.getRequestURI().getRawQuery();
        server.respond(exchange, 200, "text/plain;charset=UTF-8", Collections.emptyMap(), query == null ? "" : query);
    }

    /**
     * 构造「完整URL落在path上、baseUrl为空」的请求，与URL写在@Get注解上时代理构造出的请求一致
     *
     * @param path 假服务端上的路径
     * @return 请求对象
     */
    private DefaultRequest requestWithFullUrlOnPath(String path) {
        return new DefaultRequest("", server.baseUrl() + path, RequestMethod.GET);
    }

    /**
     * 构造 /cache/{id} 形式的请求，用于验证缓存不会把不同id的请求串号
     *
     * @param id 路径参数
     * @return 请求对象
     */
    private DefaultRequest cachedRequest(String id) {
        DefaultRequest request = new DefaultRequest("", server.baseUrl() + "/cache/" + id, RequestMethod.GET);
        return request;
    }

    /**
     * 从baseUrl中取出端口号
     *
     * @return 假服务端监听端口
     */
    private int port() {
        String baseUrl = server.baseUrl();
        return Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
    }
}
