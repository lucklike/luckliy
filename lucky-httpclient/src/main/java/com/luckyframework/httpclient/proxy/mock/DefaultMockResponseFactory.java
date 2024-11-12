package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.AgreedOnMethodExecuteException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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
    public Response doGetMockResponseByCache(Request request, MethodContext context, String mockResponse, int status, String[] headers, String body, boolean cache) {
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
    private Response doCreateMockResponse(Request request, MethodContext context, String mockResponse, int status, String[] headers, String body) {

        // 存在Mock表达式
        if (StringUtils.hasText(mockResponse)) {
            Response mockResp = context.parseExpression(mockResponse, Response.class);
            setRequestObject(mockResp, request);
            return mockResp;
        }

        // 存在约定的Mock方法
        Method agreedOnMockMethod = getAgreedOnMockMethod(context);
        if (agreedOnMockMethod != null) {
            Response mockResp = executeAgreedOnMethod(context, agreedOnMockMethod);
            setRequestObject(mockResp, request);
            return mockResp;
        }

        // 注解响应配置
        MockResponse mockResp = MockResponse.create().request(request).status(status);
        for (String headerString : headers) {
            int index = headerString.indexOf(":");
            if (index == -1) {
                throw new MockException("Wrong mock header parameter expression: '" + headerString + "'. Please use the correct separator: ':'");
            }

            String nameExpression = headerString.substring(0, index).trim();
            String valueExpression = headerString.substring(index + 1).trim();

            mockResp.header(context.parseExpression(nameExpression, String.class), context.parseExpression(valueExpression));
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
            throw new MockException("Type that is not supported by the mock response body.  expression: {}, resultType: {}", body, ClassUtils.getClassName(bodyObject));
        }
        return mockResp;
    }

    /**
     * 获取约定的Mock方法
     *
     * @param context 方法上下文
     * @return 约定的Mock方法
     */
    @Nullable
    private Method getAgreedOnMockMethod(MethodContext context) {
        final String SUFFIX = "Mock";

        String agreedOnMockExpression = context.getCurrentAnnotatedElement().getName() + SUFFIX;
        Method agreedOnMockMethod = context.getVar(agreedOnMockExpression, Method.class);
        if (agreedOnMockMethod != null && Response.class.isAssignableFrom(agreedOnMockMethod.getReturnType())) {
            return agreedOnMockMethod;
        }
        return null;
    }

    /**
     * 尝试为相应对象设置请求属性
     *
     * @param response 响应对象
     * @param request  请求对象
     */
    private void setRequestObject(Response response, Request request) {
        if (response instanceof RequestAware) {
            ((RequestAware) response).setRequest(request);
        }
    }

    /**
     * 执行约定方法
     *
     * @param context        方法上下文
     * @param agreedOnMethod 约定方法
     * @return 执行结果
     */
    private Response executeAgreedOnMethod(MethodContext context, Method agreedOnMethod) {
        try {
            return (Response) MethodUtils.invoke(null, agreedOnMethod, context.getMethodParamObject(agreedOnMethod));
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new AgreedOnMethodExecuteException(e, "Failed to execute the Mock method: {}", agreedOnMethod.toGenericString());
        }
    }
}
