package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodWrap;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionMismatchException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
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

    /**
     * 约定的Mock方法后缀
     */
    public final String MOCK_FUNCTION_SUFFIX = "$Mock";

    @Override
    public Response createMockResponse(Request request, MockContext context) {
        Mock mockAnn = context.toAnnotation(Mock.class);
        String mockExpression = mockAnn.mockResp();
        String mockFuncName = mockAnn.mockFunc();
        String status = mockAnn.status();
        String[] header = mockAnn.header();
        String body = mockAnn.body();
        return doGetMockResponseByCache(request, context.getContext(), mockExpression, mockFuncName, status, header, body, mockAnn.cache());
    }

    /**
     * 获取MockResponse，当cache=true时会先尝试从缓存中获取
     *
     * @param request        请求实例
     * @param context        方法上下文
     * @param mockExpression mock表达式
     * @param mockFuncName   指定的mock函数名
     * @param status         mock状态码
     * @param headers        mock响应头
     * @param body           mock响应体
     * @param cache          是否缓存第一次生成的MockResponse
     * @return MockResponse
     */
    public Response doGetMockResponseByCache(Request request,
                                             MethodContext context,
                                             String mockExpression,
                                             String mockFuncName,
                                             String status,
                                             String[] headers,
                                             String body,
                                             boolean cache) {
        if (cache) {
            Method method = context.getCurrentAnnotatedElement();
            return mockResponsesCache.computeIfAbsent(method, _m -> doCreateMockResponse(request, context, mockExpression, mockFuncName, status, headers, body));
        }
        return doCreateMockResponse(request, context, mockExpression, mockFuncName, status, headers, body);
    }


    /**
     * 获取MockResponse
     *
     * @param request        请求实例
     * @param context        方法上下文
     * @param mockExpression mock表达式
     * @param mockFuncName   指定的mock函数名
     * @param status         mock状态码
     * @param headers        mock响应头
     * @param body           mock响应体
     * @return MockResponse
     */
    private Response doCreateMockResponse(Request request,
                                          MethodContext context,
                                          String mockExpression,
                                          String mockFuncName,
                                          String status,
                                          String[] headers,
                                          String body) {

        // 存在Mock表达式
        if (StringUtils.hasText(mockExpression)) {
            Response mockResp = context.parseExpression(mockExpression, Response.class);
            setRequestObject(mockResp, request);
            return mockResp;
        }

        // 检查是否配置了Mock处理函数以及是否存在约定的Mock处理函数
        Method mockFuncMethod = getMockFuncMethod(context, mockFuncName);
        if (mockFuncMethod != null) {
            Response mockResp = executeMockFuncMethod(context, mockFuncMethod);
            setRequestObject(mockResp, request);
            return mockResp;
        }

        // 注解响应配置
        MockResponse mockResp = MockResponse.create().request(request).status(context.parseExpression(status, int.class));
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
     * 获取指定的Mock函数，如果不存在则会尝试查找约定的Mock函数
     *
     * @param context      方法上下文
     * @param mockFuncName 指定的Mock函数名
     * @return 约定的Mock方法
     */
    @Nullable
    private Method getMockFuncMethod(MethodContext context, String mockFuncName) {

        // 是否指定了处理函数
        boolean isAppoint = StringUtils.hasText(mockFuncName);

        // 获取指定的Mock函数名，如果不存在则使用约定的Mock函数名
        MethodWrap mockFuncMethodWrap = context.getSpELFuncOrDefault(mockFuncName, MOCK_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (mockFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Mock SpEL function named '{}' is not found in context.", mockFuncName);
            }
            return null;
        }

        // 函数返回值类型不匹配时的处理
        Method mockFuncMethod = mockFuncMethodWrap.getMethod();
        if (!Response.class.isAssignableFrom(mockFuncMethod.getReturnType())) {
            if (isAppoint) {
                throw new SpELFunctionMismatchException("The SpEL function '{}' that is specified to generate a MockResponse has a return value type error. \n\t--- func-return-type: {} \n\t--- correct-type: {}", mockFuncName, mockFuncMethod.getReturnType(), Response.class);
            }
            return null;
        }

        // 校验条件满足
        return mockFuncMethod;
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
     * 执行Mock方法
     *
     * @param context        方法上下文
     * @param mockFuncMethod mock方法
     * @return 执行结果
     */
    private Response executeMockFuncMethod(MethodContext context, Method mockFuncMethod){
        try {
            return (Response) context.invokeMethod(null, mockFuncMethod);
        } catch (LuckyInvocationTargetException e) {
            throw new ActivelyThrownException(e.getCause());
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new SpELFunctionExecuteException(e, "Mock method run exception: ['{}']", MethodUtils.getLocation(mockFuncMethod));
        }
    }
}
