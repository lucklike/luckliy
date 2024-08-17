package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认支持SpEL表达式的Mock Response工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/17 03:31
 */
public class DefaultMockResponseFactory implements MockResponseFactory {

    private final Map<Method, Response> mockResponsesCache = new ConcurrentHashMap<>(16);

    @Override
    public Response createMockResponse(Request request, MockContext context) {
        Mock mockAnn = context.toAnnotation(Mock.class);
        String mockResponse = mockAnn.mockResp();
        int status = mockAnn.status();
        String[] header = mockAnn.header();
        String body = mockAnn.body();
        return doGetMockResponseByCache(request, context.getContext(), mockResponse, status, header, body, mockAnn.cache());
    }

    public Response doGetMockResponseByCache(Request request,
                                                MethodContext context,
                                                String mockResponse,
                                                int status,
                                                String[] headers,
                                                String body,
                                                boolean cache) {
        if (cache) {
            Method method = context.getCurrentAnnotatedElement();
            return mockResponsesCache.computeIfAbsent(method, _m -> doCreateMockResponse(request, context, mockResponse, status, headers, body));
        }
        return doCreateMockResponse(request, context, mockResponse, status, headers, body);
    }


    private Response doCreateMockResponse(Request request,
                                          MethodContext context,
                                          String mockResponse,
                                          int status,
                                          String[] headers,
                                          String body) {
        if (StringUtils.hasText(mockResponse)) {
            MockResponse mockResp = context.parseExpression(mockResponse, MockResponse.class);
            mockResp.request(request);
            return mockResp;
        }
        MockResponse mockResp = MockResponse.create(request).status(status);
        for (String headerString : headers) {
            int index = headerString.indexOf(":");
            if (index == -1) {
                throw new IllegalArgumentException("Wrong mock header parameter expression: '" + headerString + "'. Please use the correct separator: ':'");
            }

            String nameExpression = headerString.substring(0, index).trim();
            String valueExpression = headerString.substring(index + 1).trim();

            mockResp.header(
                    context.parseExpression(nameExpression, String.class),
                    context.parseExpression(valueExpression)
            );
        }

        Object bodyObject = context.parseExpression(body);
        if (bodyObject instanceof String) {
            mockResp.body((String) bodyObject);
        } else if (bodyObject instanceof byte[]) {
            mockResp.body((byte[]) bodyObject);
        } else if (bodyObject instanceof InputStream) {
            mockResp.body((InputStream) bodyObject);
        } else if (bodyObject instanceof File) {
            mockResp.file((File) bodyObject);
        } else if (bodyObject instanceof Resource) {
            mockResp.resource((Resource) bodyObject);
        } else {
            throw new LuckyRuntimeException("Type that is not supported by the mock response body: body={}, type={}",
                    body,
                    bodyObject == null ? "null" : bodyObject.getClass());
        }
        return mockResp;
    }
}
