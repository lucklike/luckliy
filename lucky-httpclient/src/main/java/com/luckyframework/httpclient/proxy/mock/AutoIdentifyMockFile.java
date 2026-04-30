package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.function.ResourceFunctions;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.SpELImport;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.SimpleSpelBean;
import org.springframework.core.io.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/**
 * 自动识别指定目录下的特定文件来进行Mock，支持json/yml/properties格式文件
 *
 * <pre>
 *  eg:
 *  Class Api:
 * </pre>
 * <pre>
 *  {@code
 *      @HttpClient("http://localhost:8080")
 *      @AutoIdentifyFileMock
 *      @UseAutoUrlDerivationInsurance
 *      public interface AutoIdentifyFileMockApi {
 *
 *          @Get("login")
 *          SpelBean<?> login();
 *
 *          @Post("logout")
 *          String logout(@JsonParam String token);
 *      }
 *  }
 * </pre>
 *
 * <pre>
 *   Mock_AutoIdentifyFileMockApi.yml:
 * </pre>
 *
 * <pre>
 *  {@code
 *      #总开关
 *      $mainSwitch$: true
 *      #方法级别的延时模拟，（单位：毫秒）
 *      $latency$: 1000
 *
 *      #login方法的Mock数据
 *      login:
 *        #方法级别开关
 *        enable: false
 *        #条件匹配
 *        match:
 *          #条件1+结果1
 *          - when: "#{1==1}"
 *            latency: 1200
 *            status: 200
 *            headers:
 *              Server: nginx/1.18.0
 *              Date: Mon, 16 Mar 2026 06:46:14 GMT
 *              Content-Length: 157
 *            body:
 *              txt: qwqw
 *              file: classpath:deded/lll.txt
 *          # 条件2+结果2
 *          - when: "#{c == 3}"
 *            latency: 1300
 *            status: 404
 *            headers:
 *              Server: nginx/1.18.0
 *              Date: Mon, 16 Mar 2026 06:46:14 GMT
 *              Content-Length: 157
 *            body:
 *              txt: 404 Not Found
 *              file: classpath:deded/lll.txt
 *
 *        #延时模拟，（单位：毫秒）
 *        latency: 1000
 *        #状态码
 *        status: 200
 *        #响应头，Key和Value均支持SpEL表达式
 *        headers:
 *          Content-Type: application/json
 *          X-Random-Emial: #{random_email()};
 *        #响应体
 *        body:
 *          #文本格式响应体，支持SpEL表达式
 *          txt: |
 *            {
 *              "access_token": "e6c0991176784141583030b2af550655812729af8cd92598b5b99a9c0f89",
 *              "expire_time": 36000,
 *              "expires_in": "2026-01-08 21:06:04",
 *              "random_tel": "#{random_tel()}"
 *            }
 *          #文件类型的响应体
 *          file: classpath:test/mocak.pdf
 *
 *      #logout方法的Mock数据
 *      logout:
 *        headers:
 *          Content-Type: application/json
 *        body:
 *          txt: |
 *            {
 *                "error": {
 *                    "error_no": "0",
 *                    "error_info": "",
 *                    "error_pathinfo": null
 *                },
 *                "data": {
 *                    "staff_no": "1163",
 *                    "user_id": "1163",
 *                    "user_name": "fukang7075",
 *                    "info": "【#{$0}】登出成功"
 *                }
 *            }
 *  }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Mock(enable = "#{__enable_mock__($mc$)}", mockResp = "#{__mock_result__($mc$)}")
@SpELImport(AutoIdentifyMockFile.MockFunction.class)
public @interface AutoIdentifyMockFile {

    /**
     * Mock文件所在目录，支持SpEL表达式
     */
    String mockDir() default "classpath:mock-response";

    /**
     * Mock文件资源路径表达式，支持SpEL表达式
     */
    String mockFile() default "Mock_#{$class$.getSimpleName()}.yml";

    /**
     * 文件中的Key，支持SpEL表达式
     */
    String mockKey() default "#{$method$.getName()}";

    /**
     * Mock相关的函数
     */
    class MockFunction {

        @FunctionAlias("__enable_mock__")
        public static boolean enableMock(MethodContext mc) {
            AutoIdentifyMockFile ann = mc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);

            // mock文件是否存在
            String mockFilePath = StringUtils.joinUrlPath(mc.parseExpression(ann.mockDir(), String.class), mc.parseExpression(ann.mockFile(), String.class));
            Resource mockResource = ResourceFunctions.resource(mockFilePath);
            if (!mockResource.exists() || !mockResource.isFile()) {
                return false;
            }

            // 不是Map结构
            SimpleSpelBean<?> mockBean = Resources.resourceAsSpelBean(mockFilePath);
            if (!(mockBean.getBean() instanceof Map)) {
                return false;
            }

            // 总开关
            Object mainSwitch = mockBean.get("$mainSwitch$");
            if (mainSwitch != null && !mc.parseExpression(String.valueOf(mainSwitch), boolean.class)) {
                return false;
            }

            // 存在Mock配置
            String mockKey = mc.parseExpression(ann.mockKey(), String.class);
            Object mockValue = mockBean.get(String.format("['%s']", mockKey));
            if (!(mockValue instanceof Map)) {
                return false;
            }

            // 方法级别开关
            Object enable = ((Map<?, ?>) mockValue).get("enable");
            return enable == null || mc.parseExpression(String.valueOf(enable), boolean.class);
        }

        @FunctionAlias("__mock_result__")
        public static Response mockResult(MethodContext mc) throws Exception {
            AutoIdentifyMockFile ann = mc.getMergedAnnotationCheckParent(AutoIdentifyMockFile.class);

            String mockFilePath = StringUtils.joinUrlPath(mc.parseExpression(ann.mockDir(), String.class), mc.parseExpression(ann.mockFile(), String.class));
            String mockKeyConfig = mc.parseExpression(ann.mockKey(), String.class);
            String mockKey = String.format("['%s'].", mockKeyConfig);
            SimpleSpelBean<?> mockBean = Resources.resourceAsSpelBean(mockFilePath);

            MockResponse mockResponse = MockResponse.create();
            mockResponse.header("Mock-Annotation", "@AutoIdentifyMockFile");
            mockResponse.header("Mock-File", mockFilePath);
            mockResponse.header("Mock-File-Key", mockKeyConfig);

            // latency
            // 总延迟时间
            String mainLatency = mockBean.getString("$latency$");

            String latencyStr = mockBean.getString(mockKey + "latency");
            String status = mockBean.getString(mockKey + "status");
            Map<String, Object> headers = mockBean.get(mockKey + "headers", new SerializationTypeToken<Map<String, Object>>() {
            });

            // match
            boolean bodySetter = false;
            SimpleSpelBean<?> matchLisTBean = mockBean.getSimpleSpelBean(mockKey + "match");
            if (matchLisTBean.beanTypeMatch(List.class)) {
                List<?> matchList = (List<?>) matchLisTBean.getBean();
                for (Object match : matchList) {
                    if ((match instanceof Map) && ((Map<?, ?>) match).containsKey("when")) {
                        SimpleSpelBean<Object> matchBean = SimpleSpelBean.of(match);
                        boolean when = mc.parseExpression(matchBean.getString("when"), boolean.class);
                        if (when) {

                            // 设置分支
                            mockResponse.header("Mock-Branch", matchBean.getString("when"));

                            // latency
                            String _latencyStr = matchBean.getString("latency");
                            if (StringUtils.hasText(_latencyStr)) {
                                latencyStr = _latencyStr;
                            }

                            // status
                            String _status = matchBean.getString("status");
                            if (StringUtils.hasText(_status)) {
                                status = _status;
                            }

                            // header
                            Map<String, Object> _headers = matchBean.get("headers", new SerializationTypeToken<Map<String, Object>>() {
                            });
                            if (ContainerUtils.isNotEmptyMap(headers)) {
                                if (ContainerUtils.isNotEmptyMap(_headers)) {
                                    headers.putAll(_headers);
                                }
                            } else {
                                headers = _headers;
                            }

                            // body
                            // FILE
                            String fileBody = matchBean.get("body?.file", String.class);
                            if (StringUtils.hasText(fileBody)) {
                                mockResponse.resource(Resources.getResource(mc.parseExpression(fileBody, String.class)));
                                bodySetter = true;
                            }

                            // TXT
                            if (!bodySetter) {
                                String txtBody = matchBean.get("body?.txt", String.class);
                                if (StringUtils.hasText(txtBody)) {
                                    mockResponse.body(mc.parseExpression(txtBody, String.class));
                                    bodySetter = true;
                                }
                            }
                            break;
                        }
                    }
                }
            }




            // status
            setStatus(mc, mockResponse, status);

            // header
            setHeaders(mc, mockResponse, headers);

            // body
            if (!bodySetter) {
                String fileBody = mockBean.get(mockKey + "body?.file", String.class);
                if (StringUtils.hasText(fileBody)) {
                    mockResponse.resource(Resources.getResource(mc.parseExpression(fileBody, String.class)));
                    bodySetter = true;
                }

                // TXT
                if (!bodySetter) {
                    String txtBody = mockBean.get(mockKey + "body?.txt", String.class);
                    if (StringUtils.hasText(txtBody)) {
                        mockResponse.body(mc.parseExpression(txtBody, String.class));
                    }
                }
            }

            // latency
            String finalLatency = StringUtils.hasText(latencyStr) ? latencyStr : mainLatency;
            setLatency(mc, finalLatency);

            return mockResponse;
        }

        /**
         * 设置延时
         *
         * @param mc      方法上下文
         * @param latency 延时配置
         * @throws InterruptedException 可能出现的异常
         */
        private static void setLatency(MethodContext mc, String latency) throws InterruptedException {
            if (StringUtils.hasText(latency)) {
                long _latency = mc.parseExpression(latency, long.class);
                if (_latency > 0) {
                    Thread.sleep(_latency);
                }
            }
        }


        /**
         * 设置状态
         *
         * @param mc           方法上下文
         * @param mockResponse Mock响应
         * @param status       状态配置
         */
        private static void setStatus(MethodContext mc, MockResponse mockResponse, String status) {
            if (StringUtils.hasText(status)) {
                mockResponse.status(mc.parseExpression(status, Integer.class));
            } else {
                mockResponse.status(200);
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
}
