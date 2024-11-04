package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
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

    /**
     * Mock Response 缓存
     */
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

    /**
     * 获取MockResponse，当cache=true时会先尝试从缓存中获取
     *
     * @param request      请求实例
     * @param context      方法上下文
     * @param mockResponse mock表达式
     * @param status       mock状态码
     * @param headers      mock响应头
     * @param body         mock响应体
     * @param cache        是否缓存第一次生成的MockResponse
     * @return MockResponse
     */
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


    /**
     * 获取MockResponse
     *
     * @param request      请求实例
     * @param context      方法上下文
     * @param mockResponse mock表达式
     * @param status       mock状态码
     * @param headers      mock响应头
     * @param body         mock响应体
     * @return MockResponse
     */
    private Response doCreateMockResponse(Request request,
                                          MethodContext context,
                                          String mockResponse,
                                          int status,
                                          String[] headers,
                                          String body) {

        String mockResponseExpression = getAgreedOnMockResponseExpression(context, mockResponse);
        if (StringUtils.hasText(mockResponseExpression)) {
            Response mockResp = context.parseExpression(mockResponseExpression, Response.class);
            if (mockResp instanceof RequestAware) {
                ((RequestAware) mockResp).setRequest(request);
            }
            return mockResp;
        }

        MockResponse mockResp = MockResponse.create().request(request).status(status);
        for (String headerString : headers) {
            int index = headerString.indexOf(":");
            if (index == -1) {
                throw new MockException("Wrong mock header parameter expression: '" + headerString + "'. Please use the correct separator: ':'");
            }

            String nameExpression = headerString.substring(0, index).trim();
            String valueExpression = headerString.substring(index + 1).trim();

            mockResp.header(
                    context.parseExpression(nameExpression, String.class),
                    context.parseExpression(valueExpression)
            );
        }

        Object bodyObject = context.parseExpression(body);

        // String
        if (bodyObject instanceof String) {
            mockResp.body((String) bodyObject);
        }
        // byte[]
        else if (bodyObject instanceof byte[]) {
            mockResp.body((byte[]) bodyObject);
        }
        // ByteBuffer
        else if (bodyObject instanceof ByteBuffer) {
            mockResp.body(((ByteBuffer) bodyObject).array());
        }
        // InputStream
        else if (bodyObject instanceof InputStream) {
            mockResp.body((InputStream) bodyObject);
        }
        // File
        else if (bodyObject instanceof File) {
            mockResp.file((File) bodyObject);
        }
        // Resource
        else if (bodyObject instanceof Resource) {
            mockResp.resource((Resource) bodyObject);
        } else if (bodyObject instanceof InputStreamSource) {
            try {
                mockResp.body(((InputStreamSource) bodyObject).getInputStream());
            } catch (Exception e) {
                throw new LuckyRuntimeException(e);
            }
        }
        // Exception
        else {
            throw new MockException("Type that is not supported by the mock response body.  expression: {}, resultType: {}",
                    body,
                    ClassUtils.getClassName(bodyObject));
        }
        return mockResp;
    }

    /**
     * 获取生成MockResponse的表达式
     * <pre>
     *     1.configExpression不为null时直接返回configExpression
     *     2.尝试在方法上下文中获取名称为方法名+Mock的变量，例如方法名为hello，则找helloMock的变量
     *     3.能找到且找到的变量符合要求则返回表达式#{#方法名+Mock()}，例如#{#helloMock()}
     *     4.找不到或者不符合要求时返回configExpression
     * </pre>
     *
     * @param context          方法上下文
     * @param configExpression mock表达式
     * @return mock表达式
     */
    private String getAgreedOnMockResponseExpression(MethodContext context, String configExpression) {
        if (StringUtils.hasText(configExpression)) {
            return configExpression;
        }

        final String SUFFIX = "Mock";
        String agreedOnMockExpression = context.getCurrentAnnotatedElement().getName() + SUFFIX;
        Method agreedOnMockMethod = context.getVar(agreedOnMockExpression, Method.class);
        if (agreedOnMockMethod != null && Response.class.isAssignableFrom(agreedOnMockMethod.getReturnType())) {
            String expressionTemp = "#{#%s(%s)}";
            List<String> varNameList = context.getMethodParamVarNames(agreedOnMockMethod);
            String varStr = StringUtils.join(varNameList, ", ");
            return String.format(expressionTemp, agreedOnMockExpression, varStr);
        }

        return configExpression;
    }
}
