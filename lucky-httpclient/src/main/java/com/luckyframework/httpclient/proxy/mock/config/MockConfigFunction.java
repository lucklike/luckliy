package com.luckyframework.httpclient.proxy.mock.config;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.CommonFunctions;
import com.luckyframework.httpclient.proxy.mock.MockResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mock配置相关的工具函数与回调函数
 */
public class MockConfigFunction {


    /**
     * 是否执行Mock逻辑，Mock配置对象存在且开关开启
     *
     * @param mc 方法上下文
     * @return 是否执行Mock逻辑
     */
    public static boolean mockEnable(MethodContext mc, MockConfiguration mockConfig) {
        // 判断总开关
        if (mockConfig == null || Objects.equals(mockConfig.isEnable(), Boolean.FALSE)) {
            return false;
        }

        // 判断方法级别开关
        Map<String, MockResult> methods = mockConfig.getMethodConfigs();
        String apiName = CommonFunctions.getApiId(mc);
        MockResult methodMock = methods.get(apiName);
        return methodMock != null && !Objects.equals(methodMock.isEnable(), Boolean.FALSE);
    }


    /**
     * 将Mock配置对象转化为{@link MockResponse}对象
     *
     * @param mc 方法上下文
     * @return {@link MockResponse}对象
     * @throws InterruptedException 可能出现的异常
     */
    public static MockResponse mockResult(MethodContext mc, MockConfiguration mockConfig) throws InterruptedException {

        MockResponse mockResponse = MockResponse.create();
        String apiId = CommonFunctions.getApiId(mc);
        MockResult mockResult = mockConfig.getMethodConfigs().get(apiId);

        // main
        Long latency = mockResult.getLatency() == null ? mockConfig.getLatency() : mockResult.getLatency();
        Integer status = mockResult.getStatus();
        Map<String, Object> headers = mockResult.getHeaders();

        // match
        boolean bodySetter = false;
        List<WhenMockResult> matchList = mockResult.getMatch();
        if (ContainerUtils.isNotEmptyCollection(matchList)) {
            for (WhenMockResult math : matchList) {

                // 判断when表达式是否成立
                String whenExp = math.getWhen();
                if (!StringUtils.hasText(whenExp) || !mc.parseExpression(whenExp, boolean.class)) {
                    continue;
                }

                mockResponse.header("Mock-Branch", whenExp);

                // latency
                latency = math.getLatency() == null ? latency : math.getLatency();

                // status
                status = math.getStatus() == null ? status : math.getStatus();

                // headers
                Map<String, Object> _headers = math.getHeaders();
                if (ContainerUtils.isNotEmptyMap(headers)) {
                    if (ContainerUtils.isNotEmptyMap(_headers)) {
                        headers.putAll(_headers);
                    }
                } else {
                    headers = _headers;
                }

                // body
                MockBody body = math.getBody();
                // file
                if (StringUtils.hasText(body.getFile())) {
                    mockResponse.resource(Resources.getResource(mc.parseExpression(body.getFile(), String.class)));
                    bodySetter = true;
                }

                // txt
                if (!bodySetter) {
                    if (StringUtils.hasText(body.getTxt())) {
                        mockResponse.body(mc.parseExpression(body.getTxt(), String.class));
                        bodySetter = true;
                    }
                }
                break;
            }
        }

        // status
        setStatus(mockResponse, status);

        // header
        setHeaders(mc, mockResponse, headers);

        // body
        MockBody body = mockResult.getBody();
        if (!bodySetter) {
            if (StringUtils.hasText(body.getFile())) {
                mockResponse.resource(Resources.getResource(mc.parseExpression(body.getFile(), String.class)));
                bodySetter = true;
            }

            // TXT
            if (!bodySetter) {
                if (StringUtils.hasText(body.getTxt())) {
                    mockResponse.body(mc.parseExpression(body.getTxt(), String.class));
                }
            }
        }

        //latency
        setLatency(latency);

        //return
        return mockResponse;
    }

    /**
     * 设置状态
     *
     * @param mockResponse Mock响应
     * @param status       状态配置
     */
    private static void setStatus(MockResponse mockResponse, Integer status) {
        mockResponse.status(status == null ? 200 : status);
    }

    /**
     * 设置延时
     *
     * @param latency 延时配置
     * @throws InterruptedException 可能出现的异常
     */
    private static void setLatency(Long latency) throws InterruptedException {
        if (latency != null && latency > 0) {
            Thread.sleep(latency);
        }
    }

    /**
     * 设置响应头
     *
     * @param mc           方法上下文
     * @param mockResponse Mock响应
     * @param headers      响应头配置
     */
    private static void setHeaders(MethodContext mc, MockResponse mockResponse, Map<String, Object> headers) {
        if (ContainerUtils.isNotEmptyMap(headers)) {
            headers.forEach((k, v) -> {
                String hName = mc.parseExpression(k, String.class);
                if (ContainerUtils.isIterable(v)) {
                    ContainerUtils.getIterable(v).forEach(e -> {
                        mockResponse.header(hName, mc.parseExpression(String.valueOf(e)));
                    });
                } else {
                    mockResponse.header(hName, mc.parseExpression(String.valueOf(v)));
                }
            });
        }
    }

}
